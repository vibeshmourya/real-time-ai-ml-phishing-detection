# Prahar 2.0 — Academic Project Overview

## 1. Project Title & Metadata
- **Project Name:** Prahar — Real-Time Multi-Channel AI/ML Phishing Detection and Incident Response Platform
- **Academic Context:** Master of Computer Applications (MCA) Final Project
- **Domain:** Application Security, Machine Learning, Full-Stack Web Systems, Cyber Defense
- **Version:** 2.0.0 (Release Candidate)

---

## 2. Problem Statement
Cyber fraud and phishing in India and globally have evolved from basic email spoofs into sophisticated, multi-channel attacks:
1. **Multi-Vector Threat Delivery:** Attackers distribute malicious payloads across SMS (Smishing), WhatsApp/Telegram, QR codes (Quishing) pasted at merchant checkout counters, and fake banking portals impersonating KYC/Electricity bill updates.
2. **Victim Confusion & Panic:** When a victim suffers an unauthorized financial debit, they typically experience the critical **"Golden Hour" (first 2–3 hours)** without knowing whom to contact or what legal safeguards protect them.
3. **Fragile Detection Tools:** Existing consumer anti-phishing extensions rely on static blacklists that fail against fast-flux zero-hour phishing domains registered minutes prior to the campaign.
4. **Data Privacy Hazards:** Many online scanners upload private corporate or personal screenshots to third-party public indexing services, inadvertently leaking PII, OTPs, and sensitive financial records.

---

## 3. Project Objectives
Prahar was engineered to solve these challenges through four primary technical objectives:

1. **Multi-Channel Pre-Execution Inspection:**
   Analyze URLs, SMS texts, raw email bodies, decoded QR codes, and uploaded screenshot images before the user executes a transaction or inputs credentials.
2. **Hybrid Detection Engine (Lexical Heuristics + Machine Learning):**
   Combine deterministic structural security rules (30+ indicators: punycode homographs, IP-based URLs, high-entropy subdomains, suspicious TLDs) with machine learning inference (FastAPI + Scikit-learn/XGBoost).
3. **Rapid Incident Containment & Indian Cyber Protocol Integration:**
   Equip victims with actionable emergency workflows: direct one-click guidance to the **National Cyber Crime Reporting Portal ([cybercrime.gov.in](https://cybercrime.gov.in/))**, the **1930 National Helpline**, and an automated legal dispute letter generator citing the **RBI Zero-Liability Circular** (`RBI/2017-18/15`) and **IT Act 2000** (`Sections 43A, 66C, 66D`).
4. **Privacy-by-Design & Zero-Leakage Architecture:**
   Enforce server-side SSRF firewall guards to protect internal networks, implement automated PII/banking data redaction (`SensitiveDataScrubber`), and ensure zero client-side credential exposure.

---

## 4. System Scope & Boundaries

### What Prahar Implements:
- ✅ Multi-vector threat scanner (URL, SMS, Email, QR Code via ZXing, Screenshot analysis).
- ✅ Machine Learning prediction microservice with 30-feature lexical extraction and model governance gates.
- ✅ Conversational Safety Assistant with an offline 22-intent rule engine and optional Google Gemini proxying.
- ✅ Personal Money & Transaction Monitor with CSV bank statement parsing, SHA-256 deduplication, and debit outlier anomaly detection.
- ✅ Fraud Incident Response Centre with lifecycle tracking and dispute letter generation.
- ✅ Real-time persistent notifications powered by Server-Sent Events (SSE).
- ✅ Google Chrome browser extension (Manifest V3) for active tab threat inspection.

### What Prahar Deliberately Excludes:
- ❌ **No Live Bank Account Access:** Prahar does not ask for or connect to live net banking credentials or core banking APIs; financial transactions are imported via user-uploaded CSVs or pasted SMS alerts.
- ❌ **No Direct SIM / Telephony Control:** Device safety provides verified carrier suspension hotlines and CEIR links rather than claiming unauthorized remote telco API controls.
- ❌ **No Automated Malicious Payload Execution:** Scanning is performed statically and heuristically; Prahar does not run a dynamic malware detonation sandbox.

---

## 5. Technology Stack Summary

| Tier | Technologies |
| :--- | :--- |
| **Frontend Web App** | React 18, Vite 5, Tailwind CSS, Lucide Icons, Axios, EventSource (SSE) |
| **Backend REST API** | Java 17/21/25, Spring Boot 3.3.4, Spring Security, JWT (JJWT), Spring Data JPA, Hibernate, ZXing 3.5.3, Tesseract OCR |
| **ML Microservice** | Python 3.11, FastAPI 0.111.0, Uvicorn, Scikit-learn 1.5, XGBoost, Pandas, NumPy, Joblib |
| **Database** | MySQL 8.0 (Production) / In-Memory H2 (Local Development Profile) |
| **Browser Extension** | Chrome Extension Manifest V3, Vanilla JavaScript, Chrome Storage API |
| **Edge & Ingress** | Nginx 1.27 Alpine (Reverse Proxy & TLS Termination) |
| **Containerization** | Docker, Docker Compose (Multi-stage builds, non-root users) |
