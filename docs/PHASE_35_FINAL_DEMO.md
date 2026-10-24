# PHASE 35 — FINAL REPRODUCIBLE DEMONSTRATION & HANDOVER PACKAGE

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Document Level:** Final Academic Demonstration Guide & Examiner Execution Manual  
**Operating Baseline:** 100% Offline Capable, Zero Paid External Dependencies, 186/186 Tests Passing

---

## 1. Verified Prerequisites

Before starting the application, ensure the host system has the following runtimes installed and available in the system PATH:

| Runtime / Tool | Verified Version | Verification Command | Purpose |
| :--- | :--- | :--- | :--- |
| **Java Development Kit** | OpenJDK 17.x or higher | `java -version` | Spring Boot 3.3.4 Core Backend |
| **Apache Maven** | Maven 3.8.x or higher | `mvn -version` | Java Dependency Management & Build |
| **Python** | Python 3.11.x | `python --version` | FastAPI Microservice & XGBoost / Scikit-Learn |
| **Node.js & npm** | Node.js v18.x or v20.x | `node -v` & `npm -v` | React 18 / Vite 5 Frontend SPA |
| **Google Chrome / Edge** | Modern Chromium Browser | — | User Dashboard & Browser Extension |

---

## 2. Environment Configuration

Prahar is designed to execute out-of-the-box using safe default settings and in-memory mock fallbacks. No paid cloud services are required.

### 2.1 Backend Configuration (`backend/src/main/resources/application.yml`)
Key defaults verified:
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:h2:mem:phishshield;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: update
ml:
  service:
    url: http://localhost:8000
    timeout-ms: 2000
security:
  rate-limit:
    scans-per-minute: 60
    auth-per-minute: 15
gemini:
  api:
    key: ${GEMINI_API_KEY:}   # Optional: If empty, local RuleBasedNlpEngine handles all queries
```

### 2.2 Optional External API Key Setup
If an examiner wishes to test live Google Gemini 1.5 Flash generative AI responses instead of the built-in deterministic NLP engine:
```powershell
# Optional: Set in PowerShell before starting backend
$env:GEMINI_API_KEY = "your-gemini-api-key-here"
```
*(If unset, Prahar functions 100% offline with zero degradation).*

---

## 3. Step-by-Step Service Startup Procedure

To run the full Prahar platform, open three separate terminal windows in the project root:

### Terminal 1: Start Python ML Microservice (Port 8000)
```powershell
cd "d:\MCA notes\MINI PRO SIH\ml-service"
# Start FastAPI Uvicorn ASGI server
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- **Expected Console Output:**
  ```text
  INFO:     Initializing PhishShield Machine Learning Microservice...
  INFO:     Successfully loaded ML model 'Logistic Regression' from models/best_model.joblib
  INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
  ```

### Terminal 2: Start Spring Boot Core Backend (Port 8080)
```powershell
cd "d:\MCA notes\MINI PRO SIH\backend"
# Launch Spring Boot application
mvn spring-boot:run
```
- **Expected Console Output:**
  ```text
  :: Spring Boot ::                (v3.3.4)
  INFO  c.phishshield.PhishShieldBackendApplication - Started PhishShieldBackendApplication in ~7.5 seconds
  INFO  c.phishshield.config.DataInitializer - Initialized default admin user: admin [development mode]
  INFO  c.phishshield.config.DataInitializer - Seeded initial security advisories
  ```

### Terminal 3: Start React 18 Frontend Dashboard (Port 5173)
```powershell
cd "d:\MCA notes\MINI PRO SIH\frontend"
# Launch Vite development dev server
npm run dev
```
- **Expected Console Output:**
  ```text
  VITE v5.4.21  ready in 450 ms
  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ```

---

## 4. Service Health Checks

Open a browser or secondary terminal and verify operational readiness:

1. **ML Microservice Health:**
   ```powershell
   curl http://localhost:8000/health
   ```
   *Expected Response:* `{"status":"UP","service":"prahar-ml-service","model_loaded":true,"model_name":"Logistic Regression"}`
2. **Backend Gateway Liveness Check:**
   ```powershell
   curl http://localhost:8080/api/health
   ```
   *Expected Response:* `{"success":true,"message":"System is alive and operational","data":{"status":"UP","service":"prahar-backend"}}`
3. **Backend ML Integration Status:**
   ```powershell
   curl http://localhost:8080/api/scans/ml-status
   ```
   *Expected Response:* `{"success":true,"message":"ML service status","data":{"status":"HEALTHY","available":true}}`

---

## 5. Live Demonstration Sequence (8–12 Minutes)

Open browser at `http://localhost:5173` and execute the following verified test sequence:

### Step 1: Benign URL Fast-Path Whitelist Demo
- **Action:** In the URL scanner (`/scan`), enter:
  ```text
  https://onlinesbi.sbi/portal/login.htm
  ```
- **Expected Result:**
  - Renders a green **SAFE** verdict banner.
  - Risk Score: `0 / 100`.
  - Threat Category: `BENIGN`.
  - Latency: $<15\text{ms}$.
- **Examiner Explanation:** Explain that Prahar maintains apex domain whitelists for critical national banking infrastructure, suppressing false positive alerts on authentic utility portals.

### Step 2: Safe Synthetic Phishing Lure Demo
- **Action:** Paste the safe synthetic phishing fixture:
  ```text
  http://secure-login-update.sbi.bank.account-verification.xyz/auth/signin?token=928374
  ```
  *(Do NOT attempt to open this link in a browser tab).* Click **"Analyze URL"**.
- **Expected Result:**
  - Renders a red **HIGH RISK / PHISHING DETECTED** banner.
  - Risk Score: between `88` and `95 / 100`.
  - Threat Category: `CREDENTIAL_THEFT`.
- **Examiner Explanation:** Walk through the **Explainability Breakdown Card**:
  - Show Heuristic Rule 04: Brand Impersonation (`sbi`, `bank` in third-level subdomain).
  - Show Heuristic Rule 12: High-risk TLD (`.xyz`).
  - Show the ML prediction confidence and multi-signal score floor ($75$).

### Step 3: Quishing (QR Phishing) & Image Analysis
- **Action:** Navigate to **"Image & QR Scanner"** (`/image-scan`). Upload `media/demo_fixtures/phishing_qr_fixture.png`.
- **Expected Result:**
  - ZXing QR engine extracts encoded destination link: `http://fake-banking-reward.xyz/claim`.
  - URL is automatically routed into the security pipeline and flagged as **HIGH RISK**.
- **Examiner Explanation:** Explain the Quishing vector used by cybercriminals on parking meters and deceptive emails to evade traditional text-based email filters.

### Step 4: SSRF Perimeter Security Defense
- **Action:** Submit an internal network probe into the URL scanner:
  ```text
  http://127.0.0.1:8080/actuator/env
  ```
- **Expected Result:**
  - System immediately returns HTTP 400 Bad Request with:
    `"Security Policy Violation: The domain resolves to a restricted/private network address: 127.0.0.1"`.
- **Examiner Explanation:** Explain that `SSRFProtectionService.java` resolves DNS pre-fetch and terminates requests targeting loopback (`127.0.0.0/8`), RFC-1918 private subnets, and AWS metadata IPs (`169.254.169.254`) before any network socket is opened.

### Step 5: Cybersecurity AI Chatbot Advisor
- **Action:** Navigate to **"AI Incident Assistant"** (`/assistant`). Submit:
  ```text
  I received an SMS claiming my electricity bill is unpaid with an APK download link. What should I do?
  ```
- **Expected Result:**
  - Assistant provides structured step-by-step guidance: (1) Do not click link or install unknown APK, (2) Verify bill on official electricity provider app, (3) Report to National Cyber Crime Portal (`1930` / `cybercrime.gov.in`).
- **Examiner Explanation:** Highlight that even without a paid Google Gemini API key, Prahar's local deterministic NLP engine (`RuleBasedNlpEngine.java`) reliably serves verified cybercrime playbooks.

---

## 6. Troubleshooting Instructions

| Issue | Probable Cause | Single-Line Resolution |
| :--- | :--- | :--- |
| **Port 8080 already in use** | Stray Java process from previous run | In PowerShell: `Get-Process java \| Stop-Process -Force` |
| **Port 8000 already in use** | Stray Python process | In PowerShell: `Get-Process python \| Stop-Process -Force` |
| **Frontend displays white screen** | Stale Vite cache | Hard refresh browser using `Ctrl + F5` |
| **ML Service shows DOWN in backend** | FastAPI service not started | Start microservice: `python -m uvicorn app.main:app --port 8000` |
| **Database table lock in H2** | Stale in-memory handle | Restart Spring Boot backend: `mvn spring-boot:run` |

---

## 7. Shutdown & Cleanup Procedure

When the demonstration and viva examination conclude:

1. In each terminal window, press **`Ctrl + C`** to gracefully stop Vite, Spring Boot, and Uvicorn.
2. In-memory H2 database tables and transient token buckets automatically release all memory.
3. No lingering background daemons or altered system configurations remain.
