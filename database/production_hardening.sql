-- ==============================================================================
-- PRAHAR 2.0 - PRODUCTION DATABASE HARDENING & LEAST-PRIVILEGE PROVISIONING
-- Database Engine: MySQL 8.0+
-- ==============================================================================

-- 1. Create Dedicated Application Runtime User (DML Only)
-- This user has zero administrative or DDL privileges (cannot DROP, ALTER, or TRUNCATE)
CREATE USER IF NOT EXISTS 'phishshield_app'@'%' 
    IDENTIFIED BY '${APP_DB_PASSWORD}'
    REQUIRE SSL
    WITH MAX_QUERIES_PER_HOUR 0
         MAX_UPDATES_PER_HOUR 0
         MAX_CONNECTIONS_PER_HOUR 0
         MAX_USER_CONNECTIONS 50;

-- Grant strictly necessary DML permissions to application user
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON phishshield_db.* TO 'phishshield_app'@'%';

-- 2. Create Dedicated Schema Migration User (DDL Only)
-- Used exclusively during CI/CD migration runs or DBA maintenance
CREATE USER IF NOT EXISTS 'phishshield_migrator'@'%' 
    IDENTIFIED BY '${MIGRATOR_DB_PASSWORD}'
    REQUIRE SSL
    WITH MAX_USER_CONNECTIONS 5;

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON phishshield_db.* TO 'phishshield_migrator'@'%';

-- 3. Flush Privileges to ensure active enforcement
FLUSH PRIVILEGES;

-- 4. Verify Active Database Hardening Variables:
-- The following parameters should be verified in MySQL configuration (my.cnf / RDS Parameter Group):
--
-- binlog_format                 = ROW
-- binlog_expire_logs_seconds    = 604800   (7 days retention for PITR)
-- sync_binlog                   = 1        (Crash-safe binary logging)
-- innodb_flush_log_at_trx_commit = 1       (Strict ACID durability)
-- require_secure_transport      = ON       (Reject all non-TLS connections)
-- tls_version                   = TLSv1.2,TLSv1.3
-- max_connections               = 200
-- default_password_lifetime     = 90       (Force 90-day password rotation policy)
