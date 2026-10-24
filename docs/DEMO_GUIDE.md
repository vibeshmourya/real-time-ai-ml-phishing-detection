# Prahar 2.0 — Repeatable Demonstration & Evaluation Guide

This guide provides step-by-step instructions for conducting a safe, repeatable, and technically sound local demonstration of the Prahar Cybersecurity Platform for academic juries, evaluators, and project defense.

---

## 1. System Prerequisites

The following software versions have been verified on the current source tree:

| Component | Minimum Version | Recommended / Tested Version |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS 12+, or Ubuntu 22.04+ | Windows 11 / Ubuntu 22.04 LTS |
| **Java Development Kit (JDK)** | OpenJDK 17 | OpenJDK 21 or 25 |
| **Apache Maven** | Maven 3.8+ | Maven 3.9.6 |
| **Python** | Python 3.10+ | Python 3.11 |
| **Node.js & npm** | Node.js 18+ & npm 9+ | Node.js v20+ & npm 10+ |
| **Docker & Compose** *(Optional)* | Docker 24+ & Compose v2.20+ | Docker Desktop 4.30+ |

---

## 2. Environment Setup (Zero Secrets Required)

Prahar is designed to operate out-of-the-box using safe local defaults (in-memory H2 database and local heuristic rule fallback). No cloud API keys or database servers are required for the standard demonstration.

If configuring custom environment variables, copy `.env.example`:
```bash
cp .env.example .env
```
Key defaults applied automatically in development mode:
- `SPRING_PROFILES_ACTIVE=dev` (Runs embedded in-memory H2 database; zero database setup required).
- `AI_PROVIDER=none` (Activates local 22-intent rule-based safety engine; zero Gemini API key required).
- `ML_SERVICE_URL=http://localhost:8000` (Connects to local FastAPI microservice).

---

## 3. Launching Services

### Option A: Windows PowerShell Multi-Service Orchestrator (Recommended)

Run the unified orchestrator script from the project root:
```powershell
.\start_services.ps1
```

**Script Flow:**
1. Launches **FastAPI ML Service** on `http://127.0.0.1:8000` in a new PowerShell window.
2. Prompts optionally for Gemini AI key. Press `Enter` (or type `n`) to use the local offline safety engine.
3. Launches **Spring Boot Backend** on `http://127.0.0.1:8080` in a new PowerShell window.
4. Launches **Vite Frontend Web App** on `http://localhost:5173` in a new PowerShell window.

---

### Option B: Native Manual Startup (Three Terminals)

#### Terminal 1 — Python ML Microservice
```powershell
cd ml-service
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

#### Terminal 2 — Spring Boot Backend API
```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

#### Terminal 3 — React Frontend Web App
```powershell
cd frontend
npm install
npm run dev
```

---

### Option C: Docker Compose Full Stack (Containers)
```bash
docker compose up -d --build
```
*Frontend runs on `http://localhost:3000`, Backend on `http://localhost:8080`, ML on `http://localhost:8000`.*

---

## 4. Verifying System Health

Before demonstrating to evaluators, verify that all services are alive:

```bash
# 1. Spring Boot Liveness Check
curl http://localhost:8080/api/health

# 2. Comprehensive System Readiness Check (Probes DB and ML microservice)
curl http://localhost:8080/api/health/readiness

# 3. Python ML Microservice Direct Health Check
curl http://localhost:8000/health
```

**Expected Response (`/api/health/readiness`):**
```json
{
  "success": true,
  "message": "Prahar system is ready for traffic",
  "data": {
    "database": { "status": "UP" },
    "mlService": { "status": "UP", "modelLoaded": true },
    "aiProvider": { "status": "OFFLINE_RULE_FALLBACK", "provider": "none" },
    "overallStatus": "READY"
  }
}
```

---

## 5. Safe Demonstration Sequence

All demo fixtures are located in [`media/demo_fixtures/`](../media/demo_fixtures/):

### Test 1: Benign Legitimate Portal Scan
1. Open the web app at `http://localhost:5173/scanner`.
2. Enter URL: `https://cybercrime.gov.in`
3. Click **Analyze Threat**.
4. **Expected Result:**
   - **Verdict:** `SAFE / LOW RISK` (Score: 0–15/100).
   - **Badges:** Official Domain, Valid HTTPS, No Suspicious Keywords.

---

### Test 2: Controlled Phishing Pattern Scan
1. In the URL scanner, enter: `http://secure-sbi-portal-verify.top/kyc-update`
2. Click **Analyze Threat**.
3. **Expected Result:**
   - **Verdict:** `PHISHING / HIGH RISK` (Score: $\ge 75/100$).
   - **Triggered Indicators:** Suspicious TLD (`.top`), Brand Impersonation (`sbi-portal-verify`), Security Urgency (`kyc-update`), Plain HTTP without SSL.

---

### Test 3: Synthetic QR Code Analysis
1. Select the **QR / Screenshot** tab in QuickScanner.
2. **Scenario A (Benign):** Upload [`media/demo_fixtures/benign_qr_fixture.png`](../media/demo_fixtures/benign_qr_fixture.png).
   - **Decoded Payload:** `https://cybercrime.gov.in`
   - **Verdict:** `SAFE`
3. **Scenario B (Phishing QR):** Upload [`media/demo_fixtures/phishing_qr_fixture.png`](../media/demo_fixtures/phishing_qr_fixture.png).
   - **Decoded Payload:** `http://secure-sbi-portal-verify.top/kyc-update`
   - **Verdict:** `PHISHING / HIGH RISK`

---

### Test 4: Screenshot & OCR Analysis
1. Upload [`media/demo_fixtures/phishing_screenshot_fixture.png`](../media/demo_fixtures/phishing_screenshot_fixture.png).
2. The scanner extracts candidate text and analyzes visual urgency patterns.
3. **Expected Result:** Flags urgency wording ("Account Suspended within 24 Hours", "Verify KYC Now").

---

### Test 5: Real-Time Security Alert Notification
1. Immediately following Test 2 or Test 3 (Scenario B), observe the **Notification Bell** icon in the navbar.
2. A red badge counter will increment.
3. Click the bell to view the alert delivered in real time via **Server-Sent Events (SSE)**.
4. Click the notification to view the linked threat report.

---

### Test 6: Offline Conversational Assistant ("Ask Prahar")
1. Navigate to `/assistant` ("Ask Prahar").
2. Type: `"I got an SMS asking to update my electricity bill KYC immediately. Is this fake?"`
3. **Expected Result:**
   - The 22-intent rule engine recognizes the `ELECTRICITY_KYC_SCAM` intent.
   - Provides immediate Indian context containment advice: Do not click unknown links, contact official power DISCOM, and call **1930 Helpline**.

---

### Test 7: Fraud Incident Response & Dispute Letter Generation
1. Navigate to `/fraud-centre`.
2. Fill out a simulated transaction dispute: Bank Name = `State Bank of India`, Amount = `₹25,000`, Fraud Type = `Unauthorized UPI Debit`.
3. Click **Generate Formal Dispute Letter**.
4. **Expected Result:** Compiles a legally structured complaint letter citing the **RBI Zero-Liability Circular** (`RBI/2017-18/15`) and **IT Act 2000** (`Section 66D`).

---

## 6. Browser Extension Demonstration

1. Open Google Chrome and navigate to `chrome://extensions`.
2. Toggle **Developer mode** in the top-right corner.
3. Click **Load unpacked** and select the [`browser-extension/`](../browser-extension/) folder.
4. Pin the Prahar extension icon to the browser toolbar.
5. Click the extension popup while viewing any web page:
   - Displays real-time URL risk analysis.
   - Demonstrates zero external secret leakage and strict permission boundaries (`activeTab`, `storage`).

---

## 7. Troubleshooting & Recovery

| Symptom | Cause | Resolution |
| :--- | :--- | :--- |
| **ML Status shows "DEGRADED"** | FastAPI service on port 8000 is not running. | Open a terminal in `ml-service/` and run `python -m uvicorn app.main:app --port 8000`. Spring Boot automatically falls back to heuristic rules. |
| **Port 8080 already in use** | Another process is holding port 8080. | Stop the conflicting process or launch with `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"`. |
| **CORS error on scanner** | Origin not in allowed list. | Ensure you access frontend via `http://localhost:5173` or `http://localhost:3000`. |
| **OCR returns "UNEXTRACTED"** | Tesseract binary is not installed on host OS. | Normal behavior. Prahar gracefully handles uninstalled OCR binaries without error. |

---

## 8. Shutdown & Cleanup

To stop all running services:
- Close the terminal windows opened by `start_services.ps1` (or press `Ctrl+C` in each window).
- If running via Docker Compose:
  ```bash
  docker compose down
  ```
