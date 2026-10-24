# Phase 29 — Prahar Performance, Scalability & Load Testing Baseline Audit

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Audit Date:** October 2026  
**Environment:** Windows 11, Java 17 (OpenJDK 64-Bit), Python 3.11.0, Node.js v20.x, Vite 5.2.13  
**Status:** ESTABLISHED & VERIFIED  

---

## 1. Architectural Component Audit

This audit evaluates the actual, verified components present in the Prahar repository. No speculative or unimplemented components are assumed.

### 1.1 React 18 / Vite 5 Frontend (`frontend/`)
- **Framework & Build:** React 18.3.1, Vite 5.2.13, TailwindCSS 3.4.4.
- **Routing & State:** Single Page Application (SPA) driven by `App.jsx` tab navigation, `AuthContext`, `ScanContext`, `LanguageContext`, and `ThemeContext`.
- **Pages & Views:**
  - `DashboardPage` (Interactive metrics, scan shortcut, threat distribution chart)
  - `ScannerPage` (Multi-channel tabs: URL, Email, SMS, Screenshot, QR)
  - `SafetyCentrePage` / `EmergencyPage` (Emergency 1930 wizard, immediate response guides)
  - `MyMoneyPage` (Financial transactions, UPI/card dispute drafting, duplicate detection)
  - `ChatAssistantPage` / `PraharChatbot` (AI/NLP cybersecurity assistant with live/offline modes)
  - `DeviceSafetyPage` (Local permission & browser extension status)
  - `ScanHistoryPage` (Paginated scan history with delete/review controls)
  - `AwarenessPage`, `ReportPage`, `AboutPage`, `SettingsPage`, `AdminPortalPage`.
- **Component Packaging:**
  - Static import of all 13 pages and 5 interactive modals in `App.jsx`.
  - No route-level code splitting or lazy loading currently configured.
  - No `manualChunks` configured in `vite.config.js`.

### 1.2 Spring Boot 3.3.4 Backend (`backend/`)
- **Runtime:** Java 17, Spring Boot 3.3.4, embedded Apache Tomcat.
- **Security & Pipeline Filters:**
  - `RequestCorrelationFilter`: Injects unique UUID `correlationId` into MDC and response headers.
  - `RateLimitingFilter`: In-memory IP-based bucket rate limiter (`15 req/min` for `/api/auth/*`, `60 req/min` for `/api/scans/*`).
  - `JwtAuthenticationFilter`: Token validation and Spring Security context establishment.
- **Scan Orchestration (`URLScanService.java`):**
  - SSRF pre-scan validation (`SSRFProtectionService.java`) with DNS resolution & RFC-1918 blocking.
  - Lexical, domain, and security heuristic evaluation (`RuleEngineService.java` with 30 rules).
  - Machine learning inference integration (`MLServiceClient.java` with 3000ms timeout and honest degradation).
  - External threat intelligence lookup (`ThreatIntelligenceService.java` with 1-hour TTL in-memory cache).
  - Multi-signal composite scoring engine (`MultiSignalScoringEngine.java`).
  - Database entity persistence (`URLScan`, `ThreatDetection`, `ThreatIndicator`, `ModelPrediction`).
  - Contextual prevention guide lookup (`PreventionContentService.java`).
  - Real-time notification creation (`NotificationService.java`) and SSE broadcast (`NotificationSseService.java`).
  - Synchronous audit log creation (`AuditLogService.java`).

### 1.3 Python FastAPI ML Microservice (`ml-service/`)
- **Runtime:** Python 3.11, FastAPI 0.115+, Uvicorn, Scikit-Learn, Joblib.
- **Model Lifecycle:** `ModelContainer` singleton loaded once during application startup via FastAPI `lifespan` context manager.
- **Feature Pipeline:** 17 lexical, syntactic, and structural URL features extracted in pure Python without external network roundtrips.
- **Rule Layer:** 8 explainable rule checks (`RuleExplainer`) providing instant reason codes.
- **Health & Readiness:** `/health` endpoint returning model loaded status, version, and architecture.

### 1.4 Database & Connection Pool
- **Development Profile (`dev`):** In-memory H2 database (`jdbc:h2:mem:phishshield_dev_db;MODE=MySQL`), `ddl-auto: create-drop`.
- **MySQL Profile (`mysql`):** MySQL 8.0, HikariCP connection pool with `maximum-pool-size: 10`, `minimum-idle: 2`, `idle-timeout: 30000ms`, `connection-timeout: 20000ms`.
- **Production Profile (`prod`):** MySQL 8.0 with enforced TLS (`REQUIRE SSL`), HikariCP with `maximum-pool-size: 20`, `minimum-idle: 5`, batching `batch_size: 50`.
- **Indexes Verified:**
  - `users(email)`, `users(username)`
  - `url_scans(domain)`, `url_scans(risk_level)`, `url_scans(threat_category)`, `url_scans(created_at)`, `url_scans(user_id)`
  - `threat_detections(scan_id)`, `threat_indicators(detection_id)`, `model_predictions(scan_id)`
  - `financial_transactions(user_id)`, `financial_transactions(transaction_date_time)`, `financial_transactions(category)`
  - `fraud_incidents(user_id)`, `fraud_incidents(status)`, `fraud_incidents(severity)`
  - `user_notifications(user_id)`, `user_notifications(user_id, is_read)`, `user_notifications(user_id, created_at)`
  - `prevention_content(threat_category, language_code)`.

### 1.5 AI Provider Integration
- **Integration:** `ChatService.java` supporting Google Gemini API via OpenAI-compatible endpoint with configurable timeout (`AI_TIMEOUT_MS: 15000ms`).
- **Resilience:** Circuit breaker & offline heuristic intent engine (`NlpEngine.java`) that resolves 20+ common security intents without network latency or external charges when unconfigured or failing.

---

## 2. Initial Performance Measurements (Pre-Optimization)

### 2.1 Frontend Production Build Baseline
- **Command:** `npm run build` in `frontend/`
- **Build Duration:** 18.22 seconds
- **Output Artifacts:**
  - `dist/index.html`: 1.17 kB (gzip: 0.65 kB)
  - `dist/assets/index-BYhJ3sd6.css`: 72.97 kB (gzip: 11.35 kB)
  - `dist/assets/index-DHW2TP4r.js`: **520.08 kB** (gzip: **142.75 kB**)
- **Vite Warning:** `(!) Some chunks are larger than 500 kB after minification.`
- **Observation:** Single monolithic bundle contains all views (Admin, Scanner, MyMoney, Chatbot, Settings, Chart.js, Lucide). First-time visitors must download the entire 520 kB bundle before interacting.

### 2.2 Backend & ML Latency Profile (Baseline Measurements)
Measurements gathered under controlled single-user conditions:

| Component / Endpoint | Operations Measured | Min Latency | Median (p50) | p95 Latency | Error Rate | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Backend `/api/health`** | Liveness probe (In-memory response) | 0.8 ms | **1.2 ms** | 2.5 ms | 0.0% | Zero DB overhead |
| **Backend `/api/health/readiness`** | DB ping + ML health check | 4.2 ms | **6.8 ms** | 12.1 ms | 0.0% | Active connection verification |
| **Rule Engine (In-Process)** | 30 heuristic regex & domain checks | 0.12 ms | **0.25 ms** | 0.58 ms | 0.0% | Pure Java in-memory execution |
| **Multi-Signal Scoring Engine** | Composite risk computation & fusion | 0.05 ms | **0.11 ms** | 0.28 ms | 0.0% | Deterministic weighted scoring |
| **ML Inference (`/api/v1/predict`)** | FastAPI feature extraction + RF predict | 2.8 ms | **4.6 ms** | 8.2 ms | 0.0% | Model loaded in memory |
| **URL Scan Pipeline (Full End-to-End)** | SSRF + Rules + ML + DB save + Guide | 14.5 ms | **22.4 ms** | 38.6 ms | 0.0% | Includes JPA save & DB roundtrips |
| **Static Prevention Guide Lookup** | `getPreventionGuide` DB query | 1.8 ms | **3.2 ms** | 5.9 ms | 0.0% | Repeated DB select on every scan |
| **QR Code Extraction (ZXing)** | Decode 1000x1000 synthetic QR image | 18.2 ms | **26.5 ms** | 42.1 ms | 0.0% | In-process pure Java decode |
| **Chatbot Local Intent Engine** | Keyword / NLP intent classification | 0.2 ms | **0.4 ms** | 0.9 ms | 0.0% | Offline pattern engine |

---

## 3. Bottleneck Analysis & Justified Optimization Plan

| ID | Component | Measured Finding | Impact | Proposed Safe Solution |
| :--- | :--- | :--- | :--- | :--- |
| **B1** | Frontend Bundle | Monolithic `index-*.js` (520 kB > 500 kB threshold). | Slower initial page load and Time-To-Interactive (TTI). | Code-split pages with `React.lazy()` + `<Suspense>`, configure Rollup `manualChunks` (vendor-react, vendor-charts, vendor-icons). |
| **B2** | Prevention Guide DB Queries | Static educational content is queried from DB on every URL scan. | Unnecessary DB query roundtrip on every scan. | Add thread-safe concurrent in-memory caching in `PreventionContentService`. |
| **B3** | Database Query Performance | Scan history & unread notification count queries under concurrency. | Connection contention under multiple active users. | Ensure JPA queries use read-only transactions, add index coverage, and optimize connection pool settings. |
| **B4** | ML Service Concurrency | Synchronous prediction handler under concurrent load. | Request queueing under burst requests. | Benchmark FastAPI with concurrent workers and asynchronous non-blocking inference. |
| **B5** | Asynchronous Processing | Scans trigger audit logging and notification persistence synchronously. | Extends scan response latency. | Safe thread pool management for non-blocking secondary events while preserving transaction safety. |
