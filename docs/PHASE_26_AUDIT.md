# PRAHAR 2.0 — PHASE 26 RELEASE AUDIT & INVENTORY REPORT

**Evaluation Timestamp:** October 24, 2026  
**Git Baseline Commit:** `bd56a5c80c6b56047261b5df300e25a2f2c695ad`  
**Test Suite Pass Rate:** **161 / 161 Tests Passing (100%)**  
**Audit Purpose:** Comprehensive release candidate verification and demonstration readiness audit.

---

## 1. Complete Component Inventory

| Component / Subsystem | Technology Stack | Primary Directory | Operational Role |
| :--- | :--- | :--- | :--- |
| **Frontend Web Application** | React 18, Vite 5, Tailwind CSS, Lucide Icons | [`frontend/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/) | Interactive Single Page Application (SPA) offering multi-vector threat scanner, financial anomaly ledger, fraud response centre, and AI chat interface. |
| **Backend REST API** | Spring Boot 3.3.4, Java 17/21/25, Maven, Spring Security (JWT), Spring Data JPA | [`backend/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/) | Central business logic, SSRF firewall, rate limiting, request correlation, ZXing QR decoding, OCR text analysis, and notification engine. |
| **Machine Learning Service** | Python 3.11, FastAPI 0.111.0, Uvicorn, Scikit-learn, XGBoost, Pandas | [`ml-service/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/ml-service/) | 30-feature lexical/structural URL extractor, ML inference endpoint (`/predict`), model evaluation benchmarks, and governance pipeline. |
| **Relational Database** | MySQL 8.0 / Development In-Memory H2 | [`database/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/database/) | Persistent storage for users, roles, URL scans, financial transactions, fraud incidents, chat conversations, and real-time user notifications. |
| **Browser Extension** | Chrome Manifest V3, Vanilla JavaScript, HTML/CSS | [`browser-extension/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/browser-extension/) | Lightweight real-time active tab URL and DOM link scanner interacting with the Spring Boot backend. |
| **Edge Reverse Proxy** | Nginx 1.27 Alpine | [`frontend/nginx.prod.conf`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/nginx.prod.conf) | Edge TLS termination (HTTP $\to$ HTTPS), strict host routing, rate limiting, SSE buffering bypass, and security headers. |
| **Local Orchestrator** | Windows PowerShell 5.1/7+ | [`start_services.ps1`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/start_services.ps1) | Single-command local startup script launching FastAPI, Spring Boot, and Vite with zero hardcoded credentials. |

---

## 2. Verified Working Functionality

The following capabilities have been tested and verified through automated test suites and runtime execution:

1. **Multi-Channel Threat Scanning:**
   - **URL Scanner:** Detects typosquatting, suspicious TLDs, IP addresses in hostnames, and excessive subdomains via lexical rules + ML prediction.
   - **QR Code Decoder:** Decodes QR codes using ZXing, categorizes payloads (`UPICode`, `URL`, `WiFi`, `PlainText`), and passes extracted URLs to the scanner.
   - **Screenshot & OCR Text:** Evaluates image magic bytes (PNG, JPEG, WebP, GIF, BMP) to reject malicious executables. Gracefully returns `unextracted` when Tesseract is absent on host OS without fabricating fake text.
   - **SSRF Defense Guard:** Actively resolves hostnames and blocks requests targeting loopback (`127.0.0.1`), link-local (`169.254.169.254`), or private RFC-1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
2. **Conversational Safety Assistant ("Ask Prahar"):**
   - Offline 22-intent rule engine handles common cybersecurity queries, emergency steps, and fraud reporting guidance without external dependencies.
   - Optional server-side Google Gemini proxying supported when `AI_API_KEY` is securely injected.
3. **Financial Anomaly & Transaction Monitor ("My Money"):**
   - Ledger recording via manual entry, pasted SMS alerts, and bank CSV upload.
   - SHA-256 fingerprint deduplication prevents duplicate CSV records.
   - Statistical anomaly detection flags transactions exceeding 3x the running debit average.
4. **Fraud Incident Response Centre:**
   - Integrated Indian Cybercrime protocols: 1930 Helpline guidance and [cybercrime.gov.in](https://cybercrime.gov.in/) direct workflow targeting the 2–3 hour "Golden Hour".
   - Automated dispute letter generator quoting the RBI Zero-Liability circular (`RBI/2017-18/15`) and IT Act 2000 (`Sections 43A, 66C, 66D`).
5. **Real-Time Notification Centre:**
   - High-severity scan alerts ($\ge 35$ risk score) trigger Server-Sent Events (SSE) and database persistence.
   - SHA-256 idempotency keys prevent duplicate notifications for identical scan results.
   - Strict DB-level ownership checks enforce cross-user isolation.
6. **Production Infrastructure Manifests:**
   - Three-tier network segmentation defined in [`docker-compose.prod.yml`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/docker-compose.prod.yml).
   - Zero host exposure for private ports (MySQL 3306, Spring Boot 8080, FastAPI 8000).

---

## 3. Failed, Degraded or Unverified Functionality (Honest Assessment)

| Feature / Subsystem | Status | Technical Cause & Honest Limitation |
| :--- | :--- | :--- |
| **Optical Character Recognition (OCR)** | **Gracefully Degraded** | Tesseract OCR binary is not bundled in standard Windows/Mac environments by default. When absent, the system honestly sets `ocrStatus: "UNEXTRACTED"` and relies on QR decoding + heuristic analysis. |
| **Google Gemini Live Cloud Calls** | **Unit-Mocked / Config-Dependent** | Gemini integration is unit-mocked in automated tests. Live cloud calls require the operator to provide a valid, active API key in process environment. Offline rule engine acts as primary baseline. |
| **External Threat Intelligence (SafeBrowsing / VirusTotal)** | **Disabled by Default** | Controlled by `THREAT_INTEL_ENABLED=false` and `THREAT_INTEL_REQUIRE_CONSENT=true` to prevent leaking scanned user URLs to external search engines without user consent. |
| **Production Cloud TLS** | **Manifests Ready / Not Deployed** | Production Nginx TLS termination is configured in `frontend/nginx.prod.conf`, but live certificate generation requires a publicly reachable registered domain name. |

---

## 4. Existing Test Coverage Matrix

| Test Suite | Framework / Tool | Test Classes / Modules | Total Tests | Pass Count | Failure Count |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Spring Boot API** | Maven Surefire, JUnit 5, Mockito, SpringBootTest | 17 Classes | **109** | **109** | **0** |
| **FastAPI ML Microservice** | Pytest 8.x, AnyIO, Starlette TestClient | 5 Modules | **32** | **32** | **0** |
| **React Frontend** | Node Native Test Runner (`node --test`) | `src/tests/*.test.js` | **10** | **10** | **0** |
| **Browser Extension** | Node Custom Chrome Mock Runner | `test_extension.js` | **10** | **10** | **0** |
| **TOTAL** | — | — | **161** | **161** | **0** |

---

## 5. Security & Risk Findings

1. **Secret Scanning:**
   - Ripgrep scan across all tracked files, Git staging, and frontend bundles confirmed **zero committed Gemini API keys (`AIzaSy...`) or database passwords**.
   - `.env.example` contains only standardized placeholder strings.
2. **PII Sanitization:**
   - `SensitiveDataScrubber` redacts credit card numbers (Luhn-compliant), OTPs, CVVs, and banking PINs from incident notes and chat history before database persistence.
3. **Cross-Tenant Access Controls:**
   - Endpoints require valid Bearer JWT tokens. User IDs in path variables are verified against the authenticated `SecurityContext`. Mismatched requests return `403 Forbidden`.
4. **Rate Limiting:**
   - Bucket4j-based `RateLimitingFilter` bounds public scan requests to 60 req/min and auth requests to 15 req/min per IP address.

---

## 6. Recommendations & Action Items

1. **Academic Demonstration:** Use the synthetic fixtures in [`media/demo_fixtures/`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/media/demo_fixtures/) to demonstrate benign vs. high-risk detection safely.
2. **Production Staging:** Provision a managed MySQL instance with TLS enforcement (`application-prod.yml`) and deploy via `docker-compose.prod.yml` behind a valid DNS domain.
