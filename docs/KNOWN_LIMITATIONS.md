# Prahar 2.0 — Known Limitations & Architectural Boundaries

In accordance with rigorous academic and scientific standards, this document details the verified operational constraints, probabilistic boundaries, and known limitations of the Prahar platform.

---

## 1. Probabilistic Nature of Machine Learning Detection

### A. Non-Zero False Positive & False Negative Rates
- **Principle:** Machine learning inference is probabilistic. A prediction score reflects statistical similarity to historical phishing patterns, not an absolute guarantee.
- **Evidentiary Limitation:** Attackers frequently leverage legitimate enterprise infrastructure (e.g., Google Forms, Microsoft SharePoint, Canva, Notion) to host credential harvesters. Because the hostname belongs to a highly trusted domain (`docs.google.com`), lexical URL features alone will not flag the domain as malicious.
- **Architectural Safeguard:** Prahar combines ML classification with structural heuristic checks, brand impersonation detection, and visual urgency keywords to mitigate blind spots.

---

## 2. Machine Learning Dataset Baseline

### A. 177-Sample Baseline Dataset
- **Status:** The baseline model bundled with the prototype repository was trained on a curated 177-sample dataset (`ml-service/data/raw/phishing_dataset.csv`).
- **Empirical Performance:** On this baseline evaluation, the model achieved an accuracy of ~56% and a phishing recall of 0.40 when evaluated across diverse edge cases.
- **Why It Does Not Break the System:** Prahar's backend does not rely solely on the ML model. The Spring Boot backend runs an integrated **30-rule lexical heuristic engine** that acts as an independent safety net.
- **Phase 25 Governance Pipeline:** A dedicated data ingestion and governance pipeline (`ml-service/data_pipeline/`) was introduced to ingest authorized external feeds (PhishTank, OpenPhish, Tranco) with hard negatives and false-positive rate (FPR) gates. However, in accordance with safety policies, new models require human review before replacing the baseline.

---

## 3. Optical Character Recognition (OCR) Engine Dependency

### A. Tesseract Native Binary Dependency
- **Status:** The backend integrates with Tesseract OCR via native process invocation.
- **Behavior:**
  - If the `tesseract` binary is installed and present in the host system's `PATH`, high-accuracy text extraction proceeds automatically.
  - If the `tesseract` binary is absent (common on unconfigured Windows/Mac environments), the service **honestly reports `ocrStatus: "UNEXTRACTED"` without fabricating text or crashing**.
- **User Impact:** In the absence of Tesseract, QR code decoding (ZXing) and image magic byte validation continue to function with 100% accuracy.

---

## 4. External Cloud Services & AI Assistant

### A. Google Gemini Generative AI
- **Status:** Google Gemini integration is designed as an optional server-side proxy.
- **Operational Reality:**
  - Automated tests mock this integration to avoid consuming paid API quotas or relying on external network connectivity.
  - Live external chat queries require the operator to supply a valid API key (`AI_API_KEY`) at startup.
  - When no key is provided, the platform automatically falls back to its internal, deterministic 22-intent rule-based safety engine.

### B. Threat Intelligence Feeds (SafeBrowsing & VirusTotal)
- **Status:** Disabled by default (`THREAT_INTEL_ENABLED=false`).
- **Privacy Consideration:** Querying external public threat feeds requires sending the target URL to third-party servers. Prahar enforces user consent before enabling external lookups.

---

## 5. Fraud Incident Response & Legal Automation Scope

### A. Indian Cybercrime Portal (1930 / cybercrime.gov.in)
- **Status:** Prahar provides immediate guidance, hotline numbers, and formal dispute drafts.
- **Boundary:** Prahar **cannot automatically file the official police complaint (FIR)** on the government portal. The National Cyber Crime Reporting Portal mandates individual OTP verification and CAPTCHA validation. The victim must upload Prahar's generated dispute letter to the portal or present it to their bank branch manager.

---

## 6. Personal Finance Monitor Scope

### A. Zero Live Bank API Integration
- **Status:** Prahar does not connect to live core banking APIs (such as NetBanking or UPI payment gateways).
- **Security Justification:** Requesting user bank login credentials or banking API tokens would violate security principles and create unacceptable liability. Prahar monitors finances exclusively through user-imported bank statement CSV files and pasted SMS alert texts.
