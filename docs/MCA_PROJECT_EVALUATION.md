# PRAHAR 2.0 — MCA PROJECT EVALUATION & TECHNICAL DOSSIER
**Academic Year:** 2025–2026  
**Program:** Master of Computer Applications (MCA)  
**Project Title:** PRAHAR: Real-Time Multi-Channel Phishing Threat Intelligence, Incident Safety & AI Fraud Defense System  
**Platform Version:** 2.0.0 (Release Candidate)  

---

## 1. Executive Summary & Problem Statement

### 1.1 The Problem
In modern cybersecurity, social engineering—specifically phishing, smishing, quishing (QR code fraud), and fake banking KYC threats—causes staggering financial loss globally and within India. Fraudsters exploit the psychological vulnerability of urgency, authority impersonation, and zero-day deceptive domains that bypass traditional signature-based firewalls.

Key existing gaps in consumer cybersecurity:
1. **Siloed Inspection:** Most tools inspect only plain URLs, failing when threats are embedded in QR codes, screenshot text, or SMS.
2. **Post-Attack Paralysis:** Once a user falls victim to unauthorized debits or SIM swaps, they lack immediate structured incident containment during the critical **Golden Hour (2–3 hours)** required to freeze transit funds under Indian cyber protocols (Helpline 1930 / cybercrime.gov.in).
3. **Black-Box AI / Hallucination Risks:** Cloud generative LLMs can hallucinate advice, leak financial secrets, or fabricate threat verdicts without verifiable heuristics.

### 1.2 The Prahar Solution
**Prahar 2.0** is an enterprise-grade, multi-tiered defensive platform engineered to:
- Provide **multi-channel threat inspection** (URLs, SMS, Email, QR codes, and Screenshot OCR).
- Deliver **verifiable explainability** combining 30+ structural/lexical indicators with a scikit-learn machine learning classifier.
- Provide a dedicated **Fraud & Safety Centre** with dispute letter automation (quoting RBI Circular `RBI/2017-18/15` and IT Act 2000 `Sections 43A, 66C, 66D`).
- Implement **My Money & Transactions** for ledger tracking and explainable statistical anomaly detection without claiming live unauthorized bank access.
- Deploy a privacy-first, server-side **Ask Prahar** AI assistant (with offline deterministic NLP taxonomy fallback) and **Server-Sent Events (SSE)** for real-time security alerts.

---

## 2. System Architecture & Tech Stack

```text
                                        PRAHAR 2.0 SYSTEM ARCHITECTURE
                                      +--------------------------------+
                                      |   CITIZEN / ANALYST CLIENT     |
                                      | - Web App (React 18, Vite 5)   |
                                      | - Chrome Extension (MV3)       |
                                      +--------------------------------+
                                                      │
                                                      │ HTTPS / WSS / SSE
                                                      ▼
                                      +--------------------------------+
                                      |     NGINX REVERSE PROXY        |
                                      | - Port 3000 / Security Headers |
                                      | - SSE Unbuffered Passthrough   |
                                      +--------------------------------+
                                                      │
                                                      │ HTTP / Bearer JWT
                                                      ▼
                                      +--------------------------------+
                                      |     SPRING BOOT 3.3.4 CORE     |
                                      | - Rate Limiter (Token Bucket)  |
                                      | - SSRF Defense Shield          |
                                      | - Correlation Tracing (MDC)    |
                                      | - PII Sensitive Scrubber       |
                                      | - Multi-Channel Analyzers      |
                                      | - SSE Notification Engine      |
                                      +--------------------------------+
                                          │               │          │
                 ┌────────────────────────┘               │          └────────────────────────┐
                 ▼                                        ▼                                   ▼
+--------------------------------+   +--------------------------------+   +--------------------------------+
|    MYSQL 8.0 DURABLE DB        |   |    FASTAPI PYTHON 3.11 ML      |   |   GOOGLE GEMINI 1.5 (OPTIONAL) |
| - Users, Scans, Ledgers        |   | - Feature Extractor (30 feats) |   | - Cloud Generative AI          |
| - Incidents, Notifications     |   | - Logistic Regression / XGBoost|   | - Server-Side Proxied Only     |
| - Chat History (Conversations) |   | - Port 8000 / Model Container  |   | - Zero Client Key Exposure     |
+--------------------------------+   +--------------------------------+   +--------------------------------+
```

### Technology Matrix
- **Backend API:** Java 17/21/25, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, Hibernate, JJWT 0.12.6, ZXing 3.5.3 (QR Decoding).
- **Machine Learning Microservice:** Python 3.11, FastAPI 0.111.0, Uvicorn, Scikit-learn 1.5, XGBoost, Pandas, NumPy.
- **Frontend Web Application:** React 18, Vite 5.4, TailwindCSS 3.4, Lucide Icons, Axios, Context API.
- **Browser Protection Extension:** Manifest V3, Chrome Extension APIs (`activeTab`, `storage`), vanilla JS/CSS.
- **Database & Storage:** MySQL 8.0 with InnoDB, connection pooling via HikariCP, H2 in-memory for testing.
- **Deployment & DevOps:** Docker, Docker Compose v2.20+, Nginx Alpine, PowerShell automation.

---

## 3. Core Modules & Engineering Highlights

### 3.1 Multi-Channel Threat Scanner
- **Lexical & Structural Analysis:** Evaluates 30+ indicators: entropy score, punycode/IDN homograph attacks, IP address hosts, excessive subdomains, brand token spoofing, suspicious TLDs, and path segment depth.
- **ZXing QR Engine:** Validates uploaded images against magic bytes (PNG, JPEG, WebP, GIF, BMP) before parsing 2D matrices. Rejects dangerous schemes (`javascript:`, `data:`, `file:`) and extracts nested URLs for automated scanning.
- **SSRF Defense Shield:** Custom DNS pre-resolution blocks loopback addresses (`127.0.0.1`), link-local metadata endpoints (`169.254.169.254`), and private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) prior to network lookups.

### 3.2 Machine Learning Pipeline
- **Dataset:** 177 curated threat/benign instances in `ml-service/data/raw/phishing_dataset.csv`.
- **Feature Extraction:** 30 engineered features spanning length metrics, character ratios, Shannon entropy, domain depth, and keyword tokens.
- **Model Training:** Automated comparison pipeline evaluating Logistic Regression, Random Forest, Decision Tree, and Gradient Boosting. Logistic Regression selected via stratified 5-fold cross-validation (`ml-service/models/best_model.joblib`).
- **Explainability & Zero Fabrication:** Every ML prediction returns probability, decision rationale, and top influencing feature contributions. If the ML container is offline, the backend degrades safely to the rule engine without generating synthetic ML scores.

### 3.3 Ask Prahar — NLP Conversational Safety Assistant
- **Intent Taxonomy:** 22 security-specific intents covering unauthorized debits, ATM skimming, UPI QR fraud, SIM swap, lost phones, and scam link explanations.
- **Entity Extraction:** Extracts amounts (INR, $, €, ₹), transaction IDs, phone numbers, UPI IDs, and bank names using rule-based and regex extractors.
- **Dual-Mode Intelligence:**
  1. *Online Mode:* Calls Google Gemini 1.5 Flash via OpenAI-compatible endpoints with system instructions enforcing strict safety personas. API keys are kept purely on the backend.
  2. *Deterministic Offline Mode:* If Gemini is unconfigured or rate-limited (HTTP 429), Prahar automatically generates structured legal and containment instructions locally.

### 3.4 Fraud Incident Response & Emergency Containment
- **1930 Helpline & Golden Hour Focus:** Immediate containment guidance for freezing transit amounts within the 2–3 hour window.
- **Dispute Draft Generator:** Pre-populates formal dispute letters formatted for Indian bank grievance cells and cyber police stations, quoting RBI zero-liability rules and IT Act sections.
- **Sensitive Data Scrubber:** Automatic regex scrubbing of credit card numbers (Luhn-compliant), OTPs, and banking PINs from incident descriptions before persistence.

### 3.5 Real-Time Notifications & SSE Engine
- **Event-Driven:** Generates persistent notifications strictly from verified application events (scan risk $\ge 35$ or high risk, registered fraud incidents). Zero fake notifications.
- **One-Way SSE Stream:** Dedicated `SseEmitter` pool with bounded user connections (max 5 emitters/user), 25-second keep-alive heartbeats, and client reconnection support.
- **Tamper-Proof Isolation:** Server-side ownership checks (`userId == principal.getId()`) prevent horizontal privilege escalation. SHA-256 idempotency keys prevent alert flooding.

---

## 4. Comprehensive Test Evidence Matrix

| Test Suite / Layer | Tool / Framework | Executed Commands | Tests Run | Pass | Fail | Elapsed Time | Coverage Highlights |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **Spring Boot Backend** | JUnit 5, Mockito, SpringBootTest | `mvn test` | **109** | **109** | **0** | 29.15s | SSRF blocking, Rate limiting, Token bucket, QR validation, Cross-user isolation, Incident triage, SSE notifications. |
| **Python FastAPI ML** | Pytest, FastAPI TestClient | `python -m pytest` | **27** | **27** | **0** | 5.83s | 30-feature extraction, ML inference, heuristic rule engine, health endpoint, error handling. |
| **Frontend Web App** | Node.js Test Runner | `npm test` | **10** | **10** | **0** | 0.15s | File format magic bytes, oversized upload rejection, HTTP 429/400 classification, QR scheme security. |
| **Browser Extension** | Custom Node.js Harness | `node test_extension.js` | **10** | **10** | **0** | 0.05s | Manifest V3 permissions, zero secret scanning, URL normalization, API schema parity, safe offline fallback. |
| **Frontend Production Build** | Vite 5.4.21 | `npm run build` | **1,603** modules | **1,603** | **0** | 7.36s | TypeScript/JSX compilation, asset minification, CSS bundling (`dist/` generated). |
| **Total Automated Tests** | — | — | **156** | **156** | **0** | — | **100% Pass Rate Across All Suites** |

---

## 5. Security & Privacy Release Audit

1. **Secret Scanning & Key Auditing:**
   - Workspace scanned for Google Gemini API key patterns (`AIzaSy...`), private keys (`*.pem`, `*.key`), and AWS credentials. **Zero committed keys found.**
   - All environment variables are externalized into [.env.example](file:///d:/MCA%20notes/MINI%20PRO%20SIH/.env.example). Real `.env` files are actively ignored by [.gitignore](file:///d:/MCA%20notes/MINI%20PRO%20SIH/.gitignore).
2. **SSRF Defense Verification:**
   - [SsrfProtectionServiceTest.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/test/java/com/phishshield/SsrfProtectionServiceTest.java) validates that `127.0.0.1`, `localhost`, `169.254.169.254`, and private subnets throw `SSRFSecurityException`.
3. **Data Scrubbing Verification:**
   - [SensitiveDataScrubber.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/util/SensitiveDataScrubber.java) tested against 16-digit card numbers, 6-digit OTPs, and 4-digit PINs, ensuring data is masked before logging or persistence.
4. **Rate Limiting & Abuse Defense:**
   - [RateLimitingFilter.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/security/RateLimitingFilter.java) limits `/api/auth/*` to 15 requests/min per IP and `/api/scans/*` to 60 requests/min per IP, returning HTTP 429 on abuse.
5. **Database Durability & Zero-Touch Deployment:**
   - Canonical [schema.sql](file:///d:/MCA%20notes/MINI%20PRO%20SIH/database/schema.sql) and [seed.sql](file:///d:/MCA%20notes/MINI%20PRO%20SIH/database/seed.sql) automatically initialize on startup via MySQL `docker-entrypoint-initdb.d/`.
   - Data persisted across restarts using Docker named volume `mysql_data`.

---

## 6. Demonstration Script & Walkthrough Guide

### Step 1: Launch Services
```powershell
# In root directory, launch multi-service orchestrator:
.\start_services.ps1
```
Alternatively, using Docker Compose:
```bash
docker compose up -d --build
```

### Step 2: Health & Readiness Verification
- Open Browser to: `http://localhost:8080/api/health` $\rightarrow$ Returns `{"status":"UP","service":"prahar-backend"}`
- Open Browser to: `http://localhost:8080/api/health/readiness` $\rightarrow$ Returns database and ML microservice connectivity.
- Open ML Docs: `http://localhost:8000/docs` $\rightarrow$ FastAPI Swagger UI.
- Open Frontend: `http://localhost:3000` (Docker) or `http://localhost:5173` (Vite dev).

### Step 3: Test Fixture Demonstration

| Scenario | Input / Action | Expected Result |
| :--- | :--- | :--- |
| **A. Benign URL Scan** | Scan `https://cybercrime.gov.in` or `https://www.google.com` | Risk Score: **0–15/100 (SAFE)**. Green shield badge, explains verified domain trust. |
| **B. Phishing URL Scan** | Scan `http://secure-sbi-portal-verify.top/kyc-update` | Risk Score: **85–95/100 (HIGH RISK)**. Red alert badge, flags brand spoofing, insecure HTTP, and deceptive TLD. |
| **C. Benign QR Scan** | Upload `media/demo_fixtures/benign_qr_fixture.png` | Decodes QR $\rightarrow$ extracts `https://cybercrime.gov.in` $\rightarrow$ Verdict: **SAFE**. |
| **D. Phishing QR Scan** | Upload `media/demo_fixtures/phishing_qr_fixture.png` | Decodes QR $\rightarrow$ extracts deceptive URL $\rightarrow$ Verdict: **PHISHING**. Triggers real-time notification in navbar bell. |
| **E. Phishing Screenshot** | Upload `media/demo_fixtures/phishing_screenshot_fixture.png` | Magic bytes verified $\rightarrow$ visual analysis performed $\rightarrow$ extracts suspicious link $\rightarrow$ flags threat. |
| **F. Ask Prahar NLP** | Query: *"₹15,000 debited from my SBI account without OTP"* | Intent: `UNAUTHORIZED_DEBIT`. Highlights Golden Hour protocol, direct link to 1930, and dispute letter generator. |
| **G. Safety Centre** | Navigate to Safety Centre $\rightarrow$ Incident Reporting | Generates pre-filled RBI Zero-Liability legal dispute letter ready for PDF/print export. |
| **H. Real-Time Bell** | Check top-right bell in Navbar | Unread badge increments dynamically via Server-Sent Events (SSE). |

---

## 7. Academic Limitations & Future Enhancements

### 7.1 Identified Limitations
1. **ML Training Corpus Size:** The local training dataset contains 177 curated samples for demonstration. While high accuracy is achieved on this benchmark, enterprise production deployment requires retraining on large-scale threat feeds (e.g., PhishTank, OpenPhish).
2. **OCR System Dependency:** Full screenshot text extraction relies on Tesseract OCR. When the Tesseract binary is not installed in the OS path, the platform degrades honestly without hallucinating text.
3. **Browser Extension Packaging:** The extension runs in Developer Mode as an unpacked Manifest V3 extension; Chrome Web Store publication requires domain verification and Google developer registration.

### 7.2 Future Research & Roadmap
- Integration of lightweight on-device Transformer embeddings (e.g., DistilBERT) directly within FastAPI for sub-50ms lexical sequence classification.
- Multi-factor WebAuthn/FIDO2 hardware security key support for administrative login.
- Integration with live Indian carrier API webhooks for automated SIM swap status verification.

---

## 8. Final Release Decision

```text
================================================================================
FINAL EVALUATION DECISION: READY FOR CONTROLLED DEMO & ACADEMIC DEFENSE
================================================================================
Criteria:
- All 156 automated test cases passing (109 Backend, 27 ML, 10 Frontend, 10 Extension).
- Production build succeeds without errors (1,603 Vite modules minified).
- Zero secret keys in repository tracking; comprehensive .env.example provided.
- Multi-channel inspection (URL, QR, Screenshot, NLP Chat, Ledger, Incidents) fully functional.
- Zero fabrication guarantee strictly enforced across all fallbacks.
================================================================================
```
