# PHASE 34 — END-TO-END INTEGRATION AUDIT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Audit Date:** October 2026  
**Auditor:** Lead Systems Integration & QA Automation Specialist  
**Verification Baseline:** Working Tree Source Code, Integration Test Suite, and Service Schemas

---

## 1. System Integration Overview & Service Topology

Prahar operates across four interconnected tiers forming a polyglot defense ecosystem:

```
+-----------------------------------------------------------------------------------------+
|                               PRAHAR SERVICE INTEGRATION MAP                            |
+-----------------------------------------------------------------------------------------+
                                        |
                 +----------------------+----------------------+
                 |                                             |
                 v                                             v
+----------------------------------+          +----------------------------------+
| [ WEB FRONTEND ]                 |          | [ BROWSER EXTENSION ]            |
| - React 18.2 + Vite 5 (Port 5173)|          | - Chrome Manifest V3             |
| - Axios REST API clients         |          | - Background Service Worker      |
| - Server-Sent Events subscriber  |          | - Content & Popup Scripts        |
+----------------------------------+          +----------------------------------+
                 |                                             |
                 +----------------------+----------------------+
                                        | HTTP REST / JSON (Port 8080)
                                        v
+-----------------------------------------------------------------------------------------+
| [ CORE API GATEWAY & SECURITY PERIMETER ] - Spring Boot 3.3.4 (Java 17)                |
|   Security Chains:                                                                      |
|     - RequestCorrelationFilter (Trace ID header: `X-Request-ID`, MDC logging)           |
|     - RateLimitingFilter (Bucket4j: 60 scans/min, 15 auth/min per IP)                   |
|     - SecurityConfig (Stateless JWT authentication & CORS perimeter)                    |
|   Controllers:                                                                          |
|     - URLScanController (`/api/scans/url`, `/api/scans/ml-status`)                       |
|     - UnifiedImageAnalysisController (`/api/scans/unified-image`)                       |
|     - HealthController (`/api/health`, `/api/health/readiness`)                         |
|     - ChatController (`/api/chat/message`, `/api/chat/conversations`)                   |
|     - NotificationController (`/api/notifications`, `/api/notifications/stream`)        |
|   Core Integration Services:                                                            |
|     - URLScanService (Main Orchestration Engine)                                        |
|     - SSRFProtectionService (DNS pre-resolution IP filtering)                           |
|     - MultiSignalScoringEngine (Mathematical signal fusion)                             |
|     - MLServiceClient (HTTP REST client communicating with port 8000)                   |
|     - QRCodeService (Google ZXing barcode decoding engine)                              |
|     - OCRService / ScreenshotTextAnalyzer (Tesseract native wrapper & NLP engine)       |
|     - SensitiveDataScrubber (Luhn payment card and secret redactor)                     |
+-----------------------------------------------------------------------------------------+
                 |                                                |
       REST HTTP / JSON (Port 8000)                   JPA / Hibernate (In-Memory / Port 3306)
                 v                                                v
+---------------------------------------------+  +----------------------------------------+
| [ ML MICROSERVICE ] - FastAPI (Python 3.11) |  | [ PERSISTENCE LAYER ]                  |
| - `main.py` (ASGI app on port 8000)         |  | - In-Memory H2 DB (`test`/`dev` mode)   |
| - `prediction_service.py` (Orchestrator)    |  | - MySQL 8.0 Cluster (`prod` mode)      |
| - `url_features.py` (30 feature extractor)  |  | - Tables: `url_scans`, `users`,        |
| - `models/best_model.joblib` (LogReg)       |  |   `threat_detections`, `notifications`,|
| - Latency: <5ms synchronous inference       |  |   `financial_transactions`             |
+---------------------------------------------+  +----------------------------------------+
```

---

## 2. Service Communication Matrix

| Source | Destination | Transport / Port | Payload / Schema | Auth / Headers | Verified Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **Frontend SPA** | Spring Boot Backend | HTTP / Port 8080 | JSON (`URLScanRequest`, `ChatRequest`, Multipart) | `X-Request-ID`, `Authorization: Bearer <JWT>` | **`VERIFIED`** |
| **Browser Extension** | Spring Boot Backend | HTTP / Port 8080 | JSON (`URLScanRequest`) | `X-Request-ID`, JSON content headers | **`VERIFIED`** |
| **Spring Boot Backend** | Python ML Service | HTTP / Port 8000 | `POST /predict` (`{"url": "..."}`) | `Content-Type: application/json`, 2000ms timeout | **`VERIFIED`** |
| **Spring Boot Backend** | H2 Database | In-Memory JDBC | SQL (`jdbc:h2:mem:phishshield`) | SA user / in-memory | **`VERIFIED`** |
| **Spring Boot Backend** | MySQL Database | TCP / Port 3306 | SQL (`jdbc:mysql://...`) | HikariCP pool, credentials via env vars | **`PARTIALLY VERIFIED`** |
| **Spring Boot Backend** | Google Gemini LLM | HTTPS (Port 443) | REST JSON (`generativelanguage.googleapis.com`) | `x-goog-api-key: ${GEMINI_API_KEY}` | **`PARTIALLY VERIFIED`** |
| **Spring Boot Backend** | Tesseract OCR | OS Local IPC | ProcessBuilder (`tesseract.exe`) | System PATH binary execution | **`PARTIALLY VERIFIED`** |

---

## 3. Communication Paths & Failure Degradation Verification

### 3.1 Backend to ML Microservice Communication
- **Contract:** `POST http://localhost:8000/predict` accepts `PredictionRequest(url: str)` and returns `PredictionResponse(url, prediction, risk_level, risk_score, ml_probability, confidence, ...)`.
- **Timeout Policy:** Implemented in `MLServiceClient.java` using a strict **2,000ms read timeout** via Spring's `RestTemplate`.
- **Graceful Degradation Verified:** When the ML microservice is unreachable, `MLServiceClient` traps `RestClientException`, logs a structured warning with correlation ID, and sets `MLPredictionResult.isAvailable(false)`. The `MultiSignalScoringEngine.java` dynamically redistributes signal weights (65% heuristic rules, 35% threat intelligence), preventing backend 500 errors.

### 3.2 Frontend to Backend Communication
- **Contract:** Handled by Axios clients in `frontend/src/api/scanApi.js`, `incidentApi.js`, `chatApi.js`, `notificationApi.js`.
- **Response Standard:** Backend wraps all payloads in `ApiResponse<T>` (`success: boolean`, `message: string`, `data: T`, `timestamp: string`).
- **Error Handling:** When the backend returns HTTP 400 (SSRF or invalid input) or HTTP 429 (Rate Limit Exceeded), Axios interceptors catch the standardized error envelope and render non-blocking UI alert banners.

### 3.3 Database Connectivity & Schema Migrations
- **Academic Demonstration Mode:** Boots instantly on `H2 In-Memory Database` (`jdbc:h2:mem:phishshield;DB_CLOSE_DELAY=-1`). Hibernate validates entity mappings automatically.
- **Production Mode:** Scripted migrations versioned under `database/`:
  - `migration_v2.sql`: Transaction analysis and fraud incident entities.
  - `migration_v3.sql`: Notification and preference schema.
  - `production_hardening.sql`: B-tree indexes on `normalized_url`, `created_at`, `client_ip`.

---

## 4. Integration Defect & Risk Assessment

| Component | Potential Risk / Defect | Severity | Implemented Mitigation | Audit Verdict |
| :--- | :--- | :---: | :--- | :---: |
| **SSRF Perimeter** | Attackers submit loopback or cloud metadata IPs to probe internal networks | **P0** | `SSRFProtectionService.java` resolves DNS pre-fetch and blocks RFC-1918, loopback, and 169.254.169.254 | **`VERIFIED`** |
| **API Rate Limiting** | Volumetric DoS attempts overwhelm scanning threads | **P1** | `RateLimitingFilter.java` enforces token-bucket limits (60 scans/min, 15 auth/min per IP) | **`VERIFIED`** |
| **PII Data Leakage** | Users paste raw credit cards or passwords in SMS scans | **P1** | `SensitiveDataScrubber.java` executes Luhn algorithm validation and masks card/PIN sequences | **`VERIFIED`** |
| **Polyglot File Upload** | Malicious executable masked as PNG/JPEG uploaded to image scanner | **P1** | `ImageSecurityValidator.java` verifies binary magic bytes and strictly blocks SVG / executable types | **`VERIFIED`** |
| **ML Outage** | Python process crashes or hangs | **P2** | `MLServiceClient.java` fails over to 30-rule heuristic scoring with zero downtime | **`VERIFIED`** |
| **Native OCR Missing** | Host machine lacks `tesseract.exe` in Windows PATH | **P2** | `OCRService.java` safely returns empty string and logs missing binary without throwing exceptions | **`PARTIALLY VERIFIED`** |
| **Gemini Key Missing** | No external Google API key configured | **P2** | `ChatService.java` routes queries to local deterministic `RuleBasedNlpEngine.java` | **`PARTIALLY VERIFIED`** |
