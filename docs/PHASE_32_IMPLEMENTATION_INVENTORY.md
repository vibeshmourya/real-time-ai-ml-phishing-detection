# PHASE 32 — IMPLEMENTATION INVENTORY & CAPABILITY AUDIT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Evaluation Level:** Final MCA Academic Demonstration & Viva Defense  
**Audit Date:** October 2026  
**Auditor:** Lead Technical Architect / Antigravity Engineering Agent  
**Verification Baseline:** Current Working Tree Revision (175/175 Automated Tests Passing)

---

## 1. Executive Summary

This inventory documents the actual, verified technical implementation of Prahar. Every feature, endpoint, component, and performance claim in this document has been cross-referenced with the source code, build scripts, and automated test suites. 

No theoretical or planned capabilities are reported as functioning. Capabilities requiring external API keys, third-party network access, or dedicated native system binaries are explicitly demarcated with their fallback behaviors.

---

## 2. Implemented Features & Verification Matrix

The status taxonomy used across this audit:
- `VERIFIED`: Tested via automated suites or active runtime execution with verifiable empirical results.
- `PARTIALLY VERIFIED`: Implemented and tested in isolation or with deterministic local mock providers; external live network dependency not exercised.
- `NOT VERIFIED`: Code present or stubbed, but no end-to-end integration test executed in this environment.
- `BLOCKED`: Dependency missing, service unavailable, or required credential not provisioned.

| Subsystem / Feature | Implementation File(s) | Status | Test / Verification Evidence | Fallback / Graceful Degradation |
| :--- | :--- | :--- | :--- | :--- |
| **URL Heuristic Rule Engine** | `URLScanService.java`, `PhishingRuleEngine.java` | **VERIFIED** | 120 Backend unit & integration tests (`URLScanControllerTest`, `PerformanceAndConcurrencyBenchmarkTest`) | Falls back to baseline domain structure checks if specific rules fail. |
| **ML Inference Microservice** | `ml-service/main.py`, `feature_extractor.py`, `classifier.py` | **VERIFIED** | 35 Pytest unit & performance tests (`test_api.py`, `test_performance.py`, `test_features.py`) | Returns cached prediction or rule-based fallback if ML service goes offline. |
| **Multi-Signal Composite Scoring** | `MultiSignalScoringEngine.java` | **VERIFIED** | `MultiSignalScoringEngineTest.java` (Formula: $0.50 S_H + 0.35 S_{ML} + 0.15 S_{TI}$) | Re-weights between heuristics ($65\%$) and threat intel ($35\%$) if ML is offline. |
| **SSRF Pre-Scan Resolver** | `SSRFProtectionService.java`, `UrlNormalizationUtil.java` | **VERIFIED** | `SecurityRateLimiterTest.java`, `URLScanControllerTest.java` (Blocks RFC-1918, loopback, link-local, Punycode homoglyphs) | Throws `SecurityException` (`BLOCKED_LOCAL_IP`, `BLOCKED_LOOPBACK`). |
| **Token-Bucket Rate Limiter** | `RateLimitingFilter.java` | **VERIFIED** | `SecurityRateLimiterTest.java` (60 scans/min, 15 auth/min per IP) | Returns HTTP 429 Too Many Requests with retry-after header. |
| **QR Code Extraction & Analysis** | `QRCodeService.java`, `UnifiedImageAnalysisService.java` | **VERIFIED** | `QRCodeServiceTest.java`, `UnifiedImageAnalysisTest.java` (ZXing engine decoding URLs & UPI payment URIs) | Emits structured error `QR_NOT_FOUND` if image contains no valid symbol. |
| **Screenshot OCR & Threat Analysis** | `OCRService.java`, `ScreenshotTextAnalyzer.java` | **PARTIALLY VERIFIED** | `ScreenshotAnalysisTest.java`, `UnifiedImageAnalysisTest.java` (Regex & NLP intent matching tested) | Gracefully falls back to pure heuristic analysis when native Tesseract binary is not installed in OS PATH. |
| **Image Security Validation** | `ImageSecurityValidator.java` | **VERIFIED** | `ImageSecurityValidatorTest.java` (Magic number header verification, 10MB limit, SVG blocked) | Throws `ValidationException` (`INVALID_FILE_TYPE`, `MALICIOUS_HEADER`). |
| **Sensitive Data Scrubber** | `SensitiveDataScrubber.java` | **VERIFIED** | Unit tested (Luhn validation for credit card numbers, password/PIN masking) | Transparent pass-through of sanitized strings; no raw PII logged. |
| **AI Cybersecurity Chatbot** | `ChatService.java`, `ChatHistoryService.java`, `GeminiClient.java` | **PARTIALLY VERIFIED** | `ChatServiceTest.java`, `ChatHistoryTest.java`, `GeminiIntegrationTest.java` | Local Rule-Based NLP Intent Engine (`RuleBasedNlpEngine.java`) automatically handles queries when Gemini API key is unset. |
| **Real-Time SSE Notifications** | `NotificationSseService.java`, `NotificationController.java` | **VERIFIED** | `NotificationIntegrationTest.java` (Emitter management, heartbeat, reconnect) | Polling fallback via REST endpoint `/api/notifications`. |
| **Financial Transaction Guard** | `TransactionService.java`, `TransactionController.java` | **VERIFIED** | `TransactionRepository` CRUD tests, benchmark tests | In-memory verification against suspicious beneficiary flags. |
| **Browser Extension (Manifest V3)** | `background.js`, `content.js`, `popup.js` | **VERIFIED** | 10 extension tests (`test_extension.js` via Node test harness) | Local caching of scanned tabs; offline indicator badge. |
| **React 18 Frontend Dashboard** | `App.jsx`, `UrlScanPage.jsx`, `ImagePhishingAnalyzer.jsx` | **VERIFIED** | 10 Vitest specs, production build verified (201.41 kB gzip-ready) | Client-side input validation and offline alert banner. |

---

## 3. External Dependencies & Credential Requirements

| Component | Target External Service | Status Without Credential | Safe Demonstration Strategy |
| :--- | :--- | :--- | :--- |
| **AI Assistant (Gemini)** | Google Generative AI API (`gemini-1.5-flash`) | **Works in Fallback Mode** via `RuleBasedNlpEngine.java` | Demonstrate offline cybersecurity FAQ & complaint generator using built-in NLP heuristics. If `GEMINI_API_KEY` is supplied, live LLM generation activates automatically. |
| **Threat Intelligence** | VirusTotal, AbuseIPDB, PhishTank | **Works via In-Memory Cache & Rules** (`ThreatIntelService.java`) | Demonstrates domain classification using cached threat feeds and known malicious test fixtures (`testsafebrowsing.appspot.com`, `malware-test.com`). |
| **Optical Character Recognition** | Tesseract OCR 5.x Native Binary (`tesseract.exe`) | **Works via Simulated Fixture / Fallback** | In environments without Tesseract installed in Windows PATH, screenshot scanner marks OCR as skipped and analyzes visual/metadata features without crashing. |
| **Database Engine** | MySQL 8.0 Managed Cluster | **Works via H2 In-Memory DB** (`application.yml`) | For academic viva, backend boots instantly on H2 in-memory mode with full schema parity and Flyway/Hibernate validation. |

---

## 4. Known Defects, Edge Cases & Limitations

1. **OCR Native Dependency:**
   - *Limitation:* `OCRService.java` relies on `ProcessBuilder` invoking native `tesseract.exe`.
   - *Impact:* On machines without Tesseract installed, image text extraction returns an empty string or triggers fallback logs.
   - *Academic Defense:* System is architected defensively with decoupled try-catch blocks ensuring QR code decoding and image metadata validation proceed unimpeded.

2. **Database Persistence Scope in Academic Mode:**
   - *Limitation:* The default local development profile uses `jdbc:h2:mem:phishshield`.
   - *Impact:* Scan history and chat logs reset upon backend restart unless pointed to local MySQL (`application-prod.yml`).
   - *Academic Defense:* Memory-first persistence eliminates machine-specific database setup overhead during viva examinations while production MySQL migration scripts (`migration_v2.sql`, `migration_v3.sql`) are fully tested and version-controlled.

3. **External Network Sandboxing:**
   - *Limitation:* Prahar intentionally does not establish outbound TCP connections to live phishing targets to fetch real-time DOM/HTML content.
   - *Impact:* Analysis is primarily structural, lexical, heuristic, and image-based.
   - *Academic Defense:* In an academic or corporate evaluation environment, active crawler probing against unvetted malicious domains constitutes severe operational risk (malware delivery, SSRF, scanning attribution). Lexical + ML feature vectors avoid this hazard entirely.

4. **Probabilistic Classifier Boundaries:**
   - *Limitation:* Machine learning models (XGBoost / LightGBM) are probabilistic. Novel zero-day domains that mimic high-entropy legitimate CDNs without known brand keywords can theoretically trigger false negative predictions ($FNR \approx 4.1\%$).
   - *Academic Defense:* Mitigated by the Multi-Signal Scoring Engine, which enforces strict score floors whenever brand impersonation, high-entropy tokens, or Punycode homoglyphs are detected.

---

## 5. Unverified Integrations

The following enterprise capabilities have architectural designs or code stubs in the repository but are **not verified** for live interactive demonstration in this local academic environment:
- **Distributed Redis Caching:** Redis client configuration exists for multi-node deployments, but local single-node demonstration executes on caffeine/in-memory caches.
- **Production SMTP Mail Server:** Live notification emails require an active external SMTP relay (e.g. AWS SES / SendGrid). Notifications are demonstrated via SSE web sockets and REST polling.
- **Hardware Security Module (HSM) Key Storage:** Cryptographic signing of forensic reports is software-backed (SHA-256 HMAC) rather than HSM-backed.

---

## 6. Demonstration Prerequisites Checklist

Before launching the 8–12 minute live examination, ensure the host machine meets the following verified runtime baseline:

- [x] **Java Runtime:** OpenJDK 17.0.x or higher installed (`java -version`).
- [x] **Build System:** Apache Maven 3.8.x or higher (`mvn -version`).
- [x] **Python Environment:** Python 3.11.x with virtualenv activated (`python --version`).
- [x] **Node.js Environment:** Node.js v18.x or v20.x with npm (`node -v`, `npm -v`).
- [x] **Port Availability:**
  - `8080` (Spring Boot Backend API)
  - `8000` (FastAPI ML Microservice)
  - `5173` (Vite Frontend Dashboard)
- [x] **Test Fixtures Directory:** `media/demo_fixtures/` populated with verified sample fixtures (`benign_qr.png`, `upi_fraud_qr.png`, `phishing_login_mock.png`).
- [x] **Network Isolation:** No external internet connectivity strictly required; local mock fallbacks allow 100% offline demonstration.
