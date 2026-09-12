# Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%2F%2021%20%2F%2025-orange.svg)](https://www.oracle.com/java/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.111.0-teal.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18.x-blue.svg)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-5.x-purple.svg)](https://vitejs.dev/)
[![TailwindCSS](https://img.shields.io/badge/TailwindCSS-3.4-38bdf8.svg)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Prahar** (*Detect. Understand. Stay Safe.*) is a clean, public-facing cybersecurity safety platform engineered to help digital citizens, students, employees, and senior citizens detect phishing and social-engineering threats in real time, understand why something is suspicious, and take immediate containment actions.

---

## 🛡️ Core Capabilities

1. **Multi-Channel Threat Scanner**: Inspect suspicious URLs, SMS text, WhatsApp/Telegram messages, raw email bodies, screenshot OCR images, and decoded QR codes.
2. **Explainable AI (XAI)**: Synthesizes machine learning probabilities and rule-based heuristics into plain-English reasons ("Why Prahar flagged it").
3. **Structured Risk Meter**: Clear 0–100 risk score and verdict levels (Safe, Suspicious, High Risk).
4. **"I Already Clicked" Emergency Response**: Interactive containment questionnaire guiding users through immediate password resets, bank freezes, and official helpline 1930 reporting.
5. **Prahar Learn**: Cyber-safety education hub with warning signs, interactive quizzes, audio narration, and prevention steps across 9 key scam categories.
6. **Community Threat Reporting**: Honest reporting workflow with reference IDs for community protection.
7. **Multilingual & Simple Mode**: English, Hindi, and Kannada localization with an accessibility mode for seniors.
8. **Prahar Web Protection Browser Extension**: Manifest V3 Chrome extension with real-time tab checking and in-page warning banners.
9. **Admin Security Center**: Protected portal for report triage, audit logs, and empirical model evaluation metrics.

---

## 📐 Architecture Overview

```text
                        PRAHAR
             Real-Time Phishing Protection
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
      CHECK            LEARN            HELP
   (Scanner)       (Prahar Learn)   (Emergency)
        │
        ▼
   Multi-Channel Extraction (URL / SMS / Email / Image / QR)
        │
        ▼
   Spring Boot 3 API Layer  <--->  FastAPI ML Microservice
   (SSRF Guard & Rules)            (30 Lexical Features & XGBoost)
        │
        ▼
   Explainable Verdict & Actionable Guidance
```

---

## 📂 Repository Organization

```text
prahar/
├── frontend/                     # React.js (Vite + TailwindCSS) public safety UI
│   ├── src/
│   │   ├── components/           # Navbar, QuickScanner, ScanVerdictModal, etc.
│   │   ├── pages/                # Dashboard, ScanHistory, Learn, Emergency, Admin
│   │   ├── context/              # AuthContext, ScanContext, LanguageContext, ThemeContext
│   │   └── locales/              # en.json, hi.json, kn.json
├── backend/                      # Spring Boot 3.3.4 REST API Server
│   ├── src/main/java/com/phishshield/
│   │   ├── controller/           # URLScanController, AuthController, ReportController
│   │   ├── service/              # RuleEngineService, MLServiceClient, QRCodeService
│   │   ├── domain/               # JPA Entities
│   │   └── security/             # SSRFProtectionService, JWT Auth
├── ml-service/                   # Python FastAPI Machine Learning Microservice
│   ├── app/
│   │   ├── api/routes.py         # /predict, /ocr, /qr, /model/evaluations
│   │   ├── ml/                   # ModelPredictor, Explainer
│   │   └── services/             # PredictionService, OCRService
│   ├── models/                   # XGBoost / Logistic Regression artifacts
│   └── tests/                    # 27 Pytest automated test suites
├── browser-extension/            # Prahar Web Protection Manifest V3 extension
│   ├── manifest.json
│   └── src/                      # popup.html, popup.js, background.js, content.js
├── database/                     # MySQL schema.sql & seed.sql scripts
└── evaluation/                   # Real benchmark evaluation JSON metrics
```

---

## 🚀 Running the System

### 1. Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Available at: `http://localhost:5173`

### 2. Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- API Server: `http://localhost:8080`
- Swagger UI Docs: `http://localhost:8080/swagger-ui/index.html`

### 3. ML Service (FastAPI)
```bash
cd ml-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
- ML Endpoint: `http://localhost:8000`

### 4. Browser Extension
1. Open `chrome://extensions` in Google Chrome.
2. Enable **Developer mode**.
3. Click **Load unpacked** and select the `browser-extension` folder.

---

## 🔒 Test Accounts & Personas

| Persona | Username / Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` | `Admin@123456` | Full Security Center, triage queue & model evaluations |
| **Security Analyst** | `analyst` | `Analyst@123456` | Threat report triage & evaluation metrics |
| **Citizen User** | `demo_user` | `User@123456` | Personal scan history, education & reporting |

---

## 🧪 Testing Verification

- **Frontend**: `cd frontend && npm run build`
- **Backend**: `cd backend && mvn test`
- **ML Service**: `cd ml-service && python -m pytest`

---

## 📄 License
Licensed under the MIT License.
