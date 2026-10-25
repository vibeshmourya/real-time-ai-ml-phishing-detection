# PRAHAR AI/ML-BASED PHISHING DETECTION SYSTEM
## Comprehensive Vercel Deployment Audit & Production Hosting Strategy

**Document Status:** Complete Architecture & Deployment Audit  
**Author:** Senior Full-Stack Engineer & Cloud Deployment Specialist  
**Target Repository:** `Prahar AI/ML-Based Phishing Detection and Prevention System`  
**Audit Date:** October 25, 2026  
**Target File:** `docs/PRAHAR_VERCEL_DEPLOYMENT_AUDIT.md`  

---

### Executive Summary

The **Prahar Security Platform** is a multi-tier, defense-in-depth cybersecurity application consisting of:
1. A **React 18 Single-Page Application (SPA)** powered by **Vite 5** and **Tailwind CSS**.
2. An **Enterprise Spring Boot 3.3.4 (Java 17)** REST backend managing security rules, JWT authentication, SSRF protection, threat intelligence, image/QR decoding, and audit logs.
3. A **Python FastAPI** machine learning microservice executing real-time inference on a 30-feature vector using a pre-trained **XGBoost/scikit-learn** model.
4. A **MySQL 8.0** relational database storing scans, audit logs, incidents, user accounts, and threat feedback.
5. A **Google Gemini AI** integration operating exclusively on the backend for conversational incident response and safe security advisory.

#### Core Deployment Reality Check:
* **Can the Frontend be deployed to Vercel?** **YES (100% Native Compatibility).** The React/Vite SPA builds cleanly to static assets and is ideally suited for Vercel's global Edge CDN.
* **Can the Backend and ML Microservice run on Vercel?** **NO.** Vercel is a serverless platform built for Node.js, Edge runtime, and lightweight stateless serverless functions. It **does not support long-running Java/JVM processes (Spring Boot)**, nor does it support **heavy scientific Python ML runtimes (XGBoost/scikit-learn/OpenCV with in-memory model containers)** or **relational MySQL databases**.
* **Recommended Production Strategy:** A **Decoupled Architecture** where:
  - **Vercel** hosts the React 18 / Vite Frontend.
  - A container PaaS (such as **Render**, **Railway**, **AWS Lightsail/ECS**, or **Fly.io**) hosts the Spring Boot backend and FastAPI ML microservice.
  - A managed provider (such as **Aiven MySQL**, **AWS RDS**, or **Railway MySQL**) hosts the MySQL database.
  - Vercel's Edge rewrites (`vercel.json`) or HTTPS CORS handles secure routing.

---

## TASK 1 — Project Identification & Architecture Inventory

### 1. Frontend Framework & Exact Folder
* **Framework:** React 18.3.1 (Single-Page Application)
* **Build Tool:** Vite 5.2.13 (ES Modules)
* **Styling & Icons:** Tailwind CSS 3.4.4, PostCSS, Autoprefixer, Lucide React (`^0.395.0`), Chart.js (`^4.4.3`), React-Chartjs-2
* **Exact Folder:** `frontend/`
* **Entry Points:** `frontend/index.html`, `frontend/src/main.jsx`, `frontend/src/App.jsx`
* **Build Verification:** Tested locally with `npm run build` in `frontend/`. Completed successfully in **21.42s** generating production bundles in `frontend/dist/`.

### 2. Backend Framework & Exact Folder
* **Framework:** Spring Boot 3.3.4 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`)
* **Language & Runtime:** Java 17 (tested with JDK 25 compiler compatible)
* **Build Tool:** Apache Maven (`backend/pom.xml`)
* **Security & Token Management:** JJWT 0.12.6 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
* **Documentation:** SpringDoc OpenAPI 3.0 / Swagger UI 2.6.0 (`/swagger-ui/index.html`)
* **Image & QR Processing:** Google ZXing 3.5.3 (`zxing:core`, `zxing:javase`)
* **Exact Folder:** `backend/`

### 3. Python / ML Microservice
* **Framework:** FastAPI (`>=0.111.0`) with ASGI Server Uvicorn (`>=0.30.1`)
* **ML Runtimes:** scikit-learn (`>=1.4.0`), XGBoost (`>=2.0.0`), pandas (`>=2.2.0`), numpy (`>=1.26.0`), joblib (`>=1.4.0`)
* **Computer Vision / OCR:** OpenCV (`opencv-python-headless`)
* **Model Artifacts:**
  - `ml-service/models/best_model.joblib` (Trained classifier pipeline)
  - `ml-service/models/model_metadata.json` (Feature schema, metrics, threshold config)
* **Startup Behavior:** Pre-loads the ML model artifact into memory upon startup using FastAPI's asynchronous `lifespan` context manager (`ml-service/app/main.py`).
* **Exact Folder:** `ml-service/`

### 4. Database Type & Production Connection Requirements
* **Database Engine:** MySQL 8.0 (Engine: InnoDB, Character Set: UTF-8 / utf8mb4)
* **Development In-Memory Option:** H2 In-Memory DB (`spring.profiles.active=dev` using `jdbc:h2:mem:phishshield_dev_db`)
* **Production Connection Requirements (`application-prod.yml`):**
  - JDBC URL: `jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME:phishshield_db}?sslMode=${DB_SSL_MODE:REQUIRED}&useSSL=true&requireSSL=true&verifyServerCertificate=${DB_VERIFY_SERVER_CERT:true}&allowPublicKeyRetrieval=false&serverTimezone=UTC&characterEncoding=UTF-8`
  - Connection Pool: HikariCP (`maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 20000ms`, `idle-timeout: 300000ms`)
  - Schema Strategy: `hibernate.ddl-auto: validate` (**Strict Rule:** Hibernate will NOT create or modify tables in production; DDL must be pre-applied).
  - Initialization Scripts: `database/schema.sql`, `database/seed.sql`, `database/migration_v2.sql`, `database/migration_v3.sql`, `database/production_hardening.sql`.

### 5. Gemini API Integration & Key Reading
* **Integration Location:** Strictly inside the Spring Boot backend (`backend/src/main/java/com/phishshield/service/ChatService.java`).
* **Configuration Mapping:** Read from environment variables via `backend/src/main/resources/application.yml`:
  ```yaml
  app:
    ai:
      provider: ${AI_PROVIDER:none}
      api-key: ${AI_API_KEY:${GEMINI_API_KEY:${GOOGLE_API_KEY:}}}
      api-url: ${AI_API_URL:}
      model: ${AI_MODEL:gemini-1.5-flash}
      timeout-ms: ${AI_TIMEOUT_MS:15000}
  ```
* **Protocol & Endpoint:** Uses standard HTTP POST with Bearer authentication against Google Gemini's OpenAI-compatible completions endpoint:
  `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
* **Server-Side Tool Calling:** The backend supplies function schemas to Gemini (`analyze_url`, `explain_scan_result`) and evaluates them securely on the server before completing the chat response.
* **Frontend Isolation:** The frontend (`frontend/src/api/chatApi.js`) calls only `POST /api/chat`. **No Gemini keys or Google AI credentials are ever present or exposed in frontend client code.**

### 6. Existing Authentication, CORS, API Routes, & URL Analysis
* **Authentication:**
  - Stateless Bearer JWT tokens issued by `AuthController.java` (`POST /api/auth/login`, `POST /api/auth/register`).
  - Validated by `JwtAuthenticationFilter.java` using secret `JWT_SECRET`.
  - Roles: `ROLE_USER`, `ROLE_SECURITY_ANALYST`, `ROLE_ADMIN`.
* **CORS:**
  - Defined in `backend/src/main/java/com/phishshield/config/WebConfig.java`.
  - Reads `app.cors.allowed-origins` from `${CORS_ALLOWED_ORIGINS}`.
  - Allowed Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
  - Headers: `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`, `Origin`.
  - Credentials: `allowCredentials: true`.
* **Key API Routes:**
  - Scanning: `/api/scans/url`, `/api/scans/email`, `/api/scans/message`, `/api/scans/screenshot`, `/api/scans/qr`, `/api/scans/unified-image`, `/api/scans/my-scans`.
  - Intelligence & Diagnostics: `/api/scans/ml-status`, `/api/scans/model/evaluations`, `/api/health`, `/api/health/readiness`.
  - Chatbot: `/api/chat`, `/api/chat/status`, `/api/chat/conversations/**`.
  - Administration: `/api/dashboard/admin`, `/api/audit-logs/**`, `/api/reports`, `/api/alerts`.
* **URL-Analysis Security Pipeline:**
  1. **SSRF Guard:** `SSRFProtectionService.java` resolves domain DNS and validates that target IPs are not loopback (`127.0.0.0/8`), private RFC-1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), link-local, multicast, or cloud metadata endpoints (`169.254.169.254`).
  2. **Lexical/Heuristic Engine:** `RuleEngineService.java` analyzes brand keywords, typosquatting, entropy, punycode, subdomains, and suspicious TLDs.
  3. **ML Microservice:** `MLServiceClient.java` calls FastAPI `/predict` extracting 30 structural/lexical features.
  4. **Multi-Signal Fusion Scoring:** `MultiSignalScoringEngine.java` produces a normalized composite risk score (0–100) with explainable risk level indicators.

### 7. Current Build Commands & Required Environment Variables
* **Frontend:**
  - Build Command: `npm run build` (outputs to `frontend/dist`)
  - Dev Command: `npm run dev`
  - Variables: `VITE_API_URL`
* **Backend:**
  - Build Command: `mvn clean package -DskipTests` (outputs `backend/target/backend-1.0.0-SNAPSHOT.jar`)
  - Run Command: `java -jar target/backend-1.0.0-SNAPSHOT.jar`
  - Variables: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSL_MODE`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `ML_SERVICE_URL`, `AI_PROVIDER`, `AI_API_KEY`, `AI_MODEL`.
* **ML Microservice:**
  - Build Command: `docker build -t prahar-ml ./ml-service` or `pip install -r requirements.txt`
  - Run Command: `uvicorn app.main:app --host 0.0.0.0 --port 8000`
  - Variables: `APP_ENV`, `PORT`, `HOST`.

### 8. Hardcoded Localhost URLs & Development-Only Settings
1. `frontend/vite.config.js`:
   - Contains dev proxy rule: `target: 'http://localhost:8080'` for `/api`. This is valid for Vite local dev, but in production Vercel builds, Vite proxying is ignored unless handled via `vercel.json` rewrites.
2. `frontend/src/api/client.js`:
   - Line 3: `const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';`
   - If deployed on Vercel without configuring `VITE_API_URL` or a `vercel.json` rewrite, client requests will query `https://<vercel-domain>/api/...` which will yield HTTP 404.
3. `frontend/src/api/chatApi.js`:
   - Line 74: Hardcoded error string `'Prahar backend server is unreachable (port 8080)'`.
4. Token Storage Key Mismatch:
   - `frontend/src/context/AuthContext.jsx` saves tokens to `localStorage.setItem('prahar_token', token)`.
   - `frontend/src/api/client.js` line 15 reads `localStorage.getItem('phishshield_token')`.
   - `frontend/src/components/NotificationBell.jsx` line 58 reads `localStorage.getItem('phishshield_token')`.
   - *Consequence:* While `AuthContext` injects `client.defaults.headers.common['Authorization']` into Axios upon login, subsequent page refreshes or components accessing `localStorage` directly look for the wrong key.

---

## TASK 2 — Deployment Compatibility Matrix

| Component | Technology Stack | Can it run on Vercel as configured? | Explanation & Required Hosting |
| :--- | :--- | :---: | :--- |
| **Frontend** | React 18, Vite 5, Tailwind CSS | **YES** | Vite outputs static HTML, CSS, and JS bundles to `dist/`. Vercel natively deploys Vite SPAs with global Edge CDN caching and automatic asset compression. |
| **Backend API** | Spring Boot 3.3.4 (Java 17 / JVM) | **NO** | Vercel provides **no Java/JVM runtime environment**. Spring Boot requires a persistent long-running daemon with memory management, HikariCP JDBC connection pools, and servlet container execution. **Must be hosted on a persistent container service (e.g., Render, Railway, AWS ECS, or Fly.io).** |
| **ML Microservice** | Python FastAPI, Uvicorn, XGBoost, scikit-learn | **NO** | Vercel Serverless Functions have strict package size limits (250MB uncompressed) and execution timeouts (10s on Hobby). Compiling scientific packages (XGBoost, pandas, OpenCV) and loading a persistent in-memory joblib model exceeds Vercel serverless function boundaries. **Must be hosted on a container PaaS (e.g., Render, Railway, AWS ECS, or Fly.io).** |
| **Database** | MySQL 8.0 Relational Database | **NO** | Vercel does not host relational MySQL databases. **Must be hosted on a Cloud Managed MySQL service (e.g., Aiven MySQL, AWS RDS MySQL, PlanetScale, or Railway MySQL).** |
| **AI Incident Assistant** | Google Gemini 1.5 Flash via REST | **N/A (External API)** | Runs as an external Google Generative Language API call. Executed securely server-side by the Spring Boot backend. |
| **Browser Extension** | Chrome Manifest V3 | **NO (Client App)** | Packaged into `.crx` / `.zip` and loaded into Chromium-based browsers or submitted to Chrome Web Store. |

> [!IMPORTANT]
> **Strict Engineering Rule:** Do not rewrite or transpile the Java Spring Boot backend into Next.js or Node.js serverless functions merely to force full-stack hosting onto Vercel. Doing so would destroy the multi-threaded security filters, ZXing optical QR engines, Spring Security method-level RBAC, and audit logging pipelines already built into the repository. The correct, enterprise-standard approach is a **decoupled hybrid deployment**.

---

## TASK 3 — Production Security & Operational Risk Audit

### 1. Exposed API Keys or Credentials
* **Audit Finding:** **PASS (No Hardcoded Live Credentials Committed).**
* All credential references in `backend/src/main/resources/application.yml` and `docker-compose.yml` utilize environment variable interpolation with fallback dummy tokens.
* `application-prod.yml` enforces zero-default injection:
  - `password: ${DB_PASSWORD}`
  - `secret: ${JWT_SECRET}`
  - If these are omitted at container boot time in production, Spring Boot will intentionally fail fast rather than boot insecurely.

### 2. Frontend Secrets Leakage
* **Audit Finding:** **PASS (Frontend is Clean).**
* Analysis of `frontend/src` confirms that only `import.meta.env.VITE_API_URL` is referenced.
* No Google Gemini API keys, JWT signing keys, database passwords, or internal tokens are present in client-side code.

### 3. Missing Environment Variables on Vercel
* **Risk:** When creating the Vercel project with Root Directory `frontend`, Vite will compile with default `/api`.
* **Impact:** Without setting `VITE_API_URL` or defining a `vercel.json` rewrite, all frontend scan, chat, and authentication calls will 404 in production.

### 4. Incorrect Production API Base URLs & Mixed-Content Blocking
* **Risk:** If the Vercel frontend is accessed via `https://prahar.vercel.app`, but the backend API is deployed over plain HTTP (`http://api.prahar.example.com`), modern browsers will trigger **Mixed-Content Block** and reject every API call before it leaves the browser.
* **Mitigation:** The backend must be assigned a custom domain with an SSL certificate, or deployed on a platform providing automatic HTTPS (e.g., Render/Railway default domains provide valid TLS).

### 5. CORS Restrictions
* **Risk:** The Spring Boot backend checks the `Origin` header against `${CORS_ALLOWED_ORIGINS}`.
* **Impact:** If the Vercel production domain (e.g., `https://prahar-security.vercel.app`) is not added to the backend's `CORS_ALLOWED_ORIGINS` variable, all cross-origin requests from the Vercel frontend will receive HTTP 403 Forbidden.

### 6. Authentication & Token Inconsistency
* **Risk:** In `AuthContext.jsx`, the key is `prahar_token`. In `client.js` line 15 and `NotificationBell.jsx` line 58, the key queried is `phishshield_token`.
* **Mitigation:** Standardize the storage key to `prahar_token` across `client.js` and `NotificationBell.jsx` prior to production release.

### 7. Insecure Database Configuration & DDL Rules
* **Risk:** In `application-prod.yml`, `spring.jpa.hibernate.ddl-auto` is set to `validate`.
* **Impact:** If the managed MySQL database is empty upon backend launch, the backend will crash on startup because tables do not exist.
* **Mitigation:** Run `database/schema.sql`, `database/seed.sql`, and `database/production_hardening.sql` against the managed MySQL database before starting the backend container.

### 8. Health Checks & Resiliency
* **Risk:** If the Python ML microservice is slow or restarting, scan requests could stall.
* **Current Safeguard:** `URLScanService.java` is already designed with graceful degradation: if `MLServiceClient` fails or times out (3000ms limit), it records the failure, marks `mlApplied = false`, and falls back to deterministic heuristic rule scoring without crashing.

### 9. URL-Analysis SSRF Security
* **Risk:** Malicious users submitting `http://169.254.169.254/latest/meta-data/` to scan cloud infrastructure.
* **Current Safeguard:** `SSRFProtectionService.java` explicitly blocks private and link-local IP resolution. Production deployments must keep `SSRF_PROTECTION_ENABLED=true` and `SSRF_ALLOW_PRIVATE_IPS=false`.

---

## TASK 4 — Exact Deployment Plan

### 1. Component-by-Component Hosting Architecture

```
                       +---------------------------------------+
                       |             END USER BROWSER          |
                       +---------------------------------------+
                                           |
                                           | HTTPS
                                           v
                       +---------------------------------------+
                       |           VERCEL EDGE CDN             |
                       |       (Hosts: frontend/ [Vite])       |
                       | Domain: https://prahar.vercel.app     |
                       +---------------------------------------+
                                           |
                  +------------------------+------------------------+
                  | (Option A: Direct HTTPS)| (Option B: Vercel      |
                  |  with CORS enabled)    |  Rewrite /api/(.*))    |
                  v                        v                        v
+-----------------------------------------------------------------------+
|                         BACKEND CONTAINER                             |
|          (Platform: Render / Railway / AWS ECS / Lightsail)           |
|                 Hosts: backend/ (Spring Boot 3.3.4)                   |
|               Domain: https://api.prahar.yourdomain.com               |
+-----------------------------------------------------------------------+
        |                                       |
        | Internal HTTP (Private Network)      | JDBC TLS/SSL (3306)
        v                                       v
+---------------------------------+   +---------------------------------+
|          ML MICROSERVICE        |   |       MANAGED MYSQL 8.0         |
| (Platform: Render / Railway)    |   | (Platform: Aiven / AWS RDS)     |
| Hosts: ml-service/ (FastAPI)    |   | Hosts: phishshield_db           |
+---------------------------------+   +---------------------------------+
        |
        | External HTTPS (TLS 1.3)
        v
+---------------------------------+
|      GOOGLE GEMINI API          |
|  (generativelanguage.googleapis)|
+---------------------------------+
```

### 2. Exact Vercel Project Configuration

When importing the Git repository in the Vercel Dashboard:

| Setting | Exact Value to Enter | Notes |
| :--- | :--- | :--- |
| **Project Name** | `prahar-frontend` | Or your preferred name |
| **Framework Preset** | `Vite` | Auto-detected by Vercel |
| **Root Directory** | `frontend` | **Critical:** Must select `frontend`, not repository root |
| **Build Command** | `npm run build` | Default Vite build command |
| **Output Directory** | `dist` | Generated build artifact folder |
| **Install Command** | `npm install` | Installs dependencies |
| **Node.js Version** | `20.x` | Stable LTS runtime |

#### Recommended `frontend/vercel.json` (To be added before deployment):
```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://<YOUR_DEPLOYED_BACKEND_URL>/api/:path*"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```
*Benefits:*
1. Resolves SPA client-side routing on page refresh.
2. Proxies `/api/*` seamlessly to the backend, completely eliminating browser CORS issues and allowing `VITE_API_URL` to remain `/api`.

### 3. Exact Environment Variable Names by Service

#### A. Vercel (Frontend Service)
* `VITE_API_URL` (Set to `/api` if using `vercel.json` rewrite, or full HTTPS URL `https://api.prahar.yourdomain.com/api` if querying cross-origin).

#### B. Backend Host (Render / Railway / AWS Container)
* `SPRING_PROFILES_ACTIVE` (Value: `prod`)
* `SERVER_PORT` (Value: `8080`)
* `DB_HOST`
* `DB_PORT` (Value: `3306`)
* `DB_NAME` (Value: `phishshield_db`)
* `DB_USER`
* `DB_PASSWORD`
* `DB_SSL_MODE` (Value: `REQUIRED`)
* `DB_VERIFY_SERVER_CERT` (Value: `true`)
* `JWT_SECRET` (256-bit cryptographically secure string)
* `JWT_EXPIRATION_MS` (Value: `86400000`)
* `CORS_ALLOWED_ORIGINS` (Comma-separated: `https://prahar.vercel.app,https://<your-custom-domain>`)
* `ML_SERVICE_URL` (Internal private URL or public HTTPS URL of FastAPI service)
* `ML_SERVICE_TIMEOUT_MS` (Value: `3000`)
* `SSRF_PROTECTION_ENABLED` (Value: `true`)
* `SSRF_ALLOW_PRIVATE_IPS` (Value: `false`)
* `AI_PROVIDER` (Value: `gemini`)
* `AI_API_KEY` (Your Google Gemini API Key)
* `AI_MODEL` (Value: `gemini-1.5-flash`)
* `AI_TIMEOUT_MS` (Value: `15000`)

#### C. ML Microservice Host (Render / Railway / AWS Container)
* `APP_ENV` (Value: `production`)
* `PORT` (Value: `8000`)
* `HOST` (Value: `0.0.0.0`)
* `LOG_URLS` (Value: `false`)

### 4. Exact Files Requiring Modifications

| File Path | Nature of Change | Reason for Change |
| :--- | :--- | :--- |
| `frontend/vercel.json` | **Create File** | Instructs Vercel on SPA routing fallbacks and optional `/api` proxy rewrites. |
| `frontend/src/api/client.js` | **Edit Line 15** | Change `phishshield_token` to `prahar_token` to match `AuthContext.jsx`. |
| `frontend/src/components/NotificationBell.jsx` | **Edit Line 58** | Change `phishshield_token` to `prahar_token` for notification polling consistency. |
| `frontend/src/api/chatApi.js` | **Edit Line 74** | Update error message from `port 8080` to generic `Prahar backend server is currently unreachable`. |

*(No files will be modified until you explicitly review and approve these exact edits).*

### 5. Third-Party Hosting & Account Requirements
To deploy the complete Prahar system into production:
1. **Vercel Account:** For hosting the React SPA frontend (Free tier / Pro).
2. **Cloud Container Host (Render, Railway, Fly.io, or AWS):**
   - 1 Container for Spring Boot (min 1GB RAM, recommended 2GB RAM).
   - 1 Container for Python FastAPI ML microservice (min 512MB RAM, recommended 1GB RAM).
3. **Managed MySQL 8.0 Database (Aiven, Railway, AWS RDS, or PlanetScale):**
   - 1 Database instance with TLS enabled (storage 5GB–10GB).
4. **Google AI Studio Account:**
   - Active Gemini API key for `gemini-1.5-flash`.

---

### 6. Step-by-Step Deployment Sequence

#### Step 1: Provision and Initialize the MySQL Database
1. Create a MySQL 8.0 instance on your chosen managed cloud provider (e.g., Aiven or Railway).
2. Connect using MySQL Workbench, DBeaver, or CLI.
3. Execute the database initialization scripts in sequence:
   ```bash
   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < database/schema.sql
   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < database/seed.sql
   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < database/production_hardening.sql
   ```
4. Confirm tables (`users`, `roles`, `url_scans`, `threat_indicators`, `chat_conversations`, `audit_logs`) exist.

#### Step 2: Deploy the Python ML Microservice
1. Create a Web Service on your container PaaS with Git Root Directory: `ml-service`.
2. Configure build environment using Dockerfile (`ml-service/Dockerfile`) or Python buildpack:
   - Build: `pip install -r requirements.txt`
   - Start: `uvicorn app.main:app --host 0.0.0.0 --port 8000`
3. Set environment variables: `APP_ENV=production`, `PORT=8000`.
4. Verify deployment by accessing: `https://<ml-service-host>/health`  
   Expected JSON response: `{"status": "UP", "service": "prahar-ml-service", "model_loaded": true}`.

#### Step 3: Deploy the Spring Boot Backend API
1. Create a Web Service on your container PaaS with Git Root Directory: `backend`.
2. Configure using Dockerfile (`backend/Dockerfile`) or Maven buildpack:
   - Build: `mvn clean package -DskipTests`
   - Start: `java -jar target/backend-1.0.0-SNAPSHOT.jar`
3. Configure all backend environment variables (Database, JWT, Gemini, ML Service URL).
4. Verify deployment by checking:
   - Liveness: `GET https://<backend-host>/api/health` -> `{"status": "UP"}`
   - Readiness: `GET https://<backend-host>/api/health/readiness` -> Database UP, ML Service UP.
   - Swagger Documentation: `https://<backend-host>/swagger-ui/index.html`.

#### Step 4: Deploy the Frontend to Vercel
1. Log in to [vercel.com](https://vercel.com) and click **"Add New Project"**.
2. Select your repository.
3. In Project Settings:
   - **Root Directory:** Set to `frontend`.
   - **Framework Preset:** `Vite`.
   - **Environment Variables:** Set `VITE_API_URL` to `https://<backend-host>/api` (or `/api` if using rewrites).
4. Click **Deploy**. Vercel will clone, run `npm install`, execute `npm run build`, and assign a production URL (e.g., `https://prahar.vercel.app`).
5. Update the backend's `CORS_ALLOWED_ORIGINS` to include the assigned Vercel URL.

---

### 7. Local Build & Test Commands Before Deployment

Run these commands locally in powershell to verify clean compilation across all tiers:

```powershell
# 1. Test Frontend Production Build (Verified Passing: 21.42s)
cd "d:\MCA notes\MINI PRO SIH\frontend"
npm run build

# 2. Test Frontend Unit / Heuristics Tests
npm test

# 3. Test Backend Compilation & Packaging
cd "d:\MCA notes\MINI PRO SIH\backend"
mvn clean package -DskipTests

# 4. Test Python ML Microservice
cd "d:\MCA notes\MINI PRO SIH\ml-service"
pytest tests/
```

---

### 8. Production Verification Checklist

Once deployed, execute this end-to-end verification checklist:

- [ ] **Frontend CDN Liveness:** Open `https://<your-vercel-domain>.vercel.app`. The Prahar navigation bar, quick scanner, and dark/light mode toggle render without console errors.
- [ ] **Backend Liveness:** `curl -I https://<your-backend-host>/api/health` returns HTTP 200 with `status: UP`.
- [ ] **Database Connectivity:** `GET https://<your-backend-host>/api/health/readiness` confirms `database.status: UP`.
- [ ] **ML Microservice Health:** Backend readiness confirms `ml_service.model_loaded: true`.
- [ ] **SSRF Security:** Submitting `http://127.0.0.1` or `http://169.254.169.254` into the URL scanner yields HTTP 400 with SSRF Violation.
- [ ] **URL Phishing Scan:** Scanning a known test domain (e.g., `http://secure-login-hdfcbank.account-verify.tk/login.php`) returns a structured composite score and risk classification.
- [ ] **Gemini AI Assistant:** Sending a message in the Prahar Assistant tab returns streaming/natural language incident advice with provider marked as `gemini`.
- [ ] **Authentication Flow:** Registering a new citizen user returns a valid JWT, creates a record in MySQL `users` table, and displays the authenticated user name in the top navigation bar.
- [ ] **Page Refresh Check:** Navigating to `/scanner`, `/assistant`, or `/awareness` and pressing Browser Reload (F5) successfully loads the page without 404 errors (verifying Vercel SPA routing).

---
*Report generated and archived for Prahar AI/ML Phishing Detection Platform.*
