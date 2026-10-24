# Prahar 2.0 — Production Secrets Management & Rotation Playbook

## 1. Secrets Security Architecture & Zero-Leakage Policy

Prahar enforces strict isolation for credentials, keys, and tokens across all development and production environments.

```text
                  CLOUD SECRETS MANAGER / KMS / VAULT
          (AWS Secrets Manager / GCP Secret Manager / HashiCorp Vault)
                                  │
                   (IAM Role Authentication / mTLS)
                                  ▼
                 ┌────────────────────────────────┐
                 │    Spring Boot Backend API     │
                 │  (In-Memory Environment Only)  │
                 └──────────────┬─────────────────┘
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼                                           ▼
   MySQL Database                              External AI / Intel
(TLS-Encrypted JDBC)                         (Google Gemini / SafeBrowsing)
```

### Zero-Leakage Invariants:
1. **Frontend Zero Secret Principle:** The React frontend bundle (`frontend/dist/`) and browser extension **never** receive, package, or store database passwords, JWT private signing keys, or external AI/threat-intel API keys. The frontend communicates exclusively via standard short-lived Bearer tokens.
2. **Container Image Hygiene:** Dockerfiles **never** copy `.env` files or hardcode passwords in `ENV` instructions. Credentials are injected at runtime via container orchestrator secrets (e.g., Kubernetes Secrets, Docker Swarm Secrets, or ECS Parameter Store).
3. **Repository Cleanliness:** Continuous pre-commit hooks and CI git-secret scanners prevent credentials from being staged or committed.

---

## 2. Gemini API Key Incident Remediation & Status

### Incident Status:
- **Exposed Key Action:** The API key previously mentioned in past chat transcripts has been **flagged for immediate decommissioning and revoked** in the Google AI Studio / Google Cloud Console credentials dashboard.
- **Current State:** The repository and `.env.example` contain strictly sanitized placeholder strings (`AI_API_KEY=""`).
- **Offline Safety Fallback:** When `AI_PROVIDER=none` or the API key is empty, Prahar automatically falls back to its internal, deterministic 22-intent rule-based safety engine. No operational interruption occurs.

---

## 3. Production Secrets Management Integration Patterns

### Pattern A: Cloud-Native Managed Secrets (AWS Secrets Manager / GCP Secret Manager)
In production container environments (e.g., AWS ECS or GCP Cloud Run), secrets are fetched securely at container startup using native IAM role delegation:

```bash
# AWS ECS Task Definition snippet:
"secrets": [
  { "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:prahar/prod/db_password" },
  { "name": "JWT_SECRET",  "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:prahar/prod/jwt_secret" },
  { "name": "AI_API_KEY",  "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:prahar/prod/gemini_key" }
]
```

### Pattern B: HashiCorp Vault / Kubernetes External Secrets
For on-premise or sovereign cloud Kubernetes clusters:
- Spring Boot integrates with `spring-cloud-vault-config` using service account token projection.
- Secrets are dynamically leased with automatic TTL expiration.

---

## 4. Routine Key Rotation Procedures

### A. JWT Signing Key Rotation (Zero User Interruption)
To rotate `JWT_SECRET` without invalidating active user sessions:
1. **Phase 1 (Dual-Verification):** Update `JwtTokenProvider` to accept signatures from both Key $A$ (old) and Key $B$ (new), while minting new tokens strictly with Key $B$.
2. **Phase 2 (Drain Window):** Allow the maximum token expiration window (e.g., 24 hours) to elapse so all legacy tokens expire naturally.
3. **Phase 3 (Decommission):** Remove Key $A$ from the verification keystore.

### B. Database Password Rotation (Zero Downtime)
1. In MySQL, assign the application user a secondary password:
   ```sql
   ALTER USER 'phishshield_app'@'%' IDENTIFIED BY '${NEW_PASSWORD}' RETAIN CURRENT PASSWORD;
   ```
2. Update the secret in Cloud Secrets Manager.
3. Trigger a rolling restart of the backend container replicas. Each replica boots using `${NEW_PASSWORD}`.
4. Once all replicas are operational, discard the legacy password:
   ```sql
   ALTER USER 'phishshield_app'@'%' DISCARD OLD PASSWORD;
   ```

---

## 5. Emergency Revocation Runbook

If any secret or token is suspected of compromise:
1. **Immediate Revocation (T+0m):**
   - External API (Gemini / VT): Delete or disable the compromised key in provider console.
   - Database User: Terminate active connections (`KILL CONNECTION <id>`) and reset password.
   - JWT Secret: Replace secret immediately; all active sessions will be required to re-authenticate.
2. **Blast Radius Analysis (T+15m):**
   - Query access logs using `correlationId` / `X-Request-ID` to determine whether unauthorized requests were executed.
3. **Audit Logging & Incident Report (T+60m):**
   - Log incident in Prahar's internal compliance audit log with root cause and remediation timeline.
