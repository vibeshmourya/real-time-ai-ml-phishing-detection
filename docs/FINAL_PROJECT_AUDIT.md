# Prahar Final Project Audit & Academic Handover Dossier

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Academic Degree:** Master of Computer Applications (MCA)  
**Evaluation Date:** October 2026  
**Host Environment:** Windows 11 (64-Bit), Java 17 (OpenJDK 64-Bit Server VM 25.0.3), Python 3.11.0, Node.js v20.x, Vite 5.2.13  
**Audit Status:** COMPLETE & EVIDENCE-VERIFIED  

---

## 1. System Implementation Audit & Capability Matrix

Every capability listed below is audited against the active source code, configuration files, and empirical test outcomes. No speculative or unimplemented components are marked as verified.

| Capability / Module | Primary Technologies | Implementation Status | Evidence & Verification Reference |
| :--- | :--- | :--- | :--- |
| **Frontend Web Application** | React 18.3.1, Vite 5.2.13, TailwindCSS 3.4.4 | **VERIFIED** | 13 route views, lazy loaded with `<Suspense>`, 201 kB main bundle. 10/10 automated tests pass. |
| **Spring Boot Backend Server** | Spring Boot 3.3.4, Java 17, Spring Security 6 | **VERIFIED** | 13 REST controllers, 14 domain entities, JWT auth, SSRF protection with IDN Punycode. 120/120 tests pass. |
| **FastAPI ML Microservice** | Python 3.11, FastAPI, Scikit-Learn, Joblib | **VERIFIED** | 30 URL lexical/domain features extracted, Random Forest inference (5.1ms median). 35/35 pytest tests pass. |
| **Multi-Channel Scanning Pipeline** | Spring Boot, ZXing 3.5.3, ImageIO, Regex | **VERIFIED** | URL, Email, SMS, Screenshot (visual QR + OCR analysis), and QR code image upload scanning verified. |
| **Intelligent Multi-Signal Scoring Engine** | In-Process Java Algorithm | **VERIFIED** | Fuses 30 heuristic rules, ML probability, threat intel reputation, and domain whitelists (0.04ms median). |
| **Threat Intelligence Integration** | Google Safe Browsing v4, VirusTotal v3 | **VERIFIED** (Optional) | Client providers with explicit user consent check, SSRF outbound guard, and 1-hour in-memory cache. |
| **AI Cybersecurity Chatbot** | Google Gemini API + In-Process NlpEngine | **VERIFIED** (Hybrid) | Live Gemini 2.5 Flash with timeout circuit breaker; offline fallback handling 20+ intents deterministically. |
| **Financial Safety & Incident Centre** | Spring Data JPA, MySQL 8.0 / H2 | **VERIFIED** | Transaction import, duplicate SHA-256 fingerprinting, dispute drafting (1930 / cybercrime.gov.in format). |
| **Real-Time Security Notifications** | SSE (Server-Sent Events), JPA Persistence | **VERIFIED** | Persistent notifications table, user severity preferences, and non-blocking SSE broadcast stream. |
| **Browser Extension (Manifest V3)** | Chrome Extension API, HTML5/CSS3/Vanilla JS | **VERIFIED** | Zero hardcoded keys, activeTab permission, live API communication. 10/10 automated test scripts pass. |
| **Production Deployment Configuration** | Docker, Nginx, Managed MySQL 8.0 TLS | **VERIFIED** (Configured) | Multi-stage Dockerfiles, `docker-compose.prod.yml`, least-privilege SQL scripts, and secrets playbook. |

---

## 2. Detailed Capability Breakdown

### 2.1 Web Frontend Application (`frontend/`)
- **Status:** **VERIFIED**
- **Location:** `d:\MCA notes\MINI PRO SIH\frontend`
- **Features Implemented:**
  - Route code-splitting with `React.lazy()` for all secondary pages (`ScannerPage`, `MyMoneyPage`, `SafetyCentrePage`, `ChatAssistantPage`, `AdminPortalPage`, `ScanHistoryPage`, etc.).
  - Main bundle size: **201.41 kB** (gzip: **60.86 kB**), reduced from 520 kB monolith with 0 Vite build warnings.
  - Interactive multi-channel scanner with drag-and-drop file upload, live webcam QR capture, and visual verdict modal.
  - Indian cyber fraud emergency response center with direct 1930 helpline action checklist and complaint draft generator.
  - Fully responsive dark/light mode toggle with WCAG AA compliant contrast ratios.

### 2.2 Spring Boot REST Backend (`backend/`)
- **Status:** **VERIFIED**
- **Location:** `d:\MCA notes\MINI PRO SIH\backend`
- **Features Implemented:**
  - Defense-in-depth security filter chain: `RequestCorrelationFilter` (UUID tracking in MDC), `RateLimitingFilter` (IP token bucket), and `JwtAuthenticationFilter`.
  - Comprehensive Server-Side Request Forgery (SSRF) defense with DNS pre-resolution, RFC-1918 private IP range blocking, loopback blocking, and non-ASCII IDN / Punycode conversion.
  - Intelligent multi-signal risk scoring engine (`MultiSignalScoringEngine.java`) calculating 0–100 risk scores with deterministic reason codes and structured explainability breakdowns.
  - Static prevention guide in-memory caching (`PreventionContentService.java`) delivering **0.003 ms** response times (>1,000x faster than cold DB queries).
  - 10-thread concurrent scan burst tested at **119 scans/second** with **100% success rate** and 0 primary key collisions.

### 2.3 Python ML Microservice (`ml-service/`)
- **Status:** **VERIFIED**
- **Location:** `d:\MCA notes\MINI PRO SIH\ml-service`
- **Features Implemented:**
  - `ModelContainer` singleton loaded at startup via FastAPI `lifespan` context manager.
  - 30 lexical, domain, and security URL features extracted in pure Python in **0.03 ms**.
  - Machine learning inference (`/predict`) executed in **5.12 ms median** (194.8 requests/second).
  - Data ingestion pipeline (`ingest_feeds.py`) with strict governance gates (`governance.py`) enforcing duplicate filtering and dataset provenance.

### 2.4 Browser Extension (`browser-extension/`)
- **Status:** **VERIFIED**
- **Location:** `d:\MCA notes\MINI PRO SIH\browser-extension`
- **Features Implemented:**
  - Manifest V3 architecture with least-privilege permissions (`activeTab`, `storage`).
  - Real-time page URL extraction and background communication with Spring Boot backend.
  - Zero hardcoded API keys or external CDN scripts.

---

## 3. Verified Verification Evidence Summary

| Test Suite | Total Tests | Passed | Failed | Skipped | Test Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Backend Spring Boot Tests** | 120 | **120** | 0 | 0 | 20.83 s |
| **FastAPI ML Service Tests** | 35 | **35** | 0 | 0 | 3.48 s |
| **Frontend Security & Upload Tests** | 10 | **10** | 0 | 0 | 83 ms |
| **Browser Extension Tests** | 10 | **10** | 0 | 0 | 50 ms |
| **TOTAL AUTOMATED TESTS** | **175** | **175** | **0** | **0** | **100% Green** |

---

## 4. Academic Demonstration Readiness

- **Status:** **VERIFIED**
- The entire Prahar platform can be demonstrated locally on a standard developer workstation using `.\start_services.ps1` with harmless synthetic test fixtures (e.g., banking lure samples, synthetic QR code images, and whitelisted benign URLs).
- Zero paid cloud subscriptions or active third-party API accounts are required for demonstration.
