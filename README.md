# Prahar 2.0 - Enterprise Cyber Safety, Phishing Detection & Fraud Response Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%2F%2021%20%2F%2025-orange.svg)](https://www.oracle.com/java/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.111.0-teal.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18.x-blue.svg)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-5.x-purple.svg)](https://vitejs.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Tagline:** *Detect. Understand. Stay Safe.*  
> **Mission:** Empower citizens, employees, and families to detect phishing and cyber fraud, understand deceptive tactics, organize personal financial activity securely, and execute immediate containment actions during critical emergencies.

---

## 🛡️ Prahar 2.0 Core Capabilities

### 1. Multi-Channel Phishing & Threat Scanner
Inspect URLs, SMS/messaging texts, email bodies, screenshot OCR images, and decoded QR codes. Combines 30+ structural/lexical indicators, SSRF security guards, and machine learning inference (FastAPI + XGBoost/RandomForest).

### 2. Advanced NLP Conversational Assistant ("Ask Prahar")
A context-aware, multi-turn AI safety assistant powered by an NLP pipeline (Normalizer, Entity Extractor, 22-Intent Taxonomy, Slot Memory, and Authorized Tool Invocations). Supports optional server-side Google Gemini integration while seamlessly falling back to local deterministic rule-based advice when offline.

### 3. Personal Finance & Transaction Monitor ("My Money & Transactions")
Helps users record and monitor financial records without claiming unauthorized live bank access:
* **Ingestion Channels:** Manual entry, bank statement CSV upload, and pasted SMS alert parsing.
* **SHA-256 Fingerprint Deduplication:** Automatically skips re-uploaded CSV records.
* **Explainable Anomaly Alerts:** Transparent statistical rules flagging outlier debits (>3x average), duplicate transactions within 24 hours, and uncategorized large charges.
* **Cash Flow Ledger:** Inflows, outflows, net cash flow, and category spending summaries with CSV export.

### 4. Fraud Incident Response Centre ("Fraud & Safety Centre")
Step-by-step incident containment and reporting workflows:
* **Indian Cybercrime Protocol:** Integrated access to the **1930 Helpline** and National Cyber Crime Reporting Portal ([cybercrime.gov.in](https://cybercrime.gov.in/)) prioritizing the **Golden Hour (2-3 hours)** window for freezing transit funds.
* **Dispute Letter Generator:** Automatically compiles formal complaint drafts addressed to Bank Branch Managers and Cyber Crime Police quoting the **RBI Zero-Liability Circular** (`RBI/2017-18/15`) and **IT Act 2000** (`Sections 43A, 66C, 66D`).
* **Incident Lifecycle Tracker:** Interactive checklists with real-time status progression (`Action Required`, `Report Prepared`, `Submitted Externally`, `Resolved`).

### 5. Real-Time Alerts & Persistent Notification Centre
* **Real Application Events Only:** Alerts generated strictly from verified scans (risk $\ge 35$), suspicious QR/screenshots, and fraud incidents. Zero synthetic fabrication.
* **Server-Sent Events (SSE):** One-way real-time alert pushes with bounded user connection limits, 25s heartbeat keep-alives, and automatic reconnection.
* **Strict Ownership & Idempotency:** DB-level authorization checks preventing cross-user alert leakage; SHA-256 idempotency keys preventing alert duplicates.

### 6. Personal Mobile Safety ("My Device Safety")
User-guided workflows for mobile emergency scenarios:
* **Stolen / Lost Phone:** Indian carrier SIM suspension hotlines (Jio, Airtel, Vi, BSNL), official IMEI blacklisting via India's Central Equipment Identity Register ([ceir.gov.in](https://www.ceir.gov.in/)), and remote device wipe.
* **SIM Swap Containment:** Immediate telecom carrier verification and NetBanking locks.
* **Malicious APK Containment:** Airplane mode isolation, Android Safe Mode boot procedures, and Device Administrator revocation.

### 7. Cyber Awareness Hub ("Learn")
Interactive simulation quizzes, scam encyclopedia, real-world fraud case studies, and multilingual support (English, Hindi, Kannada).

---

## 📐 Architecture & Service Dependencies

```text
                                          PRAHAR 2.0
                                 Detect. Understand. Stay Safe.
                                               │
          ┌──────────────────┬─────────────────┼──────────────────┬──────────────────┐
          ↓                  ↓                 ↓                  ↓                  ↓
        SCAN             MY MONEY        SAFETY CENTRE       ASK PRAHAR        DEVICE SAFETY
    (Multi-Channel)   (Ledger & Rules)  (1930 & Disputes)   (NLP Assistant)   (CEIR & Malware)
          │                  │                 │                  │                  │
          └──────────────────┴─────────────────┼──────────────────┴──────────────────┘
                                               ▼
                         React 18 + Vite + Tailwind CSS Frontend
                                (Reverse Proxy Nginx / Port 3000)
                                               │
                                 (JWT Bearer / X-Request-ID)
                                               ▼
                          Spring Boot 3.3.4 API (Port 8080)
          ┌────────────────────────────────────┼────────────────────────────────────┐
          ↓                                    ↓                                    ↓
MySQL 8.0 Database (Port 3306)     FastAPI ML Service (Port 8000)       Google Gemini API (Optional)
(Users, Scans, Incidents,          (Feature Extractor, Model Loader,    (External Generative AI,
 Notifications, Chat History)      XGBoost / Heuristics Engine)         Server-side Proxied)
```

---

## 🚀 Deployment & Startup Instructions

### Option A: Docker Compose Deployment (Recommended for Production)

#### Prerequisites
- Docker Engine 24+ and Docker Compose v2.20+
- At least 4GB of free RAM and 10GB disk space

#### 1. Setup Environment Secrets
Copy the environment template and configure strong secrets:
```bash
cp .env.example .env
```
Ensure you change `DB_PASSWORD`, `DB_ROOT_PASSWORD`, and `JWT_SECRET` in `.env`.

#### 2. Launch All Services
```bash
docker compose up -d --build
```

#### 3. Verify Health & Logs
```bash
docker compose ps
docker compose logs -f backend
```

| Service | Container Name | Internal URL | Host URL | Health Endpoint |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | `phishshield-frontend` | `http://frontend:80` | `http://localhost:3000` | `http://localhost:3000/` |
| **Backend** | `phishshield-backend` | `http://backend:8080` | `http://localhost:8080` | `http://localhost:8080/api/health` |
| **ML Microservice**| `phishshield-ml` | `http://ml-service:8000`| `http://localhost:8000` | `http://localhost:8000/health` |
| **MySQL Database** | `phishshield-mysql` | `mysql-db:3306` | `localhost:3306` | `mysqladmin ping` |

---

### Option B: Native Development Setup

#### Prerequisites
- Java JDK 17, 21, or 25
- Apache Maven 3.9+
- Python 3.11+
- Node.js 18+ & npm 9+
- MySQL Server 8.0+ (or use default development H2 in-memory profile)

#### 1. Python ML Microservice
```powershell
cd ml-service
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
* Interactive API Documentation: `http://localhost:8000/docs`
* Run ML tests: `python -m pytest`

#### 2. Spring Boot Backend API
```powershell
cd backend
# Run with in-memory H2 database (Development Mode):
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

# Or run with MySQL (Production Mode):
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```
* API Server: `http://localhost:8080`
* Swagger UI: `http://localhost:8080/swagger-ui/index.html`
* Run Test Suite: `mvn test`

#### 3. React Frontend Web App
```powershell
cd frontend
npm install
npm run dev
```
* Development Web App: `http://localhost:5173`
* Build Production Bundle: `npm run build`

#### 4. Automated Multi-Service Script (Windows PowerShell)
```powershell
.\start_services.ps1
```

---

## 🩺 Health & Readiness Monitoring

Prahar distinguishes lightweight **liveness** from dependency-aware **readiness**:

### 1. Spring Boot Liveness Check
```http
GET /api/health
```
Response (`200 OK`):
```json
{
  "success": true,
  "message": "System is alive and operational",
  "data": {
    "status": "UP",
    "service": "prahar-backend",
    "version": "2.0.0",
    "timestamp": 1730000000000
  }
}
```

### 2. Comprehensive System Readiness Check
```http
GET /api/health/readiness
```
Probes real MySQL database connectivity and the FastAPI ML microservice status without exposing internal passwords or stack traces. To optionally test Google Gemini API connectivity live:
```http
GET /api/health/readiness?probeAi=true
```

### 3. Request Correlation & Observability
All HTTP requests generate or propagate an `X-Request-ID` header. This UUID is attached to SLF4J MDC, outputting structured logs:
```text
2026-10-24 14:10:00.123 [http-nio-8080-exec-1] [a1b2c3d4-e5f6-7890-abcd-ef1234567890] INFO  c.p.controller.URLScanController - Initiated URL scan request
```

---

## 🗄️ Database Management & Backup Procedures

### Initial Schema & Seed
When starting MySQL, schemas are loaded in order:
1. `database/schema.sql`: Complete canonical table definitions, foreign keys, and indexes (Tables: `roles`, `users`, `url_scans`, `financial_transactions`, `fraud_incidents`, `chat_conversations`, `chat_messages`, `user_notifications`, `notification_preferences`).
2. `database/seed.sql`: Default administrative roles, threat categories, and cyber advisories.

### Incremental Migrations
- `database/migration_v2.sql`: Adds financial transactions and fraud incidents.
- `database/migration_v3.sql`: Adds user notifications and alert preferences.

### Backup & Restore Procedure
#### Backup (Database Dump):
```bash
docker exec phishshield-mysql mysqldump -u phishshield_user -p phishshield_db > backup_prahar_$(date +%Y%m%d_%H%M%S).sql
```
#### Restore:
```bash
docker exec -i phishshield-mysql mysql -u phishshield_user -p phishshield_db < backup_file.sql
```

---

## 🔒 Security & Best Practices

1. **Zero Secret Leakage:** `.gitignore` actively excludes `.env`, `*.pem`, `*.key`, and generated credentials. Gemini and database secrets are never exposed to the client or browser bundle.
2. **SSRF Guard Active:** All URL scans and threat lookups are routed through `SsrfProtectionService`, resolving DNS records and blocking loopback (`127.0.0.1`), link-local (`169.254.169.254`), and private RFC-1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
3. **Sensitive Data Scrubbing:** `SensitiveDataScrubber` automatically redacts credit card numbers (Luhn-compliant), OTPs, CVVs, and banking PINs before persisting incident notes or chat messages.
4. **Least Privilege & Ownership:** All notification, incident, and transaction endpoints verify `userId == authenticatedUser.getId()`, returning HTTP 403 Forbidden on tamper attempts.

---

## 🧪 Automated Verification Summary (186 / 186 Tests Passing - 100% Pass Rate)

All tests have been independently executed and verified on the current source revision:

| Component | Test Runner | Test Files | Total Tests | Outcome |
| :--- | :--- | :--- | :--- | :--- |
| **Spring Boot API** | Maven Surefire | 20 Test Classes | **130** | **130 Passed** (0 Failures, 0 Errors, 0 Skipped) |
| **FastAPI ML Service** | Pytest 9.x | 7 Test Modules | **36** | **36 Passed** (0 Failures, 0 Errors) |
| **React/Vite Frontend** | Node Test Runner | `src/tests/*.test.js` | **10** | **10 Passed** (0 Failures) |
| **Browser Extension** | Node Test Runner | `test_extension.js` | **10** | **10 Passed** (0 Failures) |
| **Frontend Production** | Vite 5.x | Build Bundle | **1,604 modules** | **Built in 7.73s** (Main bundle 201.41 kB gzip 60.86 kB) |
| **Concurrency Burst** | JUnit 5 Multi-thread | 10 Workers / 30 Scans | **119 scans/s** | **100% Success Rate (0 Errors)** |
| **TOTAL** | — | — | **186** | **100% Pass Rate (0 Failures)** |

---

## ⚡ Performance, Scalability & Load Testing
* 📊 **[Phase 29 Performance Baseline](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_29_BASELINE.md)**: Architectural component audit and pre-optimization bottleneck analysis.
* 📈 **[Performance Results Report](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PERFORMANCE_RESULTS.md)**: Measured benchmarks (sub-10ms URL scanning, 12,500 rule checks/sec, 61.3% bundle reduction).
* 🛠️ **[Load Testing Guide](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/LOAD_TESTING_GUIDE.md)**: Instructions for running in-process multi-threaded load tests.

---

## 🎓 Final Academic Submission & Viva Dossier (Phases 31–36)

For academic evaluators, university project guides, and external viva examiners, complete consolidated documentation is provided in `docs/`:
* 📋 **[Final Packaging Report (Phase 36)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_36_PACKAGING_REPORT.md)**: Verification of included/excluded files, startup commands, and submission readiness.
* 📑 **[Release Signoff Matrix (Phase 35)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_35_RELEASE_SIGNOFF.md)**: Multi-category assessment for academic evaluation vs. production staging.
* 🚀 **[Final Demonstration Guide (Phase 35)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_35_FINAL_DEMO.md)**: Safe 5-step live demonstration runbook with expected empirical behaviors.
* 🧪 **[End-to-End Regression Report (Phase 34)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_34_REGRESSION_REPORT.md)**: Detailed test logs across all 186 automated test cases.
* 🔬 **[ML Baseline & Evaluation Results (Phase 33)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_33_EVALUATION_RESULTS.md)**: Empirical comparison against 4 baselines, Wilson 95% CIs, and error analysis.
* 🎯 **[Comprehensive Viva Voce Bank (Phase 32)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_32_VIVA_BANK.md)**: 62 in-depth technical defense questions and evidence-backed answers.
* 🖥️ **[Academic Presentation Deck (Phase 32)](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PHASE_32_PRESENTATION_CONTENT.md)**: 14-slide presentation content with structured sections.
* 📖 **[Comprehensive Academic Project Report](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/PROJECT_REPORT.md)**: Full 21-section MCA project report adhering to academic dissertation standards.
* 📐 **[Architecture & Data-Flow Diagrams](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docs/ARCHITECTURE_DIAGRAMS.md)**: High-resolution Mermaid diagrams for architecture, scan flow, ML pipeline, and deployment.

---

## 📄 License
Licensed under the MIT License.

