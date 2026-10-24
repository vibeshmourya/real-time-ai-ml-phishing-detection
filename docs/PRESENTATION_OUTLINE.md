# Prahar Academic Presentation Outline & Slide Deck Guide

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Presentation Target:** MCA Final Viva Voce & Project Defense (12–15 Minutes)  
**Status:** EVIDENCE-VERIFIED  

---

### Slide 1: Title & Project Overview
- **Header:** PRAHAR: Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System
- **Sub-header:** A Robust Enterprise Microservices Architecture for Zero-Hour Threat Detection and Citizen Emergency Response
- **Candidate Name:** Vibesh (MCA Final Year)
- **Institution:** Department of Computer Applications
- **Key Takeaway:** Real-time protection across URLs, SMS, Emails, QR codes, and Screenshots with explainable risk scoring.

---

### Slide 2: Problem Statement
- **The Challenge:** Rapid explosion of modern social engineering attacks bypassing traditional defenses.
- **Key Pain Points:**
  - Multi-channel attack delivery (Smishing, Quishing, malicious screenshots).
  - Ephemeral zero-day domains discarded before DNS blacklists can update.
  - Punycode / Homoglyph brand impersonation exploiting visually identical characters.
  - Black-box security tools providing binary verdicts with zero educational context.
  - Average citizen delay of 4–12 hours in freezing compromised bank accounts post-phishing.

---

### Slide 3: Motivation
- **National Context:** High volume of citizen financial fraud reported on the National Cybercrime Reporting Portal (`cybercrime.gov.in`) and citizen helpline (`1930`).
- **The Golden Hour:** Siphoning of stolen funds occurs within 30 to 60 minutes via mule account layers.
- **Vision:** Create an integrated platform that not only detects phishing in real time before credentials are typed, but also arms the user with immediate automated incident response tools.

---

### Slide 4: Project Objectives
1. **Real-Time Scanning:** Deliver sub-10ms URL classification combining static heuristics and machine learning.
2. **Multi-Channel Ingestion:** Support direct URLs, raw email/SMS text, QR codes (ZXing), and screenshot images.
3. **Transparent Explainability:** Replace opaque probabilities with an explainable 0–100 risk score and structured indicator cards.
4. **Citizen Incident Centre:** Automate 1930 helpline dispute drafting and bank notification timelines.
5. **Security & Reliability:** Implement strict SSRF guards, rate limiting, and 100% offline fallback resilience.

---

### Slide 5: Limitations of Existing Systems
- **Static Blacklists (DNSBL):** Reactive; takes hours/days to flag newly registered domains.
- **Browser-Only Extensions:** Neglect QR codes (physical posters) and SMS messaging vectors.
- **Standalone ML Models:** Prone to false alarms on complex URLs and lack post-incident containment features.
- **External API Dependency:** Tools that forward every user URL to third-party APIs violate privacy and fail when external networks go down.

---

### Slide 6: Proposed Solution — Prahar
- **Hybrid Microservices Defense:**
  - **4-Layer Heuristic Rule Engine (30 Rules):** High-speed deterministic safety net.
  - **30-Feature Random Forest ML Microservice:** Detects hidden structural anomalies in unknown URLs.
  - **Multi-Signal Scoring Engine:** Harmonizes rules, ML probabilities, reputation feeds, and domain whitelists.
  - **Integrated Fraud & Safety Centre:** Incident lifecycle tracking, transaction import, and complaint generation.
  - **Manifest V3 Extension:** Real-time ambient navigation protection.

---

### Slide 7: System Architecture
- **Presentation Tier:** React 18 / Vite 5 SPA (lazy-loaded modules, 201 kB bundle) + Chrome Extension.
- **Application Tier:** Spring Boot 3.3.4 (Java 17) REST server enforcing SSRF defense, JWT security, and SSE streaming.
- **Machine Learning Tier:** Python 3.11 / FastAPI microservice executing asynchronous Random Forest inference.
- **Data Tier:** MySQL 8.0 relational schema (production) with H2 in-memory storage (development/testing).

*(Refer to `docs/ARCHITECTURE_DIAGRAMS.md` for the complete system topology diagram)*

---

### Slide 8: Technology Stack & Justification
| Layer | Technology | Justification |
| :--- | :--- | :--- |
| **Frontend** | React 18 + Vite 5 | Fast HMR, dynamic Rollup code-splitting, responsive TailwindCSS |
| **Backend** | Spring Boot 3.3.4 (Java 17) | Enterprise transaction boundaries, robust concurrency, Spring Security 6 |
| **ML Engine** | FastAPI + Scikit-Learn | High-throughput asynchronous Python microservice (194 req/s) |
| **QR Decoding** | ZXing 3.5.3 | Pure-Java optical barcode/QR decoding with multi-code detection |
| **Database** | MySQL 8.0 + HikariCP | ACID-compliant storage, connection pooling, enforced TLS/SSL |

---

### Slide 9: Phishing Detection & Scoring Methodology
- **Composite Scoring Formula:**
  $$\text{Final Score} = \min\left(100, \max\left(S_{\text{floor}}, 0.50 \cdot S_H + 0.35 \cdot S_{ML} + 0.15 \cdot S_{TI}\right)\right)$$
- **Positive Threat Intelligence Override:** Confirmed vendor match forces score to $\ge 90$ (`HIGH_RISK`).
- **Severe Heuristic Safety Net:** If $S_H \ge 40$, score floor is pinned at 70 regardless of conservative ML predictions.
- **Official Domain Whitelist:** Validates authentic bank and government domains to suppress false alarms (<2.5% FPR).

---

### Slide 10: Machine Learning Pipeline
- **30 Extracted URL Features:**
  - *Lexical (12):* Lengths, path depth, digit/hyphen counts, vowel ratios.
  - *Statistical (8):* Shannon entropy, consonant-to-vowel ratios, hex ratios, repeat patterns.
  - *Syntactic & Security (10):* IP host flag, non-standard port flag, punycode flag, sensitive keywords.
- **Inference Profile:** Random Forest classifier executes in **5.12 ms median** with Scikit-Learn model pre-loaded in memory.

---

### Slide 11: Multi-Channel Analysis & Emergency Response
- **Quishing Detection:** Decodes embedded URLs in QR codes and validates destination hosts before user navigation.
- **Screenshot Ingestion:** Magic-byte validator verifies PNG/JPEG signatures; OCR extracts text to flag urgency triggers ("Account Blocked", "Immediate KYC Required").
- **Citizen Safety Centre:** Formats formal complaint drafts according to National Cybercrime Reporting Portal standards.

---

### Slide 12: Security Hardening & Privacy Controls
- **SSRF Defense:** Blocks DNS resolution to loopback, link-local, and RFC-1918 private subnets.
- **IDN / Punycode Normalization:** Converts Cyrillic/Greek homoglyphs to Punycode (`java.net.IDN.toASCII()`), triggering homoglyph detection rules.
- **Rate Limiting:** Token-bucket filter restricts authentication endpoints to 15 req/min and scan endpoints to 60 req/min.
- **Sensitive Data Scrubber:** Automatic redaction of passwords, tokens, and bank details from logs and audit trails.

---

### Slide 13: Testing & Empirical Evaluation Results
- **Automated Regression Suite:** **175 / 175 Tests Passing (100% Green)**
  - Backend Tests: **120**
  - ML Microservice Tests: **35**
  - Frontend Tests: **10**
  - Browser Extension Tests: **10**
- **Performance Benchmarks:**
  - Rule Engine: **0.08 ms** median (>12,000 ops/s)
  - ML Inference: **5.12 ms** median (194 req/s)
  - Full End-to-End URL Scan: **8.1 ms** median
  - Concurrency Burst: **119 scans/second** across 10 worker threads with 0% error rate.
  - Bundle Size: Reduced by **61.3%** (from 520 kB to 201 kB).

---

### Slide 14: Limitations & Future Scope
- **Current Limitations:**
  - Zero-day phishing hosted on compromised legitimate corporate portals requires behavioral reporting.
  - Full native OCR requires host-level Tesseract binary.
  - Live Gemini AI queries have 800ms–2500ms latency (mitigated by sub-millisecond offline intent engine).
- **Future Work:**
  - Headless browser DOM sandboxing (Playwright) for dynamic credential harvesting detection.
  - Deep learning Transformer sequence models (BERT) for email header analysis.
  - Mobile companion app for Android SMS auto-filtering.

---

### Slide 15: Conclusion & Defense Q&A
- **Summary:** Prahar successfully demonstrates a production-grade, multi-channel phishing defense and citizen incident mitigation system.
- **Key Achievements:** Verified sub-10ms detection speed, 175 passing tests, robust SSRF hardening, and practical 1930 emergency integration.
- **Closing:** Thank you. We invite questions from the examination committee.
