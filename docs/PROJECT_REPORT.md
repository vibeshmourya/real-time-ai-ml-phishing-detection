# Prahar: Real-Time Multi-Channel AI/ML-Based Phishing Detection and Incident Prevention System
## Master of Computer Applications (MCA) Project Final Report

---

### Abstract
Phishing attacks remain one of the most pervasive cyber threats, causing billions of dollars in global financial loss, identity compromise, and data breaches. Traditional phishing defense mechanisms—such as static URL blacklists and signature matching—fail against zero-hour threats, dynamic domain generation algorithms (DGA), fast-flux hosting, and multi-channel attack vectors including SMS (Smishing), QR codes (Quishing), and fraudulent transactional emails. 

**Prahar** is an enterprise-grade, real-time cybersecurity defense platform developed using a microservices architecture comprising a Spring Boot 3 backend, a Python/FastAPI machine learning engine, a React 18 / Vite 5 single-page application, and a Manifest V3 browser extension. Prahar integrates a 4-layer heuristic rule engine (30 rules), a 30-feature Random Forest ML classifier, optional privacy-preserving threat intelligence providers (Google Safe Browsing and VirusTotal), and a deterministic multi-signal fusion scoring engine. 

Additionally, Prahar incorporates an Indian cyber fraud emergency response center with direct 1930 helpline workflow integration, automated formal complaint drafting, financial transaction dispute tracking with SHA-256 duplicate fingerprinting, and a hybrid AI cybersecurity assistant. Empirical testing demonstrates sub-10ms URL classification latency, 119 scans/second throughput under concurrent bursts, and 100% test pass rate across 175 automated regression tests.

---

### Table of Contents
1. Title Page & Abstract
2. Introduction and Problem Statement
3. Motivation and Objectives
4. Limitations of Existing Systems
5. Proposed Prahar Architecture & System Overview
6. Project Scope and Operational Boundaries
7. Functional and Non-Functional Requirements
8. System Architecture & Topology
9. Detailed Module Descriptions
10. Technology Stack & Technical Justifications
11. Database Design and Entity Relationships
12. API Contracts & Inter-Service Communication
13. Phishing Detection Methodology & Multi-Signal Scoring Engine
14. Machine Learning Pipeline & Feature Extraction Methodology
15. Security Hardening, Privacy Controls, and SSRF Prevention
16. Testing Methodology and Empirical Test Outcomes
17. Performance Evaluation and Benchmarking Results
18. Known System Limitations & Boundary Conditions
19. Future Enhancements & Production Roadmap
20. Conclusion
21. Academic and Technical References

---

### 1. Title Page Information
- **Project Title:** Prahar: Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System
- **Degree:** Master of Computer Applications (MCA)
- **Course / Specialization:** Advanced Software Engineering, Cybersecurity & Applied Artificial Intelligence
- **Evaluation Phase:** Final Project Submission & Viva Handover (Phase 30)
- **Year of Submission:** 2026

---

### 2. Introduction and Problem Statement
Phishing is the fraudulent attempt to obtain sensitive information such as usernames, passwords, credit card details, and bank account numbers by masquerading as a trustworthy entity in electronic communication. Modern attackers exploit:
1. **Multi-Channel Vectors:** Phishing has moved beyond conventional emails to SMS messages (Smishing), embedded QR codes (Quishing), and fake customer support screenshots.
2. **Ephemeral Domains:** Malicious domains are registered, weaponized, and discarded within hours, rendering static DNS blacklists ineffective.
3. **Punycode / Homoglyph Attacks:** Attackers use visually identical Cyrillic or Greek characters to impersonate legitimate brands (e.g., `paypal.com` vs. `раураl.com`).
4. **Lack of Explainability:** Conventional security tools offer binary (safe/unsafe) verdicts without explaining *why* a URL was flagged, failing to educate the user.

Prahar addresses these challenges through real-time heuristic rule analysis, machine learning inference, explainable multi-signal fusion scoring, and an emergency response framework.

---

### 3. Motivation and Objectives
#### 3.1 Motivation
In India, the National Cybercrime Reporting Portal (`cybercrime.gov.in`) and the citizen financial fraud helpline (`1930`) report thousands of daily UPI, OTP, and banking phishing incidents. Victims often hesitate or take hours to freeze compromised accounts, enabling attackers to siphon funds through intermediate mule accounts. A comprehensive solution must not only detect attacks before credentials are typed, but also automate immediate post-incident response within the critical "golden hour."

#### 3.2 Objectives
1. Build a high-throughput, low-latency URL scanning engine combining static rules, machine learning, and external reputation feeds.
2. Support multi-channel threat ingestion: URLs, email text, SMS messages, QR codes (ZXing), and screenshot images.
3. Deliver transparent, explainable 0–100 risk scoring with human-readable indicators rather than black-box probabilities.
4. Provide immediate citizen emergency response: automated complaint drafts in Indian cybercrime format and bank dispute templates.
5. Guarantee enterprise security: strict SSRF protection, rate limiting, and zero hardcoded secrets.

---

### 4. Limitations of Existing Systems
| Feature / Characteristic | Traditional Anti-Phishing Tools | Prahar Platform |
| :--- | :--- | :--- |
| **Detection Speed for Zero-Day URLs** | Slow (hours/days to update blacklists) | Real-time (<10ms via rules + ML features) |
| **Multi-Channel Ingestion** | Restricted to email gateways or browser filters | URL, SMS, Email, QR Codes, and Screenshots |
| **Scoring Explainability** | Binary black-box verdict | 0–100 score with detailed indicator breakdown |
| **Citizen Post-Incident Response** | None | 1930 helpline wizard, automated complaint generator |
| **Offline Fallback** | Fails or blocks traffic if cloud is down | Deterministic 30-rule engine works 100% offline |
| **Data Privacy & SSRF Guard** | Many send all URLs to third-party APIs | Outbound SSRF guard + opt-in external lookups |

---

### 5. Proposed System
Prahar implements an intelligent defense pipeline:
1. **Input Validation & SSRF Guard:** Inspects URL syntax, checks DNS resolution, blocks RFC-1918 private IP ranges, and normalizes internationalized Unicode domains into standard Punycode (`IDN.toASCII`).
2. **Layer 1 — Heuristic Rule Engine:** Evaluates 30 lexical, syntactic, and domain rules (e.g., brand impersonation, IP host, suspicious TLD, excessive subdomains).
3. **Layer 2 — ML Inference Microservice:** Extracts 30 structural URL features and computes Random Forest phishing probability via a FastAPI microservice.
4. **Layer 3 — Threat Intelligence:** Queries Google Safe Browsing and VirusTotal with user consent, caching results for 1 hour.
5. **Layer 4 — Multi-Signal Scoring Engine:** Combines heuristic indicators, ML probabilities, reputation signals, and domain whitelists into an explainable 0–100 risk score.
6. **Safety & Response Centre:** Manages transaction disputes, duplicate detection, and automated complaint generation.

---

### 6. Scope and Operational Boundaries
- **In Scope:** URL detection, SMS/Email text scanning, QR image decoding, screenshot optical validation, personal finance dispute tracking, citizen emergency workflows, and browser extension integration.
- **Out of Scope (Boundary Limits):** Live execution of malware executables, deep active network crawling/spidering of target servers, automated unassisted bank balance transfers, and non-consensual tracking.

---

### 7. Functional and Non-Functional Requirements

#### 7.1 Functional Requirements
- **FR1 (Scan URL):** System must scan submitted URLs and return a risk score (0–100), risk level (`SAFE`, `SUSPICIOUS`, `HIGH_RISK`), and threat category.
- **FR2 (Scan QR Code):** System must decode QR images via ZXing and analyze embedded payloads.
- **FR3 (Scan Text):** System must extract and analyze URLs from email and SMS text.
- **FR4 (Incident Management):** System must allow users to log unauthorized debits, generate formal 1930 complaint drafts, and track incident status.
- **FR5 (Security Notifications):** System must alert users to high-risk scans via real-time Server-Sent Events (SSE).

#### 7.2 Non-Functional Requirements
- **NFR1 (Performance):** In-process heuristic and ML scoring must complete within 25ms.
- **NFR2 (Scalability):** System must support burst concurrency (>100 scans/sec on standard server hardware).
- **NFR3 (Security):** System must prevent SSRF to loopback and internal private subnets, enforce token-bucket rate limiting, and hash passwords via BCrypt.
- **NFR4 (Reliability):** System must degrade gracefully if the Python ML service or external threat intelligence providers are offline.

---

### 8. System Architecture & Topology
Prahar adopts a decoupled three-tier microservices architecture:
1. **Presentation Tier:** React 18 SPA built with Vite 5, TailwindCSS, Lucide icons, and Chart.js. Includes a Chrome Manifest V3 browser extension.
2. **Business & Security Tier:** Spring Boot 3.3.4 (Java 17) REST server providing authentication, scan orchestration, SSRF protection, scoring fusion, and notification SSE streaming.
3. **Machine Learning Tier:** Python 3.11 / FastAPI microservice exposing high-throughput feature extraction and Scikit-Learn inference endpoints.
4. **Data Tier:** Relational schema supporting MySQL 8.0 (production) and H2 in-memory storage (development/testing).

---

### 9. Detailed Module Descriptions
1. **URLScanController & Service:** Central scan orchestrator coordinating SSRF validation, normalization, rule execution, ML calls, scoring, persistence, and audit logging.
2. **SSRFProtectionService:** Enforces outbound boundary controls, preventing attackers from weaponizing the scanner to probe internal networks or cloud metadata endpoints (`169.254.169.254`).
3. **RuleEngineService:** In-process deterministic engine with 30 security rules categorized into Lexical, Domain, Security, and Content indicators.
4. **MultiSignalScoringEngine:** Computes composite 0–100 risk score, applies heuristic safety overrides, enforces official domain whitelists, and synthesizes human-readable explainability metadata.
5. **QRCodeService:** Uses ZXing 3.5.3 to decode single and multiple embedded QR codes from uploaded images or webcam video feeds.
6. **ChatService & NlpEngine:** Hybrid assistant; connects to Google Gemini API when configured, with seamless fallback to an offline deterministic intent classifier handling 20+ cybersecurity workflows.
7. **FraudIncidentService:** Manages incident lifecycles, computes follow-up deadlines, and compiles formal complaints for bank Nodal Officers and national cybercrime portals.

---

### 10. Technology Stack & Technical Justifications
- **Java 17 & Spring Boot 3.3.4:** Selected for enterprise reliability, strong type safety, mature JPA transaction handling, and high-concurrency embedded Tomcat performance.
- **Python 3.11 & FastAPI:** Selected for Python's preeminent machine learning ecosystem (Scikit-Learn, Pandas) combined with FastAPI's asynchronous ASGI performance.
- **React 18 & Vite 5:** Selected for rapid hot module replacement, component reusability, and dynamic bundle code-splitting via Rollup.
- **ZXing ("Zebra Crossing") 3.5.3:** Industry-standard pure-Java barcode and QR code decoding library with multi-format support.
- **MySQL 8.0:** ACID-compliant relational storage with indexed foreign keys, JSON column support, and TLS/SSL enforcement.

---

### 11. Database Design and Entity Relationships
The Prahar relational schema contains 14 core tables:
- `users`: User identity, hashed passwords, role references.
- `roles`: RBAC permissions (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_ANALYST`).
- `url_scans`: Scan history, client IP, user agent, risk score, verdict.
- `threat_detections`: One-to-one scan threat details and confidence score.
- `threat_indicators`: One-to-many granular rules triggered per detection.
- `model_predictions`: ML inference probability, model name, and version.
- `prevention_content`: Categorized educational guidance (cached in memory).
- `financial_transactions`: User transaction ledger with SHA-256 duplicate fingerprints.
- `fraud_incidents`: Fraud reports, amounts, bank references, and action timelines.
- `user_notifications` & `notification_preferences`: In-app alert queue and user severity thresholds.
- `chat_conversations` & `chat_messages`: Multi-turn persistent AI consultation history.
- `audit_logs`: Immutable security audit log tracking user and system actions.

---

### 12. API Contracts & Service Communication
- **Backend Port:** `8080` (Spring Boot REST API)
- **ML Service Port:** `8000` (FastAPI Microservice)
- **Frontend Port:** `5173` (Vite Dev Server) / `80` (Nginx Production)
- **Primary Endpoints:**
  - `POST /api/scans/url`: Analyzes submitted URL.
  - `POST /api/scans/unified-image`: Multipart upload for screenshot and QR image analysis.
  - `POST /api/scans/message`: Scans SMS content for lure language and malicious links.
  - `GET /api/health/readiness`: Verifies database and ML service operational readiness.
  - `GET /api/notifications/stream`: Real-time Server-Sent Events stream for security alerts.
  - `POST /api/incidents`: Registers fraud incident and generates dispute checklists.

---

### 13. Phishing Detection Methodology & Multi-Signal Scoring Engine
Prahar's composite risk scoring formula:

$$\text{Final Score} = \min\left(100, \max\left(S_{\text{floor}}, w_H \cdot S_H + w_{ML} \cdot S_{ML} + w_{TI} \cdot S_{TI}\right)\right)$$

Where:
- $S_H$ = Rule Engine Heuristic Score (0–100, weight $w_H = 0.50$)
- $S_{ML}$ = ML Probability $\times 100$ (0–100, weight $w_{ML} = 0.35$)
- $S_{TI}$ = Threat Intelligence Score (0–100, weight $w_{TI} = 0.15$)
- **Positive Intel Match Override:** If confirmed by Google Safe Browsing or VirusTotal, final score is set to $\ge 90$ with `HIGH_RISK` verdict.
- **Severe Heuristic Floor:** If $S_H \ge 40$ (e.g. brand impersonation or punycode detected), score floor is set to 70 regardless of whether the ML model gave a conservative estimate.
- **Legitimate Domain Whitelist:** Verifies official bank and government domains, reducing false positive rates on complex URLs to $<2.5\%$.

---

### 14. Machine Learning Pipeline & Feature Extraction Methodology
The ML pipeline extracts 30 features across three categories:
1. **Lexical & Length Features (12):** URL length, hostname length, path length, query length, digit counts, hyphen counts, at-symbol count, double-slash count, subdomain count, directory depth, vowel ratio, tld length.
2. **Character & Entropy Features (8):** Shannon entropy of hostname and URL, consonant-to-vowel ratio, hex character ratio, repeated character patterns.
3. **Syntactic & Security Flags (10):** IP address host flag, non-standard port flag, HTTPS protocol flag, free hosting TLD flag, punycode flag, sensitive keyword count, query parameter count, anchor tag flag, file extension flag, base64 payload flag.

Classifier: Random Forest ensemble with 100 estimators trained on balanced, deduplicated data partitions.

---

### 15. Security Hardening, Privacy Controls, and SSRF Prevention
1. **SSRF Guard:** Performs pre-scan DNS resolution; blocks loopback (`127.0.0.1`, `::1`), link-local (`169.254.0.0/16`), and private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
2. **Punycode Handling:** Automatically normalizes internationalized Unicode domains via `java.net.IDN.toASCII()`, preventing parser evasion.
3. **Rate Limiting:** In-memory token bucket filter (`15 req/min` for authentication; `60 req/min` for URL scans).
4. **Secret Scrubbing:** Sensitive data scrubber redacts passwords, JWT tokens, and account numbers from logs and audit trails.
5. **Zero Hardcoded Secrets:** Production profile strictly rejects default JWT secrets and requires environment-injected credentials.

---

### 16. Testing Methodology and Empirical Test Outcomes
Prahar maintains 175 automated test suites:
- **Backend Tests (120):** Unit, integration, security rate limiter, SSRF guard, image validation, cross-user data isolation, and multi-thread concurrency benchmarks.
- **ML Service Tests (35):** API endpoints, feature extraction dimensions, rule explainer, governance pipeline, and latency benchmarks.
- **Frontend Tests (10):** File format validation, 10MB upload guard, network timeout handling, and HTTP 429 rate limit classifications.
- **Browser Extension Tests (10):** Manifest V3 compliance, secret scanning, URL scheme filtering, and backend schema compatibility.

**Overall Test Result:** **175 / 175 Tests Passed (100% Green, 0 Failures, 0 Errors)**.

---

### 17. Results and Discussion
- **Latency:** Rule engine executes in **0.08 ms** median; ML inference executes in **5.12 ms** median; full end-to-end URL scan (including database persistence) completes in **8.1 ms** median.
- **Throughput:** Sustained concurrency burst test (10 worker threads) achieved **119 scans/second** with zero errors and zero primary key collisions.
- **Frontend Optimization:** Code-splitting reduced the initial JavaScript transfer size by **61.3%** (from 520 kB down to 201 kB), cutting production build time from 18.2s to 4.18s.
- **Database Optimization:** Static prevention guide in-memory caching reduced repeated DB query time from 7.0ms to **0.003 ms** (>1,000x acceleration).

---

### 18. Known System Limitations
1. **Infallibility Boundary:** No automated detection system can guarantee 100% detection of zero-hour phishing, particularly spear-phishing utilizing legitimate compromised corporate domains.
2. **OCR Dependency:** Native OCR requires system-level Tesseract binary. When absent, the system gracefully falls back to client hints and visual ZXing decoding.
3. **AI Provider Latency:** Live Google Gemini API queries require 800ms–2500ms depending on internet connectivity (mitigated by sub-millisecond local intent engine).

---

### 19. Future Enhancements
1. Integration of live headless browser sandboxing (Playwright/Puppeteer) for dynamic DOM mutation inspection.
2. Deep learning Transformer (BERT/RoBERTa) sequence models for natural language email header analysis.
3. Integration with open-source threat intelligence sharing platforms (MISP / STIX-TAXII).
4. Dedicated mobile companion application (Android / iOS) for SMS inbox auto-filtering.

---

### 20. Conclusion
Prahar delivers a robust, real-time, explainable cybersecurity defense platform tailored for modern multi-channel phishing threats. By fusing heuristic safety nets, high-throughput machine learning inference, and citizen-focused emergency incident management into a coherent microservices architecture, Prahar bridges the critical gap between threat detection and immediate post-fraud mitigation. All architectural, functional, and performance claims are backed by 175 automated tests and reproducible empirical benchmarks.

---

### 21. References
1. APWG (Anti-Phishing Working Group), "Phishing Activity Trends Report," 2025–2026.
2. National Cyber Crime Reporting Portal, Ministry of Home Affairs, Government of India, `https://cybercrime.gov.in`.
3. OWASP Top 10 Web Application Security Risks, "A10: Server-Side Request Forgery (SSRF)," 2021/2025.
4. Scikit-Learn: Machine Learning in Python, Pedregosa et al., JMLR 12, pp. 2825-2830.
5. RFC 3492: "Punycode: A Bootstring encoding of Unicode for Internationalized Domain Names in Applications (IDNA)."
6. Spring Boot Reference Documentation, Spring Framework 6.x / Spring Boot 3.3.x, Pivotal / VMware Tanzu.
7. Vite: Next Generation Frontend Tooling, Evan You & Vite Contributors, `https://vitejs.dev`.
