# Prahar Comprehensive Viva Voce Defense Guide & Question Bank

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Academic Target:** Master of Computer Applications (MCA) Project Defense  
**Status:** EVIDENCE-VERIFIED  

---

## Topic 1: Project Overview & Motivation

### Q1: What is the core problem that Prahar solves?
**Answer:**  
Prahar addresses the failure of traditional anti-phishing tools against modern, multi-channel cyber fraud. Attackers no longer rely solely on malicious emails; they exploit SMS (Smishing), QR codes (Quishing), and deceptive customer support screenshots. Furthermore, traditional blacklists take hours or days to register newly weaponized domains. Prahar delivers sub-10ms, multi-channel phishing detection using a hybrid engine of 30 heuristic rules, machine learning, and explainable risk scoring, paired with an emergency response center for victims of financial cyber fraud.

### Q2: Why is the "Golden Hour" concept central to Prahar's design?
**Answer:**  
In financial cyber fraud (particularly UPI, netbanking, and credit card scams), stolen funds are typically siphoned through multiple layers of intermediate "mule" bank accounts within 30 to 60 minutes. Once funds are withdrawn via cash ATMs or crypto exchanges, recovery rates drop drastically. Prahar integrates direct citizen workflows for the Indian **1930** cybercrime helpline, automated bank dispute drafting, and transaction duplicate fingerprinting so victims can freeze accounts within minutes.

### Q3: Which features did you implement personally in this project?
**Answer:**  
1. **Full-Stack Architecture:** Developed the Spring Boot 3 backend orchestrator, the React 18 single-page application with route-level code-splitting, and the Manifest V3 browser extension.
2. **Detection & Scoring Engine:** Designed the 30-rule heuristic engine, the `MultiSignalScoringEngine` combining ML probabilities and threat intel, and the SSRF protection validator with IDN Punycode support.
3. **Machine Learning Pipeline:** Implemented the 30-feature URL extraction module, the Random Forest inference service in FastAPI, and data governance gates enforcing duplicate filtering.
4. **Safety & Response Centre:** Built the citizen financial dispute tracking system, SHA-256 duplicate transaction detection, and Server-Sent Events (SSE) notification streaming.

---

## Topic 2: System Architecture & Technologies

### Q4: Why did you choose a microservices architecture separating Spring Boot and FastAPI instead of a single monolith?
**Answer:**  
We separated concerns based on runtime ecosystem strengths:
- **Python (FastAPI):** Python is the preeminent ecosystem for data science and machine learning (Scikit-Learn, Pandas, Joblib). FastAPI provides asynchronous ASGI execution with sub-millisecond serialization.
- **Java (Spring Boot 3.3.4):** Java provides enterprise-grade type safety, robust JPA transaction boundaries, mature security filtering (Spring Security 6), and pure-Java multi-threaded optical QR decoding (ZXing).
- **Graceful Degradation:** If the Python ML microservice crashes or experiences heavy load, the Spring Boot core remains operational and falls back to the deterministic 30-rule heuristic engine without dropping user requests.

### Q5: How is the React frontend structured, and how did you optimize its performance?
**Answer:**  
The frontend is built with React 18 and Vite 5 using a modular architecture with context providers (`AuthContext`, `ScanContext`, `LanguageContext`, `ThemeContext`). In Phase 29, we resolved a monolithic bundle issue (previously 520 kB) by:
1. Configuring Rollup `manualChunks` in `vite.config.js` to isolate `vendor-react`, `vendor-icons` (Lucide), and `vendor-charts` (Chart.js).
2. Implementing `React.lazy()` and `<Suspense>` for all secondary pages (`ScannerPage`, `MyMoneyPage`, `AdminPortalPage`, etc.).
3. This achieved a **61.3% reduction** in initial JavaScript bundle size (down to 201 kB) and cut the production build time to 4.18 seconds.

---

## Topic 3: Detection Pipeline & Scoring Methodology

### Q6: How does a URL reach the classification stage, and what is the step-by-step pipeline?
**Answer:**  
1. **Input Normalization & Punycode Conversion:** The URL is trimmed and converted to ASCII Punycode via `java.net.IDN.toASCII()` to prevent homoglyph evasion.
2. **SSRF Boundary Check:** `SSRFProtectionService` resolves the domain via DNS and rejects loopback, link-local, and RFC-1918 private IP addresses.
3. **Layer 1 — Heuristic Evaluation:** `RuleEngineService` tests 30 lexical, syntactic, and security rules in-process (0.08ms median).
4. **Layer 2 — ML Inference:** `MLServiceClient` sends the URL to the FastAPI `/predict` microservice (5.12ms median).
5. **Layer 3 — Threat Intelligence:** If configured and user consent is granted, queries Google Safe Browsing / VirusTotal (cached for 1 hour).
6. **Layer 4 — Composite Scoring:** `MultiSignalScoringEngine` computes the weighted composite score (0–100), applies safety floors and domain whitelists, and generates human-readable explanations.
7. **Persistence & Alerting:** The scan record is saved to the database, logged in the audit ledger, and broadcast via SSE if high-risk.

### Q7: How does the system combine ML predictions and rule-based evidence?
**Answer:**  
Through the formula:
$$\text{Final Score} = \min\left(100, \max\left(S_{\text{floor}}, 0.50 \cdot S_H + 0.35 \cdot S_{ML} + 0.15 \cdot S_{TI}\right)\right)$$
- **Positive Intel Match Floor:** If external threat intelligence confirms a malicious URL, score is forced to $\ge 90$ (`HIGH_RISK`).
- **Severe Heuristic Floor:** If heuristic severity $S_H \ge 40$ (e.g. brand impersonation or Punycode homoglyph detected), the score floor is pinned at 70 regardless of whether the ML model gave a conservative probability.
- **Whitelisting:** Authenticated official domains (e.g., `onlinesbi.sbi`, `cybercrime.gov.in`) receive negative risk adjustments to suppress false alarms on complex query strings.

### Q8: What happens if the Python ML service or external AI provider is unavailable?
**Answer:**  
Prahar is engineered for zero-panic graceful degradation:
- If the **ML microservice is offline**, `MLServiceClient` catches the connection timeout (3000ms limit), records an honest `ML_SERVICE_UNAVAILABLE` status, and allows the 30-rule heuristic engine to complete the scan with full explainability.
- If the **AI chatbot provider (Gemini) is offline**, a circuit breaker trips and seamlessly switches to `NlpEngine.java`, a local deterministic intent classifier that resolves 20+ cybersecurity workflows in **<0.5 ms** without network latency.

---

## Topic 4: Machine Learning, Features & Model Governance

### Q9: What features does your ML model extract from URLs, and why?
**Answer:**  
The model extracts 30 features across three distinct categories:
1. **Lexical & Length (12):** URL length, hostname length, path depth, digit counts, hyphen count (common in typosquatting), vowel ratios.
2. **Entropy & Statistical (8):** Shannon entropy of the hostname and URL. High entropy strongly correlates with algorithmically generated domain names (DGA) used by botnets and fast-flux phishing campaigns.
3. **Syntactic & Security Flags (10):** IP address host flag, non-standard port flag, HTTPS protocol flag, free hosting TLD flag, Punycode flag, and sensitive credential keywords (`login`, `verify`, `banking`, `update`).

### Q10: How do you evaluate false positives and false negatives?
**Answer:**  
We evaluate using standard confusion matrix metrics:
- **Precision:** $\frac{TP}{TP + FP}$ — Measures the proportion of flagged URLs that are truly phishing.
- **Recall:** $\frac{TP}{TP + FN}$ — Measures the proportion of actual phishing URLs detected.
- **False Positive Rate (FPR):** $\frac{FP}{FP + TN}$ — Evaluated against a hard-negatives benchmark of legitimate banking and government portals with complex query parameters. Our governance pipeline enforces an FPR threshold of $<3.0\%$.

### Q11: What are the current ML model's limitations?
**Answer:**  
1. Purely lexical ML models cannot inspect page content behind dynamic JavaScript walls or multi-factor authentication screens.
2. Attackers hosting credential harvesting forms on legitimate cloud infrastructure (e.g., Google Forms, Canva, Notion) evade lexical classifiers because the root domain has high reputation. Prahar addresses this by combining content keywords and user warning cards.

---

## Topic 5: Security, SSRF & Database Hardening

### Q12: What is Server-Side Request Forgery (SSRF), and how does Prahar prevent it?
**Answer:**  
SSRF occurs when an attacker tricks the server into making HTTP requests to internal, non-public resources (such as `http://localhost:8080/admin` or cloud metadata endpoints `http://169.254.169.254/latest/meta-data`).  
Prahar's `SSRFProtectionService` enforces three defensive layers:
1. **Protocol Restriction:** Only `http` and `https` schemes are allowed; file and data schemes are blocked.
2. **Port Restriction:** Only standard ports (80, 443, 8080, 8443) are permitted.
3. **DNS Pre-Resolution:** Resolves the hostname before connecting and blocks loopback (`127.0.0.0/8`, `::1`), link-local (`169.254.0.0/16`), and private RFC-1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).

### Q13: How does the system handle Internationalized Domain Names (IDN) and Punycode homoglyph attacks?
**Answer:**  
Attackers register domains using visually identical Cyrillic characters (e.g., `раураl.com` using Cyrillic 'р' and 'а'). Standard URI parsers fail on raw Unicode hostnames. Prahar normalizes the domain via `java.net.IDN.toASCII()`, converting it to standard Punycode (`xn--80aa9a5ad.com`). The heuristic engine checks for the `xn--` prefix, calculates visual character mixing, and immediately flags the URL as a suspicious homoglyph attack.

### Q14: How is database performance and connection pooling configured?
**Answer:**  
- **Connection Pool:** We use HikariCP configured with 20 connections in production (`maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 20000ms`).
- **In-Memory Caching:** Static educational prevention content is memoized in a `ConcurrentHashMap` in `PreventionContentService.java`, reducing repeated DB query time from 7.0ms to **0.003 ms** (>1,000x acceleration).
- **Index Optimization:** Database tables have indexed foreign keys and lookup columns (`url_scans(domain)`, `url_scans(risk_level)`, `user_notifications(user_id, is_read)`).

---

## Topic 6: Testing, Performance & Future Scope

### Q15: How did you test system concurrency and burst scalability?
**Answer:**  
In `PerformanceAndConcurrencyBenchmarkTest.java`, we utilized Java's `ExecutorService` with 10 concurrent worker threads synchronized by a `CountDownLatch` barrier. The system executed 30 simultaneous end-to-end security scans (including SSRF, heuristics, ML client, scoring, JPA persistence, prevention guide, and audit logging) in **252 milliseconds**, demonstrating **119 scans/second throughput** with a **100% success rate** and zero primary key collisions.

### Q16: What would you improve or implement if given additional time?
**Answer:**  
1. **Dynamic Sandbox Inspection:** Deploy a containerized headless browser (Playwright/Puppeteer) to render suspicious URLs in an isolated sandbox, capturing DOM mutation events and fake password fields.
2. **Deep Learning Sequence Models:** Train a lightweight BERT/RoBERTa Transformer model on raw email headers and body text for advanced linguistic manipulation detection.
3. **Mobile Companion Application:** Build a native Android application using the Android SMS Retriever API to automatically filter incoming SMS phishing lures before the citizen taps the link.
