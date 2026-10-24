# Prahar 2.0 — Comprehensive Test Evidence & Results

**Verification Date:** October 24, 2026  
**Git Baseline Commit:** `bd56a5c80c6b56047261b5df300e25a2f2c695ad`  
**Overall Automated Test Suite Status:** **161 / 161 Passing (100% Pass Rate)**

---

## 1. Automated Test Summary Matrix

| Subsystem | Framework / Runner | Total Tests | Passed | Failed | Errors | Skipped | Execution Time |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Spring Boot API** | Maven Surefire, JUnit 5, SpringBootTest | 109 | **109** | 0 | 0 | 0 | 30.36 s |
| **FastAPI ML Service** | Pytest 8.4.2, AnyIO, Starlette TestClient | 32 | **32** | 0 | 0 | 0 | 6.10 s |
| **React Frontend** | Node Native Test Runner (`node --test`) | 10 | **10** | 0 | 0 | 0 | 116 ms |
| **Browser Extension** | Chrome API Mock Test Runner | 10 | **10** | 0 | 0 | 0 | 50 ms |
| **TOTALS** | — | **161** | **161** | **0** | **0** | **0** | **~36.6 s** |

---

## 2. Spring Boot Backend Test Suites (109 Tests)

Command: `mvn test` in `backend/`

| Test Class | Tests | Execution Time | Focus & Verification Scope |
| :--- | :--- | :--- | :--- |
| `ChatHistoryTest` | 3 | 0.052 s | Conversational session creation, message persistence, and chronological ordering. |
| `ChatServiceTest` | 4 | 0.009 s | Fallback from external AI to internal rule engine, slot memory tracking. |
| `CrossUserIsolationTest` | 3 | 0.312 s | Enforces tenant boundary: user $A$ cannot query or mutate user $B$'s notifications or incidents. |
| `GeminiIntegrationTest` | 2 | 0.006 s | Server-side Gemini request formatting, header injection, and timeout handling (unit-mocked). |
| `HealthControllerTest` | 3 | 0.021 s | Root directory endpoint, liveness `/api/health`, and readiness `/api/health/readiness`. |
| `ImageSecurityValidatorTest` | 8 | 0.045 s | Magic byte signature verification: accepts PNG, JPEG, WebP, GIF, BMP; rejects `.exe` and scripts. |
| `IncidentResponseTest` | 7 | 0.015 s | RBI Zero-Liability dispute letter drafting, IT Act citations, and `SensitiveDataScrubber`. |
| `MLServiceClientTest` | 5 | 1.337 s | Outbound HTTP client communication with FastAPI, error handling on 500/503. |
| `NlpEngineTest` | 6 | 0.007 s | 22-intent classification, slot extraction (DISCOM, bank name, amount). |
| `NotificationIntegrationTest`| 4 | 0.204 s | Real-time notification persistence, SHA-256 idempotency deduplication, unread count. |
| `QRCodeServiceTest` | 9 | 1.801 s | ZXing engine: decodes URL, UPI string, WiFi, PlainText from synthetic images. |
| `RuleEngineServiceTest` | 4 | 0.008 s | 30-rule lexical scoring: IP-in-hostname, punycode, hyphen count, brand impersonation. |
| `ScreenshotAnalysisTest` | 7 | 0.393 s | Visual urgency pattern detection; honest `UNEXTRACTED` state when Tesseract is absent. |
| `SecurityRateLimiterTest` | 1 | 1.747 s | IP bucket rate limiting: verifies HTTP 429 Too Many Requests after 15 auth failures. |
| `SsrfProtectionServiceTest` | 7 | 0.082 s | Blocks `127.0.0.1`, `169.254.169.254`, and private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`). |
| `ThreatIntelServiceTest` | 4 | 0.027 s | Integration with SafeBrowsing and VirusTotal APIs with mandatory user consent checks. |
| `UnifiedImageAnalysisTest` | 10 | 5.799 s | Full pipeline: file validation $\to$ QR decoding $\to$ OCR text extraction $\to$ candidate URL scan. |
| `URLScanControllerTest` | 11 | 0.322 s | Public scanning endpoints, input validation, corrupted upload rejection. |
| `URLScanServiceTest` | 11 | 0.018 s | End-to-end scanning lifecycle, scan persistence, risk tier classification. |
| **Total Backend Tests** | **109** | **30.365 s** | **0 Failures, 0 Errors, 0 Skipped** |

---

## 3. Python FastAPI ML Microservice Test Suites (32 Tests)

Command: `python -m pytest` in `ml-service/`

| Test Module | Tests | Focus & Verification Scope |
| :--- | :--- | :--- |
| `tests/test_api.py` | 9 | FastAPI `/health`, `/predict`, `/models/evaluate`, HTTP 422 on invalid URLs. |
| `tests/test_features.py` | 7 | Feature extraction: Shannon entropy, digit ratio, suspicious keywords, shortener checks. |
| `tests/test_prediction.py` | 4 | Scikit-learn and XGBoost model loading, probabilistic inference, feature vector alignment. |
| `tests/test_rules.py` | 7 | Heuristic rule checks, suspicious TLD detection, port validation, IP detection. |
| `tests/test_data_pipeline.py` | 5 | URL normalization, feed deduplication, False Positive Rate (FPR) metrics, governance gates. |
| **Total ML Tests** | **32** | **6.10 s (All 32 Passed)** |

---

## 4. Frontend & Browser Extension Test Suites (20 Tests)

### Frontend Suite (`frontend/src/tests/imageAnalysis.test.js`) — 10 Tests Passed
- Valid image formats accepted by frontend validator.
- Unsupported formats and executable payloads rejected pre-upload.
- Oversized files (>10MB) rejected before uploading.
- Upload request form data construction contains file and optional text hint.
- Distinguishes network connection failure from server outage.
- Handles request timeout after 30 seconds gracefully.
- Correctly classifies HTTP 400 validation rejection from backend.
- Correctly classifies HTTP 429 rate limit exceeded.
- Handles partial analysis responses with degraded components honestly.
- Dangerous script schemes in QR payloads are flagged as BLOCKED.

### Browser Extension Suite (`browser-extension/test_extension.js`) — 10 Tests Passed
- Manifest V3 & permissions integrity (minimal permissions: `activeTab`, `storage`).
- Zero secret scanning: confirms zero hardcoded API keys or credentials in extension files.
- URL validation and scheme filtering: rejects internal browser pages (`chrome://`).
- Backend API contract and schema compatibility: parses `URLScanResponse` structure accurately.
- Offline handling: verifies no fabricated scores when backend is unreachable.
- Privacy-first navigation guard defaults to disabled (opt-in only).

---

## 5. Production Build Verification

Command: `npm run build` in `frontend/`
- **Result:** Successfully compiled into `frontend/dist/`.
- **Modules Transformed:** 1,603 modules.
- **Duration:** 7.36 seconds.
- **Asset Integrity:** CSS minified, JS chunked, zero build or bundle warnings.
