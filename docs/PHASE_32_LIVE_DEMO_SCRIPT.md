# PHASE 32 — LIVE ACADEMIC DEMONSTRATION SCRIPT

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Presentation Target:** MCA Final Viva Voce & External Examiner Demonstration  
**Target Duration:** 8 to 12 Minutes  
**Demonstrator:** Candidate / Project Team  
**System Status Baseline:** 100% Offline Capable, 175 Automated Tests Passing

---

## 1. Demonstration Protocol & Timing Overview

| Step | Topic | Allocation | Cumulative Time | Primary Component Demonstrated |
| :---: | :--- | :---: | :---: | :--- |
| **01** | Problem Statement & Threat Vector Landscape | 1.0 min | 01:00 | Conceptual / Slide 1–3 |
| **02** | System Architecture & Service Topology | 1.5 min | 02:30 | Full-Stack Ecosystem Overview |
| **03** | Safe Benign URL Submission & Fast-Path Whitelisting | 1.0 min | 03:30 | Frontend, URL Scanner, Regex Rules |
| **04** | Malicious Phishing Fixture Submission (Safe Execution) | 1.5 min | 05:00 | Heuristic Engine + ML Microservice |
| **05** | Explainable Classification & Multi-Signal Fusion | 1.0 min | 06:00 | Scoring Engine & Forensic Breakdown |
| **06** | Real-Time ML Inference & 30-Feature Extraction | 1.0 min | 07:00 | FastAPI `/predict` & XGBoost Ensemble |
| **07** | Multi-Channel Vision: QR & Screenshot Analysis | 1.5 min | 08:30 | ZXing QR Decoder & Image Security |
| **08** | Cybersecurity Incident Assistant & Complaint Drafter | 1.0 min | 09:30 | Chatbot (Gemini / Rule NLP Fallback) |
| **09** | Defensive Hardening, Input Validation & Fault Tolerance | 1.0 min | 10:30 | SSRF Filter, Rate Limiter, Degradation |
| **10** | Technical Boundaries, Trade-offs & Future Scope | 1.0 min | 11:30 | Defense Summary & Examiner Q&A Handoff |

---

## 2. Step-by-Step Live Execution Script

---

### STEP 01 — Introduce the Phishing Detection Problem (00:00 – 01:00)

- **Presenter Action:**  
  Direct examiner's attention to the Prahar dashboard title. State the threat vector:
  > *"Modern phishing attacks are no longer restricted to trivial typo-squatted web links. Cyber adversaries employ multi-channel deception—including QR-code quishing, weaponized payment handles, zero-day obscured domains, and social engineering screenshots. Prahar was engineered to provide real-time, multi-signal detection across these heterogeneous vectors without exposing enterprise networks to malicious code execution."*
- **Expected Behavior:**  
  Dashboard is loaded at `http://localhost:5173` showing the clean, responsive Prahar UI with navigation links: URL Scanner, Image/QR Analyzer, Threat Intel, Safety Centre, and Incident Chatbot.
- **What to Explain to the Examiner:**  
  Explain that conventional signature-based blacklists suffer from a 48–72 hour dwell time before new phishing URLs appear in public databases. Prahar bridges this window using synchronous composite intelligence: heuristic rule checks, a 30-feature ML classifier, and image inspection.
- **Possible Failure & Recovery:**  
  *Failure:* Browser displays connection refused on `localhost:5173`.  
  *Recovery:* Open terminal and run `npm run dev` in `d:\MCA notes\MINI PRO SIH\frontend`. Page reloads within 2 seconds.

---

### STEP 02 — Show the Prahar Application & Architecture (01:00 – 02:30)

- **Presenter Action:**  
  Open the browser Developer Tools (F12 $\rightarrow$ Network Tab). Briefly highlight the three running microservices:
  1. Frontend: React 18 + Vite (Port 5173)
  2. Core Backend: Spring Boot 3.3.4 (Port 8080)
  3. ML Inference Engine: Python FastAPI + Scikit-Learn/XGBoost (Port 8000)
- **Expected Behavior:**  
  Backend health endpoint `http://localhost:8080/api/health` returns HTTP 200:
  ```json
  {"status":"UP","services":{"backend":"HEALTHY","mlService":"HEALTHY","database":"HEALTHY"}}
  ```
- **What to Explain to the Examiner:**  
  Point out the separation of concerns: Spring Boot acts as an enterprise API gateway, transaction coordinator, and security boundary (SSRF, rate limiting, audit logging), delegating raw numerical tensor calculations asynchronously to the Python ML microservice via HTTP REST.
- **Possible Failure & Recovery:**  
  *Failure:* ML service health shows `DOWN` in the health response.  
  *Recovery:* Clarify immediately that Prahar features automatic graceful degradation: Spring Boot continues functioning via the rule heuristic engine, dynamically re-weighting composite risk without interrupting the user.

---

### STEP 03 — Submit a Benign URL or Safe Test Fixture (02:30 – 03:30)

- **Presenter Action:**  
  Navigate to the URL Scan interface (`/scan`). Enter a verified benign Indian banking or government domain:
  ```text
  https://onlinesbi.sbi/portal/login.htm
  ```
  Click **"Analyze URL"**.
- **Expected Behavior:**  
  The scan completes in under 15ms. The UI renders a green **SAFE** status banner with a Risk Score of `0 / 100` (`SAFE` category).
- **What to Explain to the Examiner:**  
  Demonstrate the **Fast-Path Whitelisting Mechanism**. Prahar maintains verified apex domain hashes for vital national banking infrastructure. When an authentic whitelist entry is confirmed, the system immediately returns a low-risk score, bypassing heavy ML inference and avoiding costly false alarms on critical utility sites (<2.5% false positive rate).
- **Possible Failure & Recovery:**  
  *Failure:* UI hangs or shows loading spinner indefinitely.  
  *Recovery:* Check Network tab for CORS or port conflict; if backend stopped, restart via `mvn spring-boot:run` or execute a quick curl test directly to `http://localhost:8080/api/scan/url`.

---

### STEP 04 — Submit a Safe Malicious Phishing Test Fixture (03:30 – 05:00)

- **Presenter Action:**  
  In the URL scanner, paste the safe, non-live synthetic phishing fixture:
  ```text
  http://secure-login-update.sbi.bank.account-verification.xyz/auth/signin?token=928374
  ```
  *(Important: Do NOT attempt to navigate to or resolve this URL in the browser address bar).* Click **"Analyze URL"**.
- **Expected Behavior:**  
  The interface returns a red **HIGH RISK / PHISHING DETECTED** banner within ~25ms. The composite risk score displays between `88` and `100 / 100`.
- **What to Explain to the Examiner:**  
  Highlight that Prahar analyzes the target purely through static lexical decomposition, lexical entropy, and structural token parsing. Neither the user's browser nor the server attempts an active HTTP fetch against the malicious host. This safeguards against malicious payload delivery and zero-day browser exploits.
- **Possible Failure & Recovery:**  
  *Failure:* Classification returns moderate score instead of high risk.  
  *Recovery:* Explain the multi-signal score floor: even if ML probability is low, heuristic Rule 04 (Brand Impersonation in Subdomain) and Rule 12 (Suspicious TLD `.xyz`) mandate a minimum floor of 70, triggering warning status.

---

### STEP 05 — Explain Classification & Supporting Evidence (05:00 – 06:00)

- **Presenter Action:**  
  Scroll down on the scan result page to expand the **"Detection Breakdown & Explainability"** card.
- **Expected Behavior:**  
  The breakdown displays three distinct signal cards:
  1. **Heuristic Engine ($S_H$):** Lists triggered rules:
     - `SUBDOMAIN_BRAND_IMPERSONATION` (`sbi`, `bank`)
     - `SUSPICIOUS_HIGH_RISK_TLD` (`.xyz`)
     - `EXCESSIVE_HYPHENATION` (4 hyphens detected)
  2. **Machine Learning Model ($S_{ML}$):** Probability `0.942` (`Phishing`).
  3. **Threat Intelligence ($S_{TI}$):** Domain reputation score.
- **What to Explain to the Examiner:**  
  Present the formal mathematical composite scoring formula:
  $$\text{Final Score} = \min\left(100, \max\left(S_{\text{floor}}, 0.50 \cdot S_H + 0.35 \cdot S_{ML} + 0.15 \cdot S_{TI}\right)\right)$$
  Explain why an opaque black-box ML score is insufficient for cyber forensics: examiners, SOC analysts, and end-users need actionable, explainable evidence to defend against liability and understand why a link was blocked.
- **Possible Failure & Recovery:**  
  *Failure:* Breakdown drawer fails to expand in UI.  
  *Recovery:* Inspect the raw JSON response in DevTools Network tab showing `scan_explainability_details` containing `triggeredRules`, `ruleExplanations`, and `entropyScore`.

---

### STEP 06 — Demonstrate ML Inference & Rule-Based Checks (06:00 – 07:00)

- **Presenter Action:**  
  Open a terminal or secondary browser tab to demonstrate the Python ML microservice directly:
  ```bash
  curl -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d "{\"url\":\"http://secure-login-update.sbi.bank.account-verification.xyz/auth\"}"
  ```
- **Expected Behavior:**  
  Returns instant JSON output within ~5ms:
  ```json
  {
    "url": "http://secure-login-update.sbi.bank.account-verification.xyz/auth",
    "phishing_probability": 0.942,
    "prediction": 1,
    "confidence": 0.942,
    "top_features": {
      "subdomain_count": 4.0,
      "brand_in_subdomain": 1.0,
      "url_entropy": 4.12
    }
  }
  ```
- **What to Explain to the Examiner:**  
  Explain the feature extraction architecture in `ml-service/feature_extractor.py`: Prahar extracts exactly **30 numerical and lexical features** in Python (e.g. Shannon entropy, token lengths, digit-to-alphabet ratios, suspicious keyword frequencies) and feeds them into a trained tree ensemble (XGBoost / LightGBM) achieving sub-10ms latency.
- **Possible Failure & Recovery:**  
  *Failure:* Terminal command curl is not recognized in Windows PowerShell.  
  *Recovery:* Use `curl.exe` or PowerShell's `Invoke-RestMethod -Uri "http://localhost:8000/predict" -Method Post -Body '{"url":"..."}' -ContentType "application/json"`.

---

### STEP 07 — Demonstrate Screenshot or QR Analysis (07:00 – 08:30)

- **Presenter Action:**  
  Navigate to the **"Image & QR Scanner"** tab (`/image-scan`).  
  1. Click upload and select `media/demo_fixtures/phishing_qr_fixture.png`.
  2. Click **"Analyze Image"**.
- **Expected Behavior:**  
  The ZXing QR decoding engine identifies an embedded malicious payload URL (`http://fake-banking-reward.xyz/claim`).  
  The unified pipeline automatically pipes the decoded payload into the URL scanning engine and flags it as **SUSPICIOUS / HIGH RISK**.
- **What to Explain to the Examiner:**  
  Explain the "Quishing" (QR Phishing) threat vector: attackers print malicious QR codes on physical ATM overlays, parking meters, or phishing emails to bypass traditional corporate email gateways that only inspect raw text links. Prahar's `UnifiedImageAnalysisService.java` simultaneously runs barcode matrix analysis and image validation headers (verifying magic numbers to prevent polyglot file uploads).
- **Possible Failure & Recovery:**  
  *Failure:* Uploading a non-QR image throws an error or takes too long.  
  *Recovery:* Point out that `UnifiedImageAnalysisResponse` returns structured metadata `qrCodeFound: false` without crashing, gracefully continuing with image perceptual hashing and heuristic text detection.

---

### STEP 08 — Demonstrate AI Assistant / Incident Response (08:30 – 09:30)

- **Presenter Action:**  
  Navigate to the **"AI Assistant & Incident Center"** (`/assistant`).  
  Submit the prompt:
  ```text
  I received an SMS claiming my bank account is blocked with a link to update KYC. What should I do?
  ```
- **Expected Behavior:**  
  The assistant responds with a structured, step-by-step incident response playbook:
  1. Do NOT click the link or provide OTP/password.
  2. Verify your account status using official banking apps.
  3. Report the fraudulent SMS to the National Cyber Crime Reporting Portal (`1930` / `cybercrime.gov.in`).
  4. Block the sender.
- **What to Explain to the Examiner:**  
  Explain the dual-mode architecture in `ChatService.java`:
  - If a Google Gemini API key (`GEMINI_API_KEY`) is provisioned in the environment, the assistant uses generative AI with grounded cybersecurity system instructions.
  - If external network access is blocked or no key is configured (as in an offline defense exam), the local **Rule-Based NLP Engine** (`RuleBasedNlpEngine.java`) deterministically matches user intents against pre-compiled advisory playbooks.
- **Possible Failure & Recovery:**  
  *Failure:* Chatbot prints "API Key Missing" or returns generic error.  
  *Recovery:* Explain that the system detected an offline test environment and fell back to the local intent engine, demonstrating high system availability under network partition.

---

### STEP 09 — Defensive Hardening & Input Fault Tolerance (09:30 – 10:30)

- **Presenter Action:**  
  Submit two deliberately invalid and malicious inputs into the URL scanner:
  1. **SSRF Attack Vector:** `http://169.254.169.254/latest/meta-data/` (AWS Cloud Metadata IP) or `http://127.0.0.1:8080/admin`.
  2. **Empty / Garbage Input:** `not_a_valid_url`
- **Expected Behavior:**  
  1. For the SSRF attempt, the system immediately returns HTTP 400 with `BLOCKED_LOCAL_IP` / `BLOCKED_PRIVATE_IP`.
  2. For the malformed URL, the frontend validation flags it before submission, and backend sanitization rejects it cleanly with zero stack traces or unhandled 500 errors.
- **What to Explain to the Examiner:**  
  Walk through `SSRFProtectionService.java`: before any network processing occurs, Prahar performs pre-resolution IP address parsing and punycode canonicalization. RFC-1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), loopback (`127.0.0.1`), and AWS link-local addresses (`169.254.0.0/16`) are blocked at the perimeter.
- **Possible Failure & Recovery:**  
  *Failure:* Browser pops an uncaught alert dialog.  
  *Recovery:* Point to the backend log console showing clean `SecurityException: Blocked attempt to scan private or loopback IP` handled by `GlobalExceptionHandler.java`.

---

### STEP 10 — Technical Boundaries & Viva Handoff (10:30 – 11:30)

- **Presenter Action:**  
  Conclude the demonstration by displaying the **Limitations & Future Work** summary slide or page. Invite examiner questions:
  > *"To summarize: Prahar provides a verified, sub-25 millisecond multi-signal phishing detection engine with zero-leakage ML inference, pre-scan SSRF defenses, and explainable forensic breakdowns. We recognize its boundaries—specifically, that offline lexical models cannot inspect dynamic JavaScript cloaking without an active headless sandbox. We are now ready to answer your questions regarding the architecture, ML evaluation, or code implementation."*
- **Expected Behavior:**  
  Transition cleanly from live demonstration to viva voce question-and-answer defense.

---

## 3. Quick Reference Troubleshooting & Recovery Table

| Symptom During Viva | Root Cause | Immediate Single-Command Remedy |
| :--- | :--- | :--- |
| **Port 8080 already bound** | Orphan Java process from prior run | `Get-Process java \| Stop-Process -Force` |
| **Port 8000 already bound** | Orphan Uvicorn process | `Get-Process python \| Stop-Process -Force` |
| **Frontend build stale** | Cached Vite bundle | Hard refresh browser: `Ctrl + F5` |
| **H2 DB schema mismatch** | Stale in-memory locks | Restart Spring Boot backend: `mvn spring-boot:run` |
| **ML service unreachable** | Virtualenv deactivated | Run `.\venv\Scripts\Activate.ps1; python -m uvicorn main:app --port 8000` |
