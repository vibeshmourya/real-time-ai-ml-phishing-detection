# PHASE 34 — AUTOMATED REGRESSION REPORT & DEFECT TRIAGE

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Audit Date:** October 2026  
**Evaluation Standard:** 100% Empirical Verification across all 4 Application Tiers  
**Total Automated Tests:** 186 Passing / 0 Failures / 0 Errors / 0 Skipped (100% Pass Rate)

---

## 1. Test Suite Summary Across Runtimes

| Subsystem / Test Suite | Harness & Framework | Source Directory | Tests Run | Passed | Failed | Skipped | Elapsed Time | Build Status |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Spring Boot Backend** | JUnit 5 + Spring Boot Test | `backend/src/test/java/` | **130** | **130** | 0 | 0 | 59.95s | **`BUILD SUCCESS`** |
| **FastAPI ML Microservice** | Pytest 9.1.1 (Python 3.11) | `ml-service/tests/` | **36** | **36** | 0 | 0 | 6.28s | **`PASSED`** |
| **React 18 Frontend SPA** | Node.js Test Runner / Vitest | `frontend/src/tests/` | **10** | **10** | 0 | 0 | 0.13s | **`PASSED`** |
| **Chrome Extension (MV3)** | Node.js Manifest Test Harness | `browser-extension/` | **10** | **10** | 0 | 0 | 0.05s | **`PASSED`** |
| **Total Automated Coverage** | — | — | **186** | **186** | **0** | **0** | **~66.4s** | **`100% PASS`** |

---

## 2. Command Executions & Verified Evidence

---

### Suite 1: Backend Automated Regression Suite (`mvn test`)
- **Command:** `mvn test`
- **Working Directory:** `d:\MCA notes\MINI PRO SIH\backend`
- **Exit Code:** `0`
- **Output Excerpt:**
  ```text
  [INFO] Running com.phishshield.Phase34IntegrationRegressionTest
  [INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.332 s -- in com.phishshield.Phase34IntegrationRegressionTest
  [INFO] Running com.phishshield.QRCodeServiceTest
  [INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.73 s -- in com.phishshield.QRCodeServiceTest
  [INFO] Running com.phishshield.RuleEngineServiceTest
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.phishshield.RuleEngineServiceTest
  [INFO] Running com.phishshield.ScreenshotAnalysisTest
  [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.247 s -- in com.phishshield.ScreenshotAnalysisTest
  [INFO] Running com.phishshield.SecurityRateLimiterTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.708 s -- in com.phishshield.SecurityRateLimiterTest
  [INFO] Running com.phishshield.SsrfProtectionServiceTest
  [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.phishshield.SsrfProtectionServiceTest
  [INFO] Running com.phishshield.UnifiedImageAnalysisTest
  [INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 26.08 s -- in com.phishshield.UnifiedImageAnalysisTest
  [INFO] Running com.phishshield.URLScanControllerTest
  [INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.306 s -- in com.phishshield.URLScanControllerTest
  [INFO] 
  [INFO] Results:
  [INFO] Tests run: 130, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS - Total time: 59.954 s
  ```

---

### Suite 2: Python FastAPI Microservice Suite (`pytest`)
- **Command:** `python -m pytest`
- **Working Directory:** `d:\MCA notes\MINI PRO SIH\ml-service`
- **Exit Code:** `0`
- **Output Excerpt:**
  ```text
  platform win32 -- Python 3.11.0, pytest-9.1.1, pluggy-1.6.0
  rootdir: D:\MCA notes\MINI PRO SIH\ml-service
  collected 36 items

  tests\test_api.py .........                                              [ 25%]
  tests\test_benchmark_reproducibility.py .                                [ 27%]
  tests\test_data_pipeline.py .....                                        [ 41%]
  tests\test_features.py .......                                           [ 61%]
  tests\test_performance.py ...                                            [ 69%]
  tests\test_prediction.py ....                                            [ 80%]
  tests\test_rules.py .......                                              [100%]

  ======================= 36 passed, 3 warnings in 6.28s ========================
  ```

---

### Suite 3: React Frontend Test Suite (`npm test`)
- **Command:** `npm test`
- **Working Directory:** `d:\MCA notes\MINI PRO SIH\frontend`
- **Exit Code:** `0`
- **Output Excerpt:**
  ```text
  ✔ 1. Valid image formats are accepted by frontend validator (1.8945ms)
  ✔ 2. Unsupported formats and executable payloads are rejected (0.3569ms)
  ✔ 3. Oversized files (>10MB) are rejected before uploading (1.1755ms)
  ✔ 4. Upload request form data construction contains file and optional text hint (0.2938ms)
  ✔ 5. Distinguishes network connection failure from server outage (0.3434ms)
  ✔ 6. Distinguishes request timeout after 30 seconds (0.2036ms)
  ✔ 7. Correctly classifies HTTP 400 validation rejection from backend (0.2079ms)
  ✔ 8. Correctly classifies HTTP 429 rate limit exceeded (0.1955ms)
  ✔ 9. Handles partial analysis responses with degraded components honestly (0.2839ms)
  ✔ 10. Dangerous script schemes in QR payloads are flagged as BLOCKED (0.3046ms)
  ℹ tests 10, pass 10, fail 0, skipped 0, duration_ms 130.3228
  ```

---

### Suite 4: Chromium Extension Test Suite (`node test_extension.js`)
- **Command:** `node test_extension.js`
- **Working Directory:** `d:\MCA notes\MINI PRO SIH\browser-extension`
- **Exit Code:** `0`
- **Output Excerpt:**
  ```text
  1. Manifest V3 & Permissions Integrity
    ✓ manifest.json must exist and be valid JSON
    ✓ Permissions must be strictly minimized (only activeTab, storage)
    ✓ All referenced assets and scripts must exist on disk
  2. Security & Secret Scanning (Zero Hardcoded Keys)
    ✓ No Gemini API keys, AWS secrets, or private passwords in extension files
  3. URL Validation & Scheme Filtering
    ✓ Must reject internal browser pages gracefully
    ✓ Must correctly validate and normalize standard web URLs
  4. Backend API Contract & Schema Compatibility
    ✓ Must parse real Spring Boot URLScanResponse with ML Prediction
    ✓ Must parse safe website response accurately without false positives
  5. Zero Fabrication & Offline Handling
    ✓ Ensures NO heuristic fallback scores are fabricated if backend is down
  6. Privacy-First Navigation Guard Settings
    ✓ Navigation protection must default to disabled (opt-in only)
  TEST SUMMARY: 10/10 Passed (100%)
  ```

---

### Suite 5: Frontend Production Bundle Verification (`npm run build`)
- **Command:** `npm run build`
- **Working Directory:** `d:\MCA notes\MINI PRO SIH\frontend`
- **Exit Code:** `0`
- **Output Excerpt:**
  ```text
  vite v5.4.21 building for production...
  ✓ 1604 modules transformed.
  dist/index.html                                             1.42 kB │ gzip:  0.71 kB
  dist/assets/index-CSpob165.css                             73.05 kB │ gzip: 11.37 kB
  dist/assets/vendor-icons-D0mMpFkw.js                       33.48 kB │ gzip:  7.80 kB
  dist/assets/ScannerPage-DbBQB2N7.js                        34.83 kB │ gzip:  7.68 kB
  dist/assets/vendor-react-Do4CP7ts.js                      133.93 kB │ gzip: 43.13 kB
  dist/assets/index-BO-GDg2D.js                             201.41 kB │ gzip: 60.86 kB
  ✓ built in 7.73s
  ```

---

## 3. Defect Triage & Verification Matrix

During Phase 34 regression authoring and test execution, integration assertions were calibrated and verified against real system contracts:

| Issue ID | Module / Component | Severity | Discovered Symptom / Contract Gap | Resolution / Status |
| :---: | :--- | :---: | :--- | :--- |
| **DEF-01** | `SSRFProtectionService` / `GlobalExceptionHandler` | **P1** | Regression test asserted `"SSRF Security violation blocked"` in JSON message; actual response wraps as `"Security Policy Violation: <reason>"`. | **Resolved.** Test aligned to exact exception handler response contract. `error = "SSRF_VIOLATION_BLOCKED"` verified. |
| **DEF-02** | `RequestCorrelationFilter` | **P2** | Integration test checked `X-Correlation-ID`; filter actually uses standard `X-Request-ID` header. | **Resolved.** Test updated to assert `X-Request-ID`. Both request stamping and response propagation verified. |
| **DEF-03** | `URLScanResponse` DTO | **P2** | JSON field for forensic breakdown is `explainability` (matches `ScanExplainabilityDetails`), not `explainabilityDetails`. | **Resolved.** Assertion corrected to `$.data.explainability`. |
| **DEF-04** | `HealthController` | **P2** | `/api/health` wraps status inside `ApiResponse.data.status` (`"UP"`). Test checked root `$.status`. | **Resolved.** Path corrected to `$.data.status`. |

No P0 (critical security/data corruption) defects exist in the current working tree. All regression tests pass with 100% green status.
