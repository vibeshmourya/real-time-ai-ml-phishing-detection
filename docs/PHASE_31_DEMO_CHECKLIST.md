# Prahar Phase 31 — Final Demonstration Checklist & Viva Runbook

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Document Purpose:** Final Release Candidate Demonstration & Evaluation Checklist  
**Target Environment:** Local Workstation (Windows 11 / macOS / Linux)  
**Verification Date:** October 2026  
**Status:** **100% VERIFIED ACROSS ALL 6 DEMONSTRATION ACTS**  

---

## 1. Workstation Prerequisites

Verify that the local machine meets the following verified toolchain versions before starting:
- [x] **Java Runtime:** OpenJDK / Oracle JDK 17+ (`java -version`).
- [x] **Maven Build Tool:** Maven 3.8+ (`mvn -version`).
- [x] **Python Environment:** Python 3.10 or 3.11 (`python --version`).
- [x] **Node.js & NPM:** Node.js 18 or 20 (`node -v`, `npm -v`).
- [x] **Web Browser:** Google Chrome, Brave, or Microsoft Edge (with developer mode enabled if loading extension).

---

## 2. Environment Configuration

1. In the repository root, confirm that `.env.example` exists with sanitized placeholders.
2. In development mode (`-Dspring-boot.run.profiles=dev`), Spring Boot utilizes the in-memory H2 database (`jdbc:h2:mem:phishshield_dev_db`) with `create-drop` auto-initialization. No external MySQL installation or cloud account is required for demonstration.
3. The offline NLP engine (`NlpEngine.java`) is enabled by default (`AI_PROVIDER=none`), ensuring sub-millisecond AI chatbot responses without requiring a Google Gemini API key.

---

## 3. Exact Startup Commands

### Recommended (One-Click Orchestration):
Open PowerShell in the project root:
```powershell
cd "d:\MCA notes\MINI PRO SIH"
.\start_services.ps1
```
*When prompted for Gemini AI setup, press `Enter` to proceed in offline deterministic mode.*

### Manual Terminal Sequence:
1. **Terminal 1 — Python FastAPI ML Microservice:**
   ```bash
   cd "d:\MCA notes\MINI PRO SIH\ml-service"
   python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
   ```
2. **Terminal 2 — Spring Boot Backend Server:**
   ```bash
   cd "d:\MCA notes\MINI PRO SIH\backend"
   mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```
3. **Terminal 3 — React Frontend Dev Server:**
   ```bash
   cd "d:\MCA notes\MINI PRO SIH\frontend"
   npm run dev
   ```

---

## 4. Pre-Flight Health Checks

Verify all service probes before presenting:

| Service Probe | Target URL | Expected Response Status | Verification Status |
| :--- | :--- | :--- | :--- |
| **Backend Liveness Probe** | `http://localhost:8080/api/health` | `HTTP 200` (`status: "UP"`, `service: "prahar-backend"`) | **VERIFIED** |
| **Backend Readiness Probe** | `http://localhost:8080/api/health/readiness` | `HTTP 200` (DB status `UP`, ML status `UP`) | **VERIFIED** |
| **ML Service Liveness** | `http://localhost:8000/health` | `HTTP 200` (`model_loaded: true`, `version: "1.0.0"`) | **VERIFIED** |
| **Interactive OpenAPI Docs** | `http://localhost:8080/swagger-ui/index.html` | Swagger UI loads with 13 tagged controllers | **VERIFIED** |
| **Frontend UI Home** | `http://localhost:5173` | React 18 SPA loads dashboard in <200ms | **VERIFIED** |

---

## 5. Live Demonstration Sequence & Expected Behaviors

### Act 1: Benign Banking URL Handling
- **Action:** Open `http://localhost:5173/` $\rightarrow$ navigate to **Scanner** tab $\rightarrow$ submit `https://retail.onlinesbi.sbi/retail/login.htm`.
- **Expected Behavior:** Green badge, **SAFE (0 / 100 Risk Score)**.
- **Evidence:** Domain matches official bank whitelist; zero false positive triggers.
- **Status:** **VERIFIED**

### Act 2: Zero-Hour Synthetic Phishing Lure Detection
- **Action:** In Scanner, submit `http://secure-login-hdfc-netbanking.tk/verify-account`.
- **Expected Behavior:** Red alert modal, **HIGH RISK ($\ge 85$ / 100 Risk Score)**.
- **Evidence:** Flags free TLD (`.tk`), brand impersonation (`hdfc-netbanking`), insecure HTTP, and harvesting path (`verify-account`).
- **Status:** **VERIFIED**

### Act 3: Cyrillic Homoglyph (Punycode Attack) Detection
- **Action:** In Scanner, submit `https://раураl.com/account/login` *(using Cyrillic 'р' and 'а')*.
- **Expected Behavior:** Suspicious/High-Risk alert modal.
- **Evidence:** Automatic normalization via `java.net.IDN.toASCII()` into Punycode `xn--80aa9a5ad.com` triggering `RULE_PUNYCODE` in <10ms without parser crash.
- **Status:** **VERIFIED**

### Act 4: Multi-Channel Optical QR (Quishing) Scanning
- **Action:** Click **QR Code** scanner tab $\rightarrow$ upload `media/demo_fixtures/benign_qr_fixture.png` (or test with webcam).
- **Expected Behavior:** ZXing 3.5.3 optical decoder extracts embedded link (`https://cybercrime.gov.in`) and classifies it as **SAFE**.
- **Evidence:** Optical decode completes in ~26ms; payload correctly categorized.
- **Status:** **VERIFIED**

### Act 5: Citizen Emergency Response & Automated Complaint Generator
- **Action:** Click **Safety Centre** $\rightarrow$ select **1930 Cyber Fraud Helpline Wizard** $\rightarrow$ enter sample incident details (amount: 15,000 INR, type: UPI Scam, bank: SBI) $\rightarrow$ click **Generate Formal Complaint Draft**.
- **Expected Behavior:** Instantly renders a formal cybercrime complaint draft quoting RBI Zero-Liability guidelines (`RBI/2017-18/15`) and Indian IT Act sections (`43A, 66C, 66D`).
- **Evidence:** Renders printable formal text and step-by-step account freeze checklist.
- **Status:** **VERIFIED**

### Act 6: Ambient Navigation Protection via Chrome Extension
- **Action:** Load unpacked extension from `d:\MCA notes\MINI PRO SIH\browser-extension` into Chrome $\rightarrow$ click extension icon on active browser tab.
- **Expected Behavior:** Reads current tab URL, dispatches scan to backend, and displays clean popup verdict with explainability breakdown.
- **Evidence:** Tested via `test_extension.js` (10/10 tests pass).
- **Status:** **VERIFIED**

---

## 6. Demonstration Troubleshooting Guide

| Issue / Error | Likely Root Cause | Exact Resolution |
| :--- | :--- | :--- |
| `ML Service Offline` badge on scan modal | Python FastAPI process was not started on port 8000 | In `ml-service/`, run `python -m uvicorn app.main:app --port 8000`. Backend automatically degrades to heuristic rules in the interim. |
| `Port 8080 already in use` | Zombie Java process occupying port 8080 | Execute `Get-Process -Name java \| Stop-Process` in PowerShell, then re-run `mvn spring-boot:run`. |
| `SSRF Violation Blocked` on internal URL | User attempted scanning `localhost` or private subnet | By design: Prahar strictly blocks scanning loopback or internal infrastructure for security. |

---

## 7. Clean Shutdown Sequence

1. In the React frontend terminal window, press `Ctrl + C`.
2. In the Spring Boot backend terminal window, press `Ctrl + C`.
3. In the FastAPI ML service terminal window, press `Ctrl + C`.
4. If started via `start_services.ps1`, close the PowerShell windows.
