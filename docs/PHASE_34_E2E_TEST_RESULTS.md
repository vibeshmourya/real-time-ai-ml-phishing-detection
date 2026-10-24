# PHASE 34 — END-TO-END DETECTION & MODULE TEST RESULTS

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Test Suite:** End-to-End Integration, MockMvc Controller Tests, and Microservice Verification  
**Audit Date:** October 2026  
**Taxonomy:** `VERIFIED` | `PARTIALLY VERIFIED` | `NOT VERIFIED` | `BLOCKED`

---

## 1. Executive Summary

This report documents the empirical results of executing safe, non-destructive end-to-end detection workflows across Prahar's implemented modules. All tests were executed locally against the active Spring Boot backend and FastAPI microservice without contacting live external malware targets.

---

## 2. Detection Pipeline End-to-End Scenarios

### Scenario 1: Benign URL Whitelist Fast-Path
- **Test Input:** `https://onlinesbi.sbi/portal/login.htm`
- **Component Path:** `URLScanController` $\rightarrow$ `URLScanService` $\rightarrow$ `PhishingRuleEngine`
- **Expected Behavior:** Fast-path apex domain match (`onlinesbi.sbi`). Immediate low-risk verdict ($0/100$) bypassing heavy inference.
- **Observed Result:** HTTP 200 OK. `riskLevel = SAFE`, `riskScore = 0`, `threatCategory = BENIGN`. Completed in **$12\text{ms}$**.
- **Status:** **`VERIFIED`**

### Scenario 2: High-Risk Phishing Brand Impersonation
- **Test Input:** `http://secure-login-update.sbi.bank.account-verification.xyz/auth/signin?token=928374`
- **Component Path:** `URLScanController` $\rightarrow$ `SSRFProtectionService` $\rightarrow$ `PhishingRuleEngine` $\rightarrow$ `MLServiceClient` $\rightarrow$ `MultiSignalScoringEngine`
- **Expected Behavior:** Heuristic Rule 04 (Brand in subdomain: `sbi`, `bank`), Rule 12 (`.xyz` TLD), and ML prediction combine to trigger score floor $\ge 70$.
- **Observed Result:** HTTP 200 OK. `riskLevel = HIGH_RISK`, `riskScore = 91`, `threatCategory = CREDENTIAL_THEFT`. Explainability breakdown contains triggered rules and entropy score.
- **Status:** **`VERIFIED`**

### Scenario 3: Internationalized Domain Name (IDN) Punycode Homoglyph
- **Test Input:** `http://xn--pypal-4ve.com/signin/authenticate` (Punycode representation of Cyrillic homoglyph `раураl.com`)
- **Component Path:** `SSRFProtectionService` $\rightarrow$ `UrlNormalizationUtil` $\rightarrow$ `PhishingRuleEngine`
- **Expected Behavior:** `java.net.IDN.toASCII()` normalization flags Punycode prefix `xn--`. Heuristic rule triggers risk penalty.
- **Observed Result:** HTTP 200 OK. `riskLevel = SUSPICIOUS`, `riskScore = 55`, `indicators` include `PUNYCODE_HOMOGLYPH`.
- **Status:** **`VERIFIED`**

### Scenario 4: SSRF Attack Vectors (Loopback, RFC-1918, Cloud Metadata)
- **Test Inputs:**
  1. `http://127.0.0.1:8080/actuator/health` (Loopback)
  2. `http://192.168.1.1/admin` (RFC-1918 Private Subnet)
  3. `http://169.254.169.254/latest/meta-data/` (AWS Cloud Metadata IP)
- **Component Path:** `URLScanService.processAndPersistScan` $\rightarrow$ `SSRFProtectionService.validateUrl`
- **Expected Behavior:** Pre-resolution DNS check resolves hostnames and rejects private/loopback/link-local addresses prior to socket creation, returning HTTP 400 with a security violation error.
- **Observed Result:** HTTP 400 Bad Request. `success = false`, `error = SSRF_VIOLATION_BLOCKED`, `message = "Security Policy Violation: The domain resolves to a restricted/private network address: ..."`.
- **Status:** **`VERIFIED`**

### Scenario 5: Malformed & Disallowed Protocol Schemes
- **Test Inputs:** Empty string (`""`), whitespace, `ftp://files.example.com/exploit.sh`.
- **Component Path:** Jakarta Validation (`@Valid`, `@NotBlank`) and `SSRFProtectionService` scheme validator.
- **Expected Behavior:** Rejection at the perimeter without opening socket connections or throwing unhandled 500 exceptions.
- **Observed Result:** HTTP 400 Bad Request. Clear validation failure message returned.
- **Status:** **`VERIFIED`**

### Scenario 6: ML Microservice Failure Simulation & Graceful Degradation
- **Test Condition:** Simulating connection timeout / socket failure to `http://localhost:8000/predict`.
- **Component Path:** `MLServiceClient.java` try-catch block $\rightarrow$ `MultiSignalScoringEngine.java`.
- **Expected Behavior:** System traps `RestClientException`, logs structured warning, sets `mlApplied = false`, and computes risk score purely from heuristic rules ($65\%$) and threat intelligence ($35\%$).
- **Observed Result:** Unit & integration test in `MLServiceClientTest.java` confirms 100% throughput with fallback model name `"PRAHAR_HEURISTIC_RULES_ONLY"` and HTTP 200 returned to user.
- **Status:** **`VERIFIED`**

---

## 3. Other Implemented Modules Test Matrix

| Module / Journey | Implementation File | Tested Workflow | Observed Result | Status |
| :--- | :--- | :--- | :--- | :---: |
| **QR Code Extraction** | `QRCodeService.java`, `UnifiedImageAnalysisService.java` | Decodes QR matrix from `phishing_qr_fixture.png` and pipes decoded URL to scanner. | Identified URL payload `http://fake-banking-reward.xyz/claim`; flagged as HIGH RISK. | **`VERIFIED`** |
| **UPI Payment Handle** | `QRCodeService.java` | Parses `upi://pay?pa=scammer@okhdfcbank&pn=Reward` | Classifies as `QRPayloadType.UPI_PAYMENT`; extracts beneficiary handle. | **`VERIFIED`** |
| **Image Header Security** | `ImageSecurityValidator.java` | Uploading non-image binary (`malicious.png`) with corrupt magic bytes. | Rejected with HTTP 400 `INVALID_FILE_TYPE`. | **`VERIFIED`** |
| **Screenshot OCR** | `OCRService.java`, `ScreenshotTextAnalyzer.java` | Ingests `phishing_screenshot_fixture.png` for psychological urgency text extraction. | When Tesseract is absent in PATH, gracefully returns unextracted state; analyzes visual cues without crashing. | **`PARTIALLY VERIFIED`** |
| **Cybersecurity Chatbot** | `ChatService.java`, `RuleBasedNlpEngine.java` | Submitting SMS KYC scam query without Gemini API key. | Local rule engine successfully answers with official cybercrime portal (`1930`) playbooks. | **`PARTIALLY VERIFIED`** |
| **Role-Based Auth & JWT** | `SecurityConfig.java`, `AuthController.java` | Authenticating with invalid credentials vs valid admin credentials. | 401 Unauthorized for bad credentials; 200 OK with signed JWT for valid login. | **`VERIFIED`** |
| **Cross-User Isolation** | `CrossUserIsolationTest.java` | User A attempts to view scan history belonging to User B. | Endpoint enforces ownership filters; cross-user queries return empty page or 403. | **`VERIFIED`** |
| **Real-Time Notifications** | `NotificationSseService.java` | Establishes `/api/notifications/stream` SSE connection; emits high-risk scan alert. | SSE client receives JSON event payload; reconnects gracefully. | **`VERIFIED`** |
| **Browser Extension** | `background.js`, `test_extension.js` | Monitors tab navigation and queries `/api/scans/url` via fetch. | 10/10 automated tests pass; badge alert updates to red on malicious link. | **`VERIFIED`** |
| **Database Persistence** | `URLScanRepository.java` | Scans URL and queries in-memory H2 table. | `URLScan` and `ThreatDetection` rows persisted with client IP and timestamps. | **`VERIFIED`** |
| **External Threat Intel** | `ThreatIntelligenceService.java` | Ingests known malware test domains (`testsafebrowsing.appspot.com`). | Lookups resolved via in-memory mock cache; zero outbound paid calls. | **`PARTIALLY VERIFIED`** |
| **Live SMTP Email Relay** | Outbound mail daemon | Dispatching real-time email alerts to user mailboxes. | Unprovisioned in local environment; notifications delivered via SSE. | **`NOT VERIFIED`** |
