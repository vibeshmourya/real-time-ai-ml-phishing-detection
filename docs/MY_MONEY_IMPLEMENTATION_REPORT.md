# PRAHAR — MY MONEY & PERSONAL FINANCIAL SECURITY UPGRADE
## Comprehensive Implementation & Root-Cause Audit Report

**System**: Prahar — Real-Time AI/ML-Based Phishing Detection & Prevention System  
**Module**: My Money & Personal Financial Security Upgrade  
**Execution Date**: October 2026  
**Status**: Completed & Verified (All 235 Backend Tests & 29 Frontend Tests Passing)

---

## 1. Executive Summary & Root-Cause Audit

### 1.1 The Failure Phenomenon
Users attempting to upload bank statements in the **My Money** module received an immediate HTTP 401 Unauthorized error with the message:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/transactions/upload-csv"
}
```
This error occurred even when the user appeared logged into the Prahar interface.

### 1.2 Root Cause Analysis

A thorough code audit across the backend security filter chain, transaction controller, frontend API client, and authentication context uncovered **three interacting root causes**:

1. **Endpoint Route Mismatch**:
   - In `frontend/src/api/transactionApi.js`, the upload function targeted `POST /transactions/upload-csv` (which resolved via the Axios base configuration to `/api/transactions/upload-csv`).
   - In `backend/src/main/java/com/phishshield/controller/TransactionController.java`, the legacy CSV upload endpoint was annotated with `@PostMapping("/import/csv")` (resolving to `/api/transactions/import/csv`).
   - Because Spring Security had configured `.anyRequest().authenticated()`, any request to an unmapped path (like `/api/transactions/upload-csv`) was not caught by standard controller dispatch and was evaluated by Spring Security. When authorization failed or dispatched to the error dispatcher `/error`, `JwtAuthenticationEntryPoint` intercepted the dispatch and emitted `"Full authentication is required to access this resource"`.

2. **Manual Header Clobbering in Axios (Multipart Boundary Stripping)**:
   - In `frontend/src/api/transactionApi.js`, the upload function explicitly passed:
     ```javascript
     headers: { 'Content-Type': 'multipart/form-data' }
     ```
   - In Axios, manually specifying `'Content-Type': 'multipart/form-data'` strips the auto-generated boundary delimiter (e.g., `; boundary=----WebKitFormBoundary...`). Without this boundary, the Tomcat/Spring standard `StandardServletMultipartResolver` fails to parse the incoming request body parts, resulting in corrupted or rejected requests.

3. **Demo User Email Desynchronization & Fallback Token Invalidation**:
   - In `frontend/src/context/AuthContext.jsx`, `DEMO_PRESETS` defined demo user credentials as `demo@prahar.internal`.
   - In `backend/src/main/java/com/phishshield/config/DataInitializer.java`, default development users were seeded with `@phishshield.security` domains (e.g., `user@phishshield.security`).
   - When users clicked demo logins, the authentication request failed with `BadCredentialsException`. The frontend caught this error and fell back to storing a mock token string (`demo-token-role_user`).
   - When subsequent requests dispatched this mock string in the `Authorization: Bearer demo-token-role_user` header, `JwtAuthenticationFilter` attempted to parse the token using JJWT (`Jwts.parserBuilder().setSigningKey(key)...`). JJWT threw `MalformedJwtException`, the security context remained empty (`SecurityContextHolder.getContext().getAuthentication() == null`), and `JwtAuthenticationEntryPoint` rejected the request with HTTP 401.

### 1.3 Remediation Strategy (No Security Bypass)
Rather than weakening authentication (e.g., adding `permitAll()` to transaction endpoints, which would breach multi-tenant user isolation), we implemented strict architectural corrections:
- Added `@PostMapping("/upload-csv")` and `@PostMapping("/import/csv")` as backward-compatible aliases pointing to the unified import pipeline in `TransactionController`.
- Removed manual `Content-Type` headers from `transactionApi.js`, allowing Axios and the browser to automatically set `multipart/form-data; boundary=...`.
- Updated `CustomUserDetailsService` with bidirectional domain aliasing between `@prahar.internal` and `@phishshield.security`, ensuring both seeded passwords and demo presets authenticate cleanly with valid signed JWTs.
- Created complete mock integration and unit tests validating that unauthenticated requests are strictly rejected with HTTP 401 while authenticated requests succeed with complete tenant isolation.

---

## 2. Architectural Architecture & Files Modified

### 2.1 Component Overview

```
                                  +------------------------------------+
                                  |    Prahar Web App / My Money UI    |
                                  +-----------------+------------------+
                                                    |
                                    HTTPS REST API (JWT Authenticated)
                                                    |
                                  +-----------------v------------------+
                                  |       TransactionController        |
                                  +--------+------------------+--------+
                                           |                  |
               +---------------------------+                  +---------------------------+
               |                                                                          |
+--------------v---------------+                                            +-------------v---------------+
|    UnifiedStatementParser    |                                            |   SmsFraudDetectionService  |
|  (Pure Java SE 17: CSV, XLSX,|                                            | (OTP/PIN redaction, phishing|
|   XLS, PDF Stream Extractor) |                                            |  heuristics, Android terms) |
+--------------+---------------+                                            +-----------------------------+
               |
               v
+------------------------------+
|     TransactionService       |
+--------------+---------------+
               |
               v
+------------------------------------+
|  PersonalizedRiskAssessmentService |
|  - Median & MAD Baseline           |
|  - Modified Z-Score (>3.5, >6.0)   |
|  - Recurring Expense Suppression   |
|  - Unfamiliar Recipient Scoring    |
|  - Cold-Start Fallback (<5 txns)   |
+------------------------------------+
```

### 2.2 Inventory of Modified and Newly Created Files

| File Path | Role | Description |
| :--- | :--- | :--- |
| `backend/.../dto/response/StatementPreviewRowDto.java` | DTO (New) | Candidate parsed row with extracted date, amount, type, counterparty, duplicate flag, and risk assessment. |
| `backend/.../dto/response/StatementPreviewResponseDto.java` | DTO (New) | Comprehensive preview payload containing detected file type, column mappings, duplicate count, and ambiguities. |
| `backend/.../dto/response/PersonalizedRiskAssessment.java` | DTO (New) | Structured risk score (0-100), severity (`LOW_RISK`, `NEEDS_REVIEW`, `FLAGGED_SUSPICIOUS`), reason codes, explanation, and recommendations. |
| `backend/.../dto/request/ImportConfirmRequest.java` | DTO (New) | Verified rows payload confirmed by the user after reviewing preview ambiguities. |
| `backend/.../dto/response/SmsAnalysisResponse.java` | DTO (New) | Result of SMS fraud analysis with redacted text, fraud indicators, extracted transaction, and platform guidance. |
| `backend/.../dto/response/TransactionSummaryDto.java` | DTO (Updated) | Enriched with baseline metrics: `personalizationEstablished`, `baselineMedian`, `baselineMad`, `recurringExpensesCount`. |
| `backend/.../dto/response/TransactionDto.java` | DTO (Updated) | Added `riskScore`, `severity`, and `recommendation` fields. |
| `backend/.../service/UnifiedStatementParser.java` | Service (New) | Pure Java SE parser for CSV, XLSX (OpenXML zip/xml), XLS, and PDF (stream decompression + tokenizer). |
| `backend/.../service/PersonalizedRiskAssessmentService.java` | Service (New) | Statistical behavioral profiler implementing Median, MAD, recurring pattern detection, and recipient scoring. |
| `backend/.../service/SmsFraudDetectionService.java` | Service (New) | Privacy-preserving SMS transaction extractor and phishing/scam analyzer with OTP masking. |
| `backend/.../service/TransactionService.java` | Service (Updated) | Integrated preview/confirm import workflows, direct unified upload, and personalized risk profiling. |
| `backend/.../security/CustomUserDetailsService.java` | Security (Updated)| Added domain alias resolution between `@prahar.internal` and `@phishshield.security`. |
| `backend/.../controller/TransactionController.java` | Controller (Updated)| Exposed `/import/preview`, `/import/confirm`, `/import/unified`, `/analyze-sms`, and backward-compatible aliases. |
| `frontend/src/api/transactionApi.js` | API Client (Updated)| Added preview, confirm, unified upload, and SMS analysis methods; removed manual multipart Content-Type header. |
| `frontend/src/pages/MyMoneyPage.jsx` | UI Page (Updated) | Implemented two-step statement import modal, baseline health card, SMS fraud analyzer, risk audit modal, and toast alerts. |
| `backend/.../TransactionControllerIntegrationTest.java`| Test (New) | End-to-end integration test validating controller routes, auth enforcement, deduplication, and user isolation. |
| `backend/.../UnifiedStatementParserTest.java` | Test (New) | Unit tests verifying parsing of CSV, XLSX, XLS, PDF streams, password protection detection, and limits. |
| `backend/.../PersonalizedRiskAssessmentTest.java` | Test (New) | Unit tests validating Median/MAD scoring, recurring rent suppression, recipient scoring, and cold-start logic. |
| `backend/.../SmsFraudDetectionTest.java` | Test (New) | Unit tests validating privacy redaction, legitimate bank alerts, urgent phishing lures, and transaction extraction. |
| `frontend/src/tests/unifiedImportAndRisk.test.js` | Test (New) | Frontend test suite verifying client-side privacy masking, header auto-classification, and preview/confirm payloads. |

---

## 3. Statement Import & Parsing Engine

### 3.1 Supported File Formats & Parsing Implementation
In an environment where external Maven downloads are restricted or offline, the parser was engineered entirely using standard **Java SE 17** APIs without external dependencies:

1. **CSV & Delimited Text (`.csv`, `.txt`)**:
   - Implemented RFC 4180-compliant state-machine tokenizer handling quoted values, embedded commas, newlines, and escaped quotes.
   - Header normalization intelligently identifies dates, withdrawal/deposit amounts, balance, narration, and type (`DR`/`CR`).

2. **Microsoft Excel OpenXML (`.xlsx`)**:
   - Implemented an in-memory OpenXML unpacker utilizing `java.util.zip.ZipInputStream` and standard XML DOM (`DocumentBuilderFactory`).
   - Resolves workbook structure, extracts the shared string table (`xl/sharedStrings.xml`), and reads sheet cell nodes (`xl/worksheets/sheet1.xml`), mapping column indexes (A, B, C...) to row values.

3. **Legacy Excel (`.xls`)**:
   - Gracefully handles tab-delimited and HTML-table exports commonly emitted by Indian banks (SBI, HDFC, ICICI, Axis) with `.xls` extensions.

4. **Portable Document Format (`.pdf`)**:
   - Inspects PDF binary headers (`%PDF-`).
   - Checks for password-protected/encrypted structures (`/Encrypt`) and returns actionable error notices (`PASSWORD_PROTECTED_PDF`) instead of failing silently.
   - Locates PDF stream objects, decompresses them using standard `java.util.zip.Inflater` (FlateDecode), extracts text tokens (`BT ... ET`, `Tj`, `TJ`), cleans glyph encoding, and identifies tabular bank statement rows.
   - Detects image-only/scanned PDFs lacking text streams and prompts the user accordingly.

### 3.2 Security Controls & Defenses
- **File Size Ceiling**: Strictly capped at 10 MB per statement file.
- **Zip-Bomb & Decompression Defense**:
  - Enforces a maximum uncompressed size ratio (max 50 MB total extracted text) to prevent decompression bombs.
  - Limits max rows processed per statement to 5,000 candidate records.
- **Malicious Payload Neutralization**:
  - Formulas starting with `=`, `+`, `-`, or `@` in cell text are stripped of executable prefixes to prevent CSV/Excel formula injection (CWE-1236).

### 3.3 Two-Step Import Workflow (Preview & Confirm)
1. **Preview Step (`POST /api/transactions/import/preview`)**:
   - Parses the document into structured candidate rows without committing them to the database.
   - Cross-references existing transactions for the authenticated user to detect duplicate entries (matching date, amount, and description).
   - Flags ambiguities (e.g., duplicate rows, missing dates, unknown column mappings).
2. **Confirm Step (`POST /api/transactions/import/confirm`)**:
   - Receives user-reviewed and adjusted rows (allowing users to exclude duplicates or adjust categories).
   - Calculates baseline risk scores and persists transactions in a single atomic transaction.

---

## 4. Personalized Behavioral Anomaly Detection

### 4.1 Statistical Foundation: Median & MAD
Traditional fraud detectors that rely on Mean and Standard Deviation suffer from vulnerability to severe outliers: if a user makes a single large transaction, the sample mean and variance skew drastically, blinding the system to future fraud.

Prahar implements **Median Absolute Deviation (MAD)**:

$$\text{Median} = \tilde{x} = \text{median}(X)$$
$$\text{MAD} = \text{median}(|x_i - \tilde{x}|)$$
$$\text{Modified Z-Score} = 0.6745 \times \frac{|x_i - \tilde{x}|}{\text{MAD}}$$

- **Consistency Factor ($0.6745$)**: Scales the MAD to estimate the standard deviation for normally distributed data while maintaining resistance to extreme outliers.
- **Score Classification**:
  - Modified Z-Score $< 3.5$: **Normal Spending** (Low Risk)
  - $3.5 \le$ Modified Z-Score $< 6.0$: **Moderate Deviation** (Needs Review)
  - Modified Z-Score $\ge 6.0$: **Extreme Deviation** (Flagged Suspicious)

### 4.2 Recurring Expense Learning (Suppressing False Positives)
A common failure of generic anomaly models is flagging legitimate recurring expenses (such as monthly house rent of ₹15,000 or school fees) as suspicious simply because they are significantly larger than daily food or grocery spending.

- **Recurring Pattern Detection**:
  - Evaluates periodic debits within a $\pm 5\%$ amount tolerance appearing on similar monthly schedules (interval of 25–35 days).
  - Recognizes recurring keywords (`rent`, `maintenance`, `emi`, `sip`, `tuition`, `insurance`, `premium`, `subscription`).
  - Automatically suppresses anomaly penalties for confirmed recurring expenses, assigning `RECURRING_EXPENSE_MATCH` with a low risk score.

### 4.3 Cold-Start Handling
- When a user has fewer than 5 historical transactions, statistical baselines are not yet established.
- The system returns `personalizationEstablished = false` and applies conservative, transparent thresholds without falsely claiming personalized intelligence.
- Explanations explicitly clarify: *"Cold start mode: spending profile under construction (need at least 5 transactions to calculate personalized baseline)."*

### 4.4 Counterparty & Keyword Intelligence
- Evaluates unfamiliar recipients: if a counterparty has never been encountered in the user's ledger and the transaction amount exceeds ₹1,000, risk is incremented with `UNFAMILIAR_RECIPIENT`.
- Analyzes suspicious merchant narratives: detects high-risk keywords (`crypto`, `binance`, `wazirx`, `forex`, `betting`, `rummy`, `lottery`, `urgent transfer`, `gift card`, `telegram`) and applies weighted adjustments.

---

## 5. Mobile SMS & Personal Alert Fraud Detection

### 5.1 Privacy-First Credential Redaction
Financial SMS messages frequently contain sensitive authentication credentials. Prahar enforces zero-trust privacy controls:
- **Redaction**:
  - OTPs and security PINs matching patterns like `(?:otp|code|pin)\s*(?:is|:|#|-|=|to)?\s*([0-9]{4,8})` are replaced with `[PROTECTED_OTP]`.
  - Passwords and temporary credentials matching `(?:password|pwd)\s*(?:is|:)?\s*(\S+)` are replaced with `[PROTECTED_CREDENTIAL]`.
- Redaction occurs **before** any heuristics, ML scoring, or audit logging, ensuring no plain-text credentials are ever logged or persisted.

### 5.2 Distinguishing Legitimate Alerts from Phishing Lures
The analyzer evaluates incoming SMS texts across two separate threat dimensions:

1. **Phishing & Impersonation Threats**:
   - Urgent scare tactics: *"Account blocked within 24 hours"*, *"PAN verification failed"*, *"Electricity disconnection notice"*, *"Lottery winner"*.
   - Malicious links: Shortened URLs (`bit.ly`, `tinyurl.com`), suspicious domains (`.top`, `.xyz`, `.club`), or links disguised as legitimate bank portals.
   - APK / Malware drop lures: Prompts to download `.apk` or remote support tools (`AnyDesk`, `TeamViewer`, `RustDesk`).
2. **Legitimate Transaction Alerts**:
   - Official bank formats (HDFC, SBI, ICICI, Axis, PNB): *"A/C XX1234 debited by INR 450.00 on 25-Oct-26 via UPI..."*.
   - Automatically extracts transaction amount, type (DEBIT/CREDIT), counterparty, and reference number.
   - Correlates the extracted transaction with the user's personalized spending baseline to check for deviation anomalies.

### 5.3 Android Platform Integration & Security Disclosures
To prevent misleading claims of background spying or automatic interception:
- Prahar explicitly discloses Android platform constraints:
  - Background SMS reading requires explicit runtime permission (`RECEIVE_SMS` / `READ_SMS`) or Google Play-compliant **SMS Retriever API** / **User Consent API** for one-time verification.
  - Notification parsing requires user activation of **NotificationListenerService** with an explicit user permission prompt.
- Prahar provides a manual verification interface allowing users to paste any suspicious SMS or notification for immediate on-demand threat analysis.

---

## 6. Comprehensive Automated Test Matrix & Results

### 6.1 Backend Test Results (`mvn test`)
The backend regression suite was executed across all unit and integration test classes.

**Command Executed**:
```bash
mvn test
```

**Actual Execution Summary**:
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.phishshield.CrossUserIsolationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.PersonalizedRiskAssessmentTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.SecurityRateLimiterTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.SmsFraudDetectionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.SsrfProtectionServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.ThreatIntelServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.TransactionControllerIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.UnifiedImageAnalysisTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.UnifiedStatementParserTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.phishshield.URLScanControllerTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] ... (remaining controller and service suites)
[INFO] 
[INFO] Results:
[INFO] Tests run: 235, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 27.049 s
[INFO] ------------------------------------------------------------------------
```

### 6.2 Key Test Scenarios Verified

| Test Class | Scenarios Covered | Status |
| :--- | :--- | :--- |
| `TransactionControllerIntegrationTest` | • Rejection of unauthenticated requests with HTTP 401<br>• Summary calculation with established baseline<br>• Statement preview and confirm import with deduplication<br>• Backward-compatible upload aliases (`/upload-csv`, `/import/csv`)<br>• Cross-user transaction data isolation<br>• Direct unified import | **PASSED** (6/6) |
| `PersonalizedRiskAssessmentTest` | • Cold start handling (<5 transactions)<br>• Normal spending within median baseline<br>• Outlier detection via Modified Z-score (MAD)<br>• Recurring expense recognition (monthly rent ₹15,000)<br>• First-time unfamiliar recipient penalty<br>• Suspicious keyword scoring (crypto, betting) | **PASSED** (6/6) |
| `UnifiedStatementParserTest` | • Standard CSV bank statement parsing<br>• Quoted values and escaped commas in CSV<br>• OpenXML Excel (.xlsx) parsing via Zip/DOM<br>• Tab-delimited (.xls) statement parsing<br>• PDF FlateDecode stream extraction and tabular tokenizer<br>• Password-protected PDF detection (`/Encrypt`)<br>• File size ceiling enforcement (>10MB rejection) | **PASSED** (7/7) |
| `SmsFraudDetectionTest` | • Sensitive credential and OTP redaction<br>• Legitimate bank notification extraction<br>• High-urgency KYC suspension phishing detection<br>• Anomaly correlation with user's baseline<br>• Android platform permission disclosures<br>• Empty and malformed message handling | **PASSED** (6/6) |
| `CrossUserIsolationTest` | • Tenant isolation between user A and user B<br>• Prevention of cross-tenant data leaks | **PASSED** (2/2) |

### 6.3 Frontend Test Results (`npm test`)
**Command Executed**:
```bash
npm test
```

**Actual Execution Summary**:
```
✔ 1. API Base URL defaults to /api when env variable is missing or empty
✔ 2. API Base URL appends /api if absolute domain is provided without /api path
✔ 3. API Base URL strips trailing slashes and handles already-provided /api correctly
✔ 4. Chatbot request payload structure conforms to Spring Boot ChatRequest DTO
✔ 5. Auth token fallback supports prahar_token with backward compatibility for phishshield_token
✔ 1. Valid image formats are accepted by frontend validator
✔ 2. Unsupported formats and executable payloads are rejected
✔ 3. Oversized files (>10MB) are rejected before uploading
✔ 4. Upload request form data construction contains file and optional text hint
✔ 5. Distinguishes network connection failure from server outage
✔ 6. Distinguishes request timeout after 30 seconds
✔ 7. Correctly classifies HTTP 400 validation rejection from backend
✔ 8. Correctly classifies HTTP 429 rate limit exceeded
✔ 9. Handles partial analysis responses with degraded components honestly
✔ 10. Dangerous script schemes in QR payloads are flagged as BLOCKED
✔ 1. Default language resolves to English when storage is empty or invalid
✔ 2. Supported languages validation: en, hi, and kn are accepted
✔ 3. Switching to Hindi updates navbar, hero, scanner, and dashboard content
✔ 4. Switching to Kannada updates navbar, hero, scanner, and dashboard content
✔ 5. Switching back to English restores original strings accurately
✔ 6. Translation fallback gracefully uses English when key is missing in target locale
✔ 7. Parameter interpolation replaces dynamic tokens in all languages
✔ 8. Full parity between English, Hindi, and Kannada translation dictionaries
✔ 9. Rapid language switching produces deterministic and non-stale results
✔ 10. Never renders raw undefined or [object Object]
✔ 1. SMS sensitive credential client-side masking preserves safety before dispatch
✔ 2. Column header auto-classification recognizes standard bank statement schemas
✔ 3. Statement preview response validation flags ambiguities correctly
✔ 4. Confirm import payload packages verified user overrides and exclusions

ℹ tests 29
ℹ suites 0
ℹ pass 29
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 162.5676
```

---

## 7. Operational & Security Checklist

- [x] **Zero Global Auth Disabling**: All `/api/transactions/**` endpoints strictly enforce Spring Security JWT authentication. Unauthenticated requests are rejected with HTTP 401.
- [x] **Strict Multi-Tenant Isolation**: Every database query for transactions filters by the authenticated `User` entity derived from the JWT claims. No user can view, update, or preview another user's financial records.
- [x] **No External Dependency Bloat**: Statement parsing for CSV, XLSX, XLS, and PDF functions entirely using standard Java SE 17 libraries (`java.util.zip`, `javax.xml.parsers`, `java.io`).
- [x] **Privacy Compliance**: All SMS OTPs and authentication credentials are automatically masked before heuristic processing.
- [x] **Robust Rate Limiting**: The sliding-window rate limiter protects `/api/auth/login` (15 req/min) and prevents brute-force credential stuffing.
- [x] **Cross-Platform Android Disclosures**: The mobile SMS component honestly documents required platform permissions (SMS Retriever API, User Consent API, NotificationListenerService) without claiming unauthorized background interception.

---
*Report certified by Prahar Engineering & Security Team.*
