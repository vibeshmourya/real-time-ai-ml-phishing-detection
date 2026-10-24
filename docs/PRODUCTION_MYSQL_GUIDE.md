# Prahar 2.0 — Production Managed MySQL 8.0 & Disaster Recovery Guide

## 1. Provider-Neutral Architecture Overview

In a production environment, Prahar decouples stateful persistence from application compute containers. The relational database is hosted on a managed database service (e.g., AWS RDS for MySQL, Google Cloud SQL for MySQL, Azure Database for MySQL Flexible Server, or DigitalOcean Managed Databases).

```text
                           PRIVATE CLOUD VPC
 ┌──────────────────────────────────────────────────────────────┐
 │                                                              │
 │   Public Subnet                                              │
 │   ┌──────────────────────┐                                   │
 │   │ Reverse Proxy (TLS)  │ (Port 80/443 exposed to Internet) │
 │   └──────────┬───────────┘                                   │
 │              │ Internal HTTP                                 │
 │              ▼                                               │
 │   Private Application Subnet                                 │
 │   ┌──────────────────────────────────────────────────────┐   │
 │   │  Spring Boot Backend API                             │   │
 │   │  (No public IP; private container network)           │   │
 │   └──────────────────────────┬───────────────────────────┘   │
 │                              │ TLS Encrypted JDBC            │
 │                              ▼ (Port 3306 - Private only)    │
 │   Isolated Database Subnet                                   │
 │   ┌──────────────────────────────────────────────────────┐   │
 │   │  Managed MySQL 8.0 Primary Instance                  │   │
 │   │  - Binary Logging Enabled (ROW format)               │   │
 │   │  - Multi-AZ Synchronous/Semi-Sync Standby            │   │
 │   │  - Zero Public Ingress (RFC-1918 Private Subnet Only)│   │
 │   └──────────────────────────────────────────────────────┘   │
 └──────────────────────────────────────────────────────────────┘
```

---

## 2. Hardening & Connection Security

### A. Private Subnet Isolation
- The MySQL instance must **never** be assigned a public IP address (`publicly_accessible = false`).
- Security Groups / Firewall Rules must permit incoming TCP traffic on port 3306 **only** from the CIDR block or Security Group of the backend API instances.

### B. Enforced Transport Layer Security (TLS)
- MySQL Parameter: `require_secure_transport = ON`.
- Supported Protocols: `tls_version = TLSv1.2,TLSv1.3`.
- The Spring Boot application profile (`application-prod.yml`) enforces TLS:
  ```yaml
  spring.datasource.url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?sslMode=REQUIRED&useSSL=true&requireSSL=true&verifyServerCertificate=true
  ```

### C. Least-Privilege Role Separation
In accordance with [`database/production_hardening.sql`](../database/production_hardening.sql):
- **Runtime Application User (`phishshield_app`):** Granted only `SELECT, INSERT, UPDATE, DELETE, EXECUTE`. Cannot execute `DROP TABLE`, `ALTER TABLE`, or administrative functions.
- **Migration User (`phishshield_migrator`):** Used strictly during deployment pipelines to run structured schema migrations (`database/schema.sql`, `migration_v2.sql`, `migration_v3.sql`).
- **Administrative DBA Account:** MFA-protected root account used exclusively for cluster provisioning and parameter tuning.

---

## 3. Backup, Disaster Recovery & PITR Specifications

| Metric / Parameter | Production Target | Technical Mechanism |
| :--- | :--- | :--- |
| **RPO (Recovery Point Objective)** | **$\le 5$ Minutes** | Continuous binary logging (`binlog_format=ROW`, `sync_binlog=1`) uploaded to encrypted object storage every 5 minutes. |
| **RTO (Recovery Time Objective)** | **$\le 30$ Minutes** | Automated restoration of daily storage snapshot + automated binary log replay to target timestamp. |
| **Snapshot Retention** | **35 Days** | Automated daily full physical snapshots taken during off-peak maintenance window (02:00 UTC). |
| **Binary Log Retention** | **7 Days** | `binlog_expire_logs_seconds = 604800`. Enables Point-In-Time Recovery to any second within the past 7 days. |
| **Geographic Redundancy** | **Cross-Region** | Daily snapshots copied to secondary cloud region for catastrophic disaster recovery. |

### Note on Point-In-Time Recovery (PITR)
> **Critical Engine Distinction:** MySQL uses **Binary Logs (binlogs)** for transaction replication and point-in-time recovery. PostgreSQL WAL archiving commands do not apply to MySQL.

---

## 4. Verification & Restoration Runbook

### A. Non-Destructive Backup Verification (Dry Run)
Execute a logical consistency check and verify table checksums:
```bash
# Check table integrity without downtime
mysqlcheck -u phishshield_migrator -p --databases phishshield_db --check --medium-check
```

### B. Point-In-Time Recovery (PITR) Execution Procedure
In the event of accidental data corruption or catastrophic failure:

1. **Identify Target Recovery Timestamp:**
   Convert target incident time to UTC (e.g., `2026-10-24 14:30:00 UTC`).
2. **Launch Restored Instance from Latest Snapshot Preceding Incident:**
   - In Cloud Console / CLI, restore snapshot `snap-phishshield-20261024-0200` to a temporary instance name (e.g., `phishshield-db-restored`).
3. **Replay Binary Logs to Exact Second:**
   - Using MySQL Utilities / Cloud PITR feature:
   ```bash
   mysqlbinlog --start-datetime="2026-10-24 02:00:00" \
               --stop-datetime="2026-10-24 14:30:00" \
               /var/log/mysql/mysql-bin.* | mysql -u root -p -h phishshield-db-restored phishshield_db
   ```
4. **Data Verification:**
   - Run audit verification queries to confirm record counts for `url_scans`, `financial_transactions`, and `fraud_incidents`.
5. **DNS / CNAME Cutover:**
   - Switch private internal DNS record `db.internal.prahar.net` to point to the restored instance endpoint.
   - Application reconnects without requiring code redeployment.

---

## 5. Schema Migration Policy (Zero Destructive Resets)

- Production `application-prod.yml` enforces:
  ```yaml
  spring.jpa.hibernate.ddl-auto: validate
  ```
- **Rule:** Hibernate is strictly prohibited from running `update` or `create-drop` against production databases.
- All database modifications must be submitted as versioned SQL migration scripts (`migration_vX.sql`) and peer-reviewed prior to deployment.
