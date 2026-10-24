# Prahar Final Demonstration and Handover Guide

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Document Purpose:** Reliable Step-by-Step Viva and Practical Demonstration Runbook  
**Target Environment:** Local Developer Machine (Windows 11 / macOS / Linux)  
**Safety Guarantee:** All demonstration fixtures are synthetic and harmless; no live malicious sites or paid external APIs are required.  

---

## 1. Prerequisites Checklist

Before beginning the demonstration, verify that your workstation has the following tools installed:
- [x] **Java Development Kit (JDK):** Version 17 or higher (`java -version`).
- [x] **Apache Maven:** Version 3.8+ (`mvn -version`).
- [x] **Python:** Version 3.10 or 3.11 (`python --version`).
- [x] **Node.js:** Version 18 or 20 (`node -v`).
- [x] **Web Browser:** Google Chrome, Brave, or Microsoft Edge.

---

## 2. Quick-Start Demonstration Launch

The repository includes a single PowerShell orchestrator script that launches all three microservices in separate terminal windows:

```powershell
cd "d:\MCA notes\MINI PRO SIH"
.\start_services.ps1
```

*Note: When prompted to configure Google Gemini AI, press `Enter` (or type `N`) to use the in-process deterministic 20-intent offline NLP engine without requiring an external API key.*

### Manual Startup (Alternative if not using PowerShell)
If running on Linux or starting services individually:

1. **Terminal 1 — Python FastAPI ML Service:**
   ```bash
   cd ml-service
   python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
   ```
2. **Terminal 2 — Spring Boot Backend:**
   ```bash
   cd backend
   mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```
3. **Terminal 3 — React Frontend Dev Server:**
   ```bash
   cd frontend
   npm run dev
   ```

---

## 3. System Liveness & Readiness Verification

Before presenting to the examiners, open your browser and verify the health endpoints:

1. **Backend Liveness Probe:**  
   Navigate to: `http://localhost:8080/api/health`  
   *Expected Response:* `{"status": "UP", "service": "prahar-backend", "version": "2.0.0"}`
2. **ML Microservice Health:**  
   Navigate to: `http://localhost:8000/health`  
   *Expected Response:* `{"status": "UP", "model_loaded": true, "model_version": "1.0.0"}`
3. **Interactive Swagger Documentation:**  
   Navigate to: `http://localhost:8080/swagger-ui/index.html`
4. **Prahar Web User Interface:**  
   Navigate to: `http://localhost:5173`

---

## 4. Safe Demonstration Script (Step-by-Step)

### Demo Act 1: Scanning a Benign Official Website
1. Open `http://localhost:5173` in your browser.
2. Click on the **Scanner** tab in the top navigation bar.
3. In the URL input field, enter a legitimate banking portal:
   `https://retail.onlinesbi.sbi/retail/login.htm`
4. Click **Analyze Threat**.
5. **Observed Result:**
   - Classification: **SAFE** (Green badge).
   - Risk Score: **0 / 100**.
   - Explainability: Shows that the domain is verified on the official banking whitelist.

---

### Demo Act 2: Scanning a Safe Synthetic Phishing URL
1. In the Scanner URL field, enter the following synthetic lure fixture:
   `http://secure-login-hdfc-netbanking.tk/verify-account`
2. Click **Analyze Threat**.
3. **Observed Result:**
   - Classification: **HIGH RISK** (Red alert modal).
   - Risk Score: **$\ge 85$ / 100**.
   - Threat Category: `BANKING_PHISHING` / `CREDENTIAL_THEFT`.
   - Indicators Flagged:
     - *Free Suspicious TLD:* `.tk` flagged as high-risk free domain.
     - *Brand Impersonation:* `hdfc-netbanking` detected in non-official domain.
     - *Suspicious Subdomain / Path:* `verify-account` credential harvesting lure.
     - *Insecure Protocol:* Plain HTTP used for banking interface.
   - Explainability Card: Highlights why the score was assigned and displays contextual immediate safety guidance.

---

### Demo Act 3: Scanning an Internationalized Homoglyph (Punycode Attack)
1. Enter the following synthetic Cyrillic homoglyph test URL:
   `https://раураl.com/account/login` *(uses Cyrillic 'р' and 'а')*
2. Click **Analyze Threat**.
3. **Observed Result:**
   - Handled cleanly in **<10 ms** without server crash.
   - Automatically decoded via `java.net.IDN.toASCII()` into Punycode `xn--80aa9a5ad.com`.
   - Triggered `RULE_PUNYCODE` (High Risk / Suspicious homoglyph warning).

---

### Demo Act 4: Multi-Channel QR Code Scanning (Quishing)
1. In the Scanner view, select the **QR Code** tab.
2. Upload the synthetic test QR fixture located at:
   `media/demo_fixtures/sample_upi_qr.png`
3. Click **Scan QR**.
4. **Observed Result:**
   - ZXing optical decoder extracts the embedded UPI deep link (`upi://pay?pa=merchant@upi...`).
   - Flags the payload category as `UPI_DEEP_LINK` and validates the merchant ID.

---

### Demo Act 5: Citizen Emergency Response & Automated Complaint Drafting
1. Click on the **Safety Centre** tab in the navigation bar.
2. Select **Report an Unauthorized Transaction / 1930 Wizard**.
3. Enter test details:
   - *Amount:* `15,000 INR`
   - *Fraud Type:* `UPI / OTP Scam`
   - *Bank:* `State Bank of India`
   - *Transaction ID:* `UPI/2026/9823198`
4. Click **Generate Formal Cybercrime Complaint Draft**.
5. **Observed Result:**
   - Instantly compiles a structured legal complaint adhering to National Cybercrime Reporting Portal (`cybercrime.gov.in`) format.
   - Highlights the 1930 toll-free helpline number and provides an immediate step-by-step containment checklist (Freezing SIM, blocking debit card, locking netbanking).

---

### Demo Act 6: Offline AI Cybersecurity Assistant
1. Click on the floating **Prahar Chatbot** icon at the bottom right.
2. Type: `"Someone called asking for my netbanking OTP to claim a prize. What should I do?"`
3. **Observed Result:**
   - Instant response in **<1 ms** generated by Prahar's local deterministic intent engine (`NlpEngine.java`).
   - Warns never to disclose OTPs, identifies lottery lure techniques, and cites official bank security practices.

---

## 5. Troubleshooting Reference

| Symptom | Probable Cause | Quick Resolution |
| :--- | :--- | :--- |
| `ML Service Offline` warning in modal | FastAPI service is not running on port 8000 | In `ml-service/`, run `python -m uvicorn app.main:app --port 8000`. The backend degrades gracefully to heuristic rules in the meantime. |
| `Port 8080 already in use` | Previous Spring Boot instance still running | Run `Get-Process -Name java \| Stop-Process` in PowerShell, then re-run `mvn spring-boot:run`. |
| Blank frontend page | Node dependencies missing | In `frontend/`, run `npm install`, then `npm run dev`. |

---

## 6. Safe Shutdown & Cleanup

When the examination or demonstration concludes:
1. In the frontend terminal window, press `Ctrl + C`.
2. In the backend terminal window, press `Ctrl + C`.
3. In the ML service terminal window, press `Ctrl + C`.
4. If started via `start_services.ps1`, simply close the spawned terminal windows.
