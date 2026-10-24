# PHASE 32 — COMPREHENSIVE VIVA VOCE QUESTION BANK

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Academic Target:** MCA Final Examination & Technical Viva Voce  
**Total Questions:** 62 Substantive Questions with Technical Answers  
**Verification Baseline:** Ground Truth from Implemented Source Code (175/175 Automated Tests Passing)

---

## Guide to Notation
- `[IMPLEMENTED]`: Directly refers to code, algorithms, configurations, and tests present in the Prahar repository.
- `[CONCEPTUAL]`: Standard theoretical computer science, machine learning, or cybersecurity principles applied to contextualize decisions.

---

## TOPIC 01: Project Objectives and Motivation

### Q1. What is the core problem that Prahar solves, and why is existing software insufficient?
**Answer:** `[IMPLEMENTED]`  
Traditional antiphishing mechanisms rely on signature blacklists (e.g., Google Safe Browsing, PhishTank). While accurate, blacklists exhibit a 48–72 hour dwell time before zero-day phishing links are indexed, leaving users unprotected during peak campaign efficacy. Furthermore, phishing has evolved across multiple channels (SMS, QR codes / quishing, and deceptive screenshots). Prahar solves this by combining synchronous heuristic analysis, a 30-feature ML inference engine, and image/QR decoding to deliver explainable verdicts in under 25ms without waiting for third-party blacklists.

### Q2. What are the measurable objectives of Prahar?
**Answer:** `[IMPLEMENTED]`  
1. Sub-50ms end-to-end scanning latency for URL feature extraction and inference.
2. Low false positive rate (<2.5%) on verified utility and banking domains via fast-path whitelisting.
3. Multi-channel detection covering raw URLs, QR codes (ZXing), and screenshot text (OCR/regex).
4. Zero outbound target crawl vulnerability (eliminating server-side drive-by infection and SSRF).
5. 100% service availability through automated graceful degradation when ML or external APIs are unreachable.

### Q3. Why did you name the system "Prahar"?
**Answer:** `[CONCEPTUAL]`  
"Prahar" (Sanskrit for "Counterstrike" or "Offensive Defense") reflects a proactive cybersecurity stance: analyzing threat vectors at the user perimeter before compromise occurs, rather than post-breach incident triage.

### Q4. Who are the primary target users and deployment environments for Prahar?
**Answer:** `[IMPLEMENTED]`  
Prahar is designed for everyday digital citizens, banking customers, enterprise SOC analysts, and university intranet users who require instant verification of suspicious links, SMS lures, and QR payment matrices.

---

## TOPIC 02: System Architecture

### Q5. Why is this polyglot microservice architecture (Spring Boot + FastAPI) suitable for this project?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
Spring Boot excels at enterprise-grade backend infrastructure: robust connection pooling, declarative transaction management, strict security filter chains (JWT, token-bucket rate limiting, SSRF perimeter guards), and typed database persistence. Conversely, Python is the uncontested industry standard for machine learning, vector arithmetic (NumPy/Pandas), and tree-ensemble libraries (Scikit-Learn, XGBoost). Running Python inside the JVM via Jython or GraalVM is brittle, slow, and lacks modern C-extension support. Decoupling them via HTTP REST isolates heavy tensor mathematics from transaction management.

### Q6. What happens if the Python ML microservice crashes or becomes unavailable?
**Answer:** `[IMPLEMENTED]`  
Prahar implements **Automated Graceful Degradation**. In `URLScanService.java` and `MLServiceClient.java`, the HTTP call to `http://localhost:8000/predict` is wrapped in a try-catch block with a strict 2000ms socket timeout. If the microservice is unreachable, `MLPredictionResult.isAvailable()` is set to `false`. The `MultiSignalScoringEngine.java` dynamically redistributes signal weights: heuristic rules are re-weighted to 65% and threat intelligence to 35%, ensuring zero disruption to the user.

### Q7. How does Prahar guarantee zero outbound target crawling risk?
**Answer:** `[IMPLEMENTED]`  
Unlike crawlers that send HTTP GET requests to target websites to render DOM trees, Prahar's detection pipeline is **completely passive and lexical**. It extracts 30 structural, entropy, and lexical features solely from the submitted URL string, QR matrix, or screenshot pixels. This eliminates the risk of server-side malware infection, SSRF, or notifying the attacker that their honeypot URL is being inspected.

### Q8. What is the role of the Request Correlation Filter?
**Answer:** `[IMPLEMENTED]`  
In `RequestCorrelationFilter.java`, an incoming HTTP request is assigned a unique UUID `X-Correlation-ID`. This ID is injected into SLF4J's Mapped Diagnostic Context (MDC), ensuring that every log line across security filters, services, database transactions, and client responses is tagged with the same trace identifier for forensic auditability.

---

## TOPIC 03: React and Frontend Architecture

### Q9. Why did you choose React 18 with Vite instead of traditional template engines (Thymeleaf/JSP)?
**Answer:** `[IMPLEMENTED]`  
A Single Page Application (SPA) architecture provides instantaneous visual feedback without full-page reloads. Real-time security applications demand dynamic progress meters, interactive risk gauges, accordion-style forensic breakdowns, and asynchronous Server-Sent Events (SSE) notification streams (`NotificationBell.jsx`), which are cumbersome to manage in static server-side templates.

### Q10. How is state managed across the frontend application?
**Answer:** `[IMPLEMENTED]`  
Prahar utilizes React's standard Hooks (`useState`, `useEffect`, `useCallback`, `useContext`) paired with modular API abstractions in `frontend/src/api/`. Transient scan results and modal state are encapsulated within local page components (`UrlScanPage.jsx`), while authentication tokens and notification badges are managed via centralized context providers.

### Q11. Why did you use pure Vanilla CSS instead of Tailwind CSS?
**Answer:** `[IMPLEMENTED]`  
Vanilla CSS with modern CSS custom variables (`index.css`) provides total control over design tokens, dark-mode color gamuts, glassmorphic card elevations, and micro-animations without external compilation dependencies or build-time class purging issues. This kept the production bundle lean at 201.41 kB gzip-ready.

### Q12. How does the frontend handle network partitions or backend outages?
**Answer:** `[IMPLEMENTED]`  
Axios interceptors in `frontend/src/api/scanApi.js` trap HTTP error status codes (400, 401, 403, 429, 500). If the backend is completely unreachable (network timeout / connection refused), the client renders a localized, non-blocking alert banner and offers offline troubleshooting guidance rather than throwing uncaught JavaScript errors.

---

## TOPIC 04: Spring Boot and REST APIs

### Q13. How are REST API errors standardized across Spring Boot?
**Answer:** `[IMPLEMENTED]`  
Prahar implements a centralized `@RestControllerAdvice` in `GlobalExceptionHandler.java`. All controller exceptions (`BadRequestException`, `SSRFSecurityException`, `ResourceNotFoundException`, `MethodArgumentNotValidException`) are mapped to a uniform generic envelope: `ApiResponse<T>` containing `success: false`, `message: String`, `data: null`, and `timestamp`.

### Q14. What are the key endpoints exposed by the Spring Boot backend?
**Answer:** `[IMPLEMENTED]`  
- `POST /api/scans/url`: Public URL phishing scanner.
- `POST /api/scans/unified-image`: Multipart endpoint for combined QR, OCR, and visual analysis.
- `GET /api/scans/ml-status`: Real-time health check of Python microservice.
- `GET /api/health`: Aggregated health check for backend, database, and ML service.
- `POST /api/chat/message`: Cybersecurity chatbot and incident advisor.
- `GET /api/notifications/stream`: Real-time Server-Sent Events (SSE) alert stream.

### Q15. Why did you use `@Transactional` annotations on scan methods?
**Answer:** `[IMPLEMENTED]`  
In `URLScanService.java`, scanning generates multiple related database records: a `URLScan` entity, a 1-to-1 `ThreatDetection` forensic summary, and multiple 1-to-many `ThreatIndicator` rows. Marking `processAndPersistScan()` with `@Transactional` guarantees atomicity: if an exception occurs mid-write, Hibernate rolls back partial inserts, preventing orphaned scan records.

### Q16. How does Spring Boot communicate with the Python FastAPI microservice?
**Answer:** `[IMPLEMENTED]`  
Through `MLServiceClient.java`, utilizing Spring's `RestTemplate` (configured with `HttpComponentsClientHttpRequestFactory` for connection reuse). It serializes target URLs into JSON requests and maps JSON responses into strongly-typed `MLPredictionResult.java` DTOs.

---

## TOPIC 05: Python and FastAPI ML Microservice

### Q17. Why FastAPI instead of Flask or Django for the ML service?
**Answer:** `[IMPLEMENTED]`  
FastAPI runs on top of ASGI (Asynchronous Server Gateway Interface) via Starlette and Uvicorn. It natively supports asynchronous concurrency, has significantly lower memory and overhead than Django, automatically validates input schemas via Pydantic, and benchmarks up to 3x faster than traditional synchronous Flask WSGI.

### Q18. How is the ML model loaded into memory, and does it reload on every request?
**Answer:** `[IMPLEMENTED]`  
In `ml-service/classifier.py`, models (`phishing_model.pkl` and `scaler.pkl`) are loaded into global memory **once during application startup** in FastAPI's `@app.on_event("startup")` lifecycle hook. Subsequent calls to `/predict` perform inference against the pre-loaded in-memory model in under 5ms, avoiding repetitive disk I/O.

### Q19. What happens if the trained pickle model file is missing or corrupted?
**Answer:** `[IMPLEMENTED]`  
In `ml-service/classifier.py`, model loading is wrapped in a defensive try-except block. If `.pkl` files are absent, the service logs a warning and instantiates an **in-memory deterministic rule-based surrogate classifier** that scores based on the extracted feature vector. The endpoint continues returning valid predictions with HTTP 200 rather than crashing the microservice.

### Q20. How is Python API performance tested?
**Answer:** `[IMPLEMENTED]`  
Through `ml-service/tests/test_performance.py` using `pytest`. The benchmark verifies that feature extraction across a batch of diverse URLs completes in <2.0ms per URL and end-to-end `/predict` inference executes in <10ms.

---

## TOPIC 06: Machine Learning Fundamentals

### Q21. Which machine learning algorithms did you evaluate, and which one did you choose?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
We evaluated Random Forest, Support Vector Machines (SVM), Logistic Regression, LightGBM, and XGBoost. We selected **XGBoost (Extreme Gradient Boosting)** as the primary model. Tabular lexical feature vectors have complex non-linear relationships and feature interactions (e.g., high entropy matters significantly more when the TLD is `.xyz` than when it is `.edu`). Gradient boosted decision trees consistently outperform linear models and deep neural networks on tabular datasets without requiring extensive deep learning parameter tuning.

### Q22. What is the difference between classification and regression in this context?
**Answer:** `[CONCEPTUAL]`  
Classification predicts discrete class labels ($y \in \{0, 1\}$, where $0 = \text{Benign}$ and $1 = \text{Phishing}$). Regression predicts continuous numerical quantities. In Prahar, the model outputs a continuous calibrated probability score $P(\text{Phishing} \mid X) \in [0.0, 1.0]$ via the tree ensemble's sigmoid leaf transformation, which is then mapped to risk percentages ($0 - 100$) in the composite scoring engine.

### Q23. What is overfitting, and how do you prevent it in your tree models?
**Answer:** `[IMPLEMENTED]`  
Overfitting occurs when a model memorizes idiosyncrasies and noise in the training set rather than learning generalizable patterns, resulting in poor test accuracy. We prevented overfitting by:
1. Restricting tree depth (`max_depth = 6`).
2. Applying L1/L2 regularization (`reg_alpha = 0.1`, `reg_lambda = 1.0`).
3. Using subsampling (`subsample = 0.8`, `colsample_bytree = 0.8`).
4. Enforcing 5-fold cross-validation during hyperparameter search.

### Q24. What is the difference between supervised and unsupervised learning?
**Answer:** `[CONCEPTUAL]`  
Supervised learning trains on labeled datasets $(X, y)$ with ground-truth classifications (Phishing vs Benign). Unsupervised learning identifies latent clusters without labels. Prahar's classifier is strictly supervised, trained on verified URL datasets from PhishTank, OpenPhish, and benign Alexa/Tranco top domains.

---

## TOPIC 07: Dataset and Feature Engineering

### Q25. How do you prevent data leakage during feature extraction and model evaluation?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
Data leakage occurs when information outside the training dataset is used to create the model. In Prahar:
1. Feature extraction operates purely on raw string inputs at runtime without accessing global dataset statistics.
2. Numerical feature scaling (StandardScaler) is fitted **strictly on the training split** and persisted to `scaler.pkl`; the test split is transformed using the pre-fitted scaler.
3. Domain duplicate URLs across benign and malicious sets were de-duplicated prior to splitting.

### Q26. What exactly are the 30 features extracted by `feature_extractor.py`?
**Answer:** `[IMPLEMENTED]`  
Features span three primary categories:
- **Structural Lengths (4):** `url_length`, `domain_length`, `path_length`, `query_length`.
- **Character Counts & Delimiters (11):** Counts of dots, hyphens, underscores, slashes, question marks, equal signs, at symbols, percent signs, tildes, colons, and digits.
- **Statistical & Lexical Properties (15):** Shannon entropy ($H(X)$), vowel-to-consonant ratio, digit-to-letter ratio, consecutive character repetitions, raw IP indicator, HTTPS presence, Punycode `xn--` prefix, subdomain depth, brand impersonation flag, and high-risk TLD weight.

### Q27. What is Shannon Entropy, and why is it effective in phishing detection?
**Answer:** `[IMPLEMENTED]`  
Shannon entropy measures the uncertainty or randomness of characters in a string:
$$H(X) = -\sum_{i=1}^{n} P(x_i) \log_2 P(x_i)$$
Legitimate brand domains are designed for human recall and typically exhibit moderate entropy (e.g., `google.com` $\approx 2.3$). Phishing domains created using Domain Generation Algorithms (DGA) or token-scrambling services exhibit high lexical randomness (e.g., `x89a-q1z89b.xyz` $\ge 4.2$).

### Q28. How does Prahar detect brand impersonation in subdomains?
**Answer:** `[IMPLEMENTED]`  
In `feature_extractor.py` and `PhishingRuleEngine.java`, the domain is split using the Public Suffix List. If a trusted brand token (`sbi`, `paypal`, `google`, `microsoft`, `netflix`) appears in third- or fourth-level subdomains while the apex registered domain belongs to a different registrant, the flag `brand_in_subdomain` is set to 1.0, triggering an automatic score penalty.

---

## TOPIC 08: Evaluation Metrics & Confusion Matrix

### Q29. How do you distinguish a False Positive from a False Negative in cybersecurity?
**Answer:** `[CONCEPTUAL]`  
*Examiner Tough Question.*  
- **False Positive (Type I Error):** The system flags an authentic, benign website as phishing. This causes user inconvenience, support tickets, and alert fatigue.
- **False Negative (Type II Error):** The system classifies an active phishing website as benign. This leads to credential theft, financial fraud, and malware compromise.
In cybersecurity defense, **False Negatives are far more dangerous** than False Positives. However, a model with too many False Positives will be disabled by frustrated users. Prahar balances this via fast-path whitelisting (<2.5% FPR) and heuristic safety floors.

### Q30. Define Precision, Recall, F1 Score, and Specificity mathematically.
**Answer:** `[CONCEPTUAL]`  
- $\text{Precision} = \frac{TP}{TP + FP}$ (Of all predicted phishing URLs, how many were truly phishing?)
- $\text{Recall / Sensitivity} = \frac{TP}{TP + FN}$ (Of all actual phishing URLs, how many did we catch?)
- $\text{F1 Score} = 2 \times \frac{\text{Precision} \times \text{Recall}}{\text{Precision} + \text{Recall}}$ (Harmonic mean of precision and recall)
- $\text{Specificity} = \frac{TN}{TN + FP}$ (Ability to correctly identify legitimate sites)

### Q31. What is a Confusion Matrix, and what are its four quadrants?
**Answer:** `[CONCEPTUAL]`  
A contingency table summarizing classification performance:
1. **True Positive (TP):** Phishing correctly identified as Phishing.
2. **False Positive (FP):** Benign mistakenly flagged as Phishing.
3. **True Negative (TN):** Benign correctly identified as Benign.
4. **False Negative (FN):** Phishing missed and flagged as Benign.

### Q32. What empirical performance does the Prahar test suite verify?
**Answer:** `[IMPLEMENTED]`  
In `ml-service/tests/test_performance.py` and `PerformanceAndConcurrencyBenchmarkTest.java`, automated test suites verify that the pipeline achieves zero crashes, sub-10ms inference latencies, 100% throughput under 10 concurrent burst threads, and accurate classification across safe benign and malicious test fixtures.

---

## TOPIC 09: Phishing Detection Methodology

### Q33. What is Prahar's Multi-Signal Composite Scoring Formula?
**Answer:** `[IMPLEMENTED]`  
Calculated in `MultiSignalScoringEngine.java`:
$$\text{Final Score} = \min\left(100, \max\left(S_{\text{floor}}, 0.50 \cdot S_H + 0.35 \cdot S_{ML} + 0.15 \cdot S_{TI}\right)\right)$$
Where:
- $S_H \in [0, 100]$: Normalized heuristic rule score from 30 deterministic checks.
- $S_{ML} \in [0, 100]$: Calibrated machine learning probability ($P \times 100$).
- $S_{TI} \in [0, 100]$: Threat intelligence reputation score.
- $S_{\text{floor}}$: Dynamic safety floor (e.g., set to 70–85 if brand impersonation or Punycode homoglyphs are verified).

### Q34. How does Prahar handle Internationalized Domain Name (IDN) Homoglyph attacks?
**Answer:** `[IMPLEMENTED]`  
Attackers register domains using Cyrillic or Greek characters that look identical to Latin characters (e.g., Cyrillic 'а' `U+0430` instead of Latin 'a' `U+0061`). In `UrlNormalizationUtil.java` and `SSRFProtectionService.java`, Prahar invokes `java.net.IDN.toASCII(host)`, converting visual homoglyphs into their canonical Punycode representation (`xn--...`). The heuristic engine flags `RULE_PUNYCODE_HOMOGLYPH` and applies a high risk penalty.

### Q35. How does the Fast-Path Whitelist function?
**Answer:** `[IMPLEMENTED]`  
In `PhishingRuleEngine.java`, a verified set of apex domains for national financial institutions and government portals (e.g., `sbi.co.in`, `onlinesbi.sbi`, `cybercrime.gov.in`, `rbi.org.in`) is maintained in an immutable lookup set. When a submitted URL matches these authentic apex domains with a valid protocol, the system returns an immediate `RiskScore = 0` (`SAFE`), bypassing costly external lookups.

### Q36. What is "Quishing" and how does Prahar detect it?
**Answer:** `[IMPLEMENTED]`  
Quishing (QR Phishing) involves weaponizing QR codes to redirect victims to phishing pages. In `QRCodeService.java`, Prahar uses the Google ZXing library to parse binary image matrices. It decodes the payload, classifies whether it is a web URL or a UPI payment URI (`upi://pay?pa=...`), runs SSRF checks, and automatically queues the extracted destination URL through the full detection pipeline.

---

## TOPIC 10: Database and API Integration

### Q37. What database schema migrations exist in Prahar?
**Answer:** `[IMPLEMENTED]`  
Located in `database/`:
- `migration_v2.sql`: Introduces financial transaction analysis tables (`financial_transactions`) and fraud incident reporting (`fraud_incidents`).
- `migration_v3.sql`: Adds real-time notifications schema (`notifications`, `notification_preferences`) and threat intelligence caching tables.
- `production_hardening.sql`: Indexes performance-critical columns (`normalized_url`, `created_at`, `client_ip`) and configures MySQL InnoDB buffer tuning.

### Q38. How is the database decoupled between academic viva mode and production?
**Answer:** `[IMPLEMENTED]`  
Via Spring Boot profiles:
- Default (`application.yml`): Uses `H2 In-Memory Database` (`jdbc:h2:mem:phishshield`), allowing examiners to run and verify the entire project without setting up external MySQL services.
- Production (`application-prod.yml`): Configures enterprise `MySQL 8.0` with HikariCP connection pooling (`maximum-pool-size: 20`, `leak-detection-threshold: 30000ms`).

### Q39. How do you prevent SQL Injection across the backend?
**Answer:** `[IMPLEMENTED]`  
Prahar strictly uses **Spring Data JPA and Hibernate ORM** with parameterized prepared statements. No raw dynamic string concatenation is permitted in repository queries. All user queries execute via named parameters or Spring Data derived queries (e.g., `findByNormalizedUrl(String url)`).

### Q40. What is HikariCP and why is it used?
**Answer:** `[IMPLEMENTED]`  
HikariCP is a high-performance JDBC connection pool library default in Spring Boot. It minimizes connection acquisition overhead through byte-code instrumentation and zero-overhead micro-optimizations, ensuring sub-millisecond database connection reuse during concurrent scan bursts.

---

## TOPIC 11: Authentication and Security Controls

### Q41. How does Prahar protect against Server-Side Request Forgery (SSRF)?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
Implemented in `SSRFProtectionService.java`:
1. The hostname is extracted and converted to canonical ASCII via `IDN.toASCII()`.
2. DNS resolution is performed to obtain all IP addresses via `InetAddress.getAllByName()`.
3. Each resolved IP is checked against forbidden IP ranges:
   - Loopback addresses (`127.0.0.0/8`, `::1`)
   - RFC-1918 Private networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
   - Link-local and cloud metadata addresses (`169.254.0.0/16`)
If any resolved address matches, an `SSRFSecurityException` is thrown before any network socket is opened.

### Q42. How does the Token-Bucket Rate Limiter work?
**Answer:** `[IMPLEMENTED]`  
In `RateLimitingFilter.java`, an in-memory token bucket is maintained per client IP. The filter allocates 60 tokens per minute for scanning endpoints and 15 tokens per minute for authentication endpoints. Each request consumes one token. If the bucket is exhausted, HTTP 429 Too Many Requests is returned with a `Retry-After: 60` header, preventing denial-of-service (DoS) attacks.

### Q43. How does the Sensitive Data Scrubber operate?
**Answer:** `[IMPLEMENTED]`  
In `SensitiveDataScrubber.java`, incoming text payloads are scrubbed prior to persistence or logging. It applies regex patterns matching credit card numbers and verifies them using the **Luhn Algorithm** ($O(N)$ checksum). Verified credit card sequences, CVVs, passwords, and 6-digit OTPs are redacted with asterisks (`****-****-****-1234`).

### Q44. How are uploaded image files validated against malicious polyglot attacks?
**Answer:** `[IMPLEMENTED]`  
In `ImageSecurityValidator.java`, the system inspects the first 16 bytes (magic numbers) of the uploaded binary file to ensure it matches real JPEG, PNG, or WebP headers. SVG uploads are explicitly rejected to prevent Stored Cross-Site Scripting (XSS) via embedded XML `<script>` payloads.

---

## TOPIC 12: Threat Intelligence Integration

### Q45. How is threat intelligence incorporated without introducing latency bottlenecks?
**Answer:** `[IMPLEMENTED]`  
In `ThreatIntelligenceService.java`, threat intelligence lookups are executed with an in-memory cache and a strict 1500ms timeout. If an external threat feed fails to respond within the deadline, the service gracefully returns a cached record or marks the intelligence signal as neutral ($S_{TI} = 0$), preventing external network stalls.

### Q46. What external threat intelligence sources does Prahar interface with?
**Answer:** `[IMPLEMENTED]`  
The architecture supports reputation lookups from VirusTotal, AbuseIPDB, and OpenPhish feeds. For local academic demonstration, deterministic mock feeds and verified test domains (`testsafebrowsing.appspot.com`, `malware-test.com`) simulate positive threat intelligence matches.

### Q47. What happens if a threat intelligence source reports a false positive?
**Answer:** `[IMPLEMENTED]`  
Threat intelligence contributes only **15%** ($0.15$) to the base composite score. A positive threat intelligence score will not automatically flag an otherwise pristine domain unless supported by heuristic or ML signals, or unless confirmed as a known active malware host.

---

## TOPIC 13: AI Assistant & Gemini Integration

### Q48. How is the Google Gemini model integrated into Prahar?
**Answer:** `[IMPLEMENTED]`  
In `GeminiClient.java`, the backend communicates with the Google Generative AI API (`gemini-1.5-flash`) via HTTP POST, passing system-grounded instructions that restrict the assistant to cybersecurity guidance, phishing incident analysis, and formal complaint drafting.

### Q49. How does the system function when the Gemini API key is missing or offline?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
In `ChatService.java`, if `gemini.api.key` is not configured, the assistant automatically routes queries to `RuleBasedNlpEngine.java`. This engine uses deterministic regex pattern matching, keyword extraction, and pre-compiled cyber crime playbooks to answer common queries (e.g., bank fraud reporting, KYC scams, UPI dispute steps) without making any external cloud API calls.

### Q50. How does Prahar mitigate LLM hallucinations in cybersecurity advice?
**Answer:** `[IMPLEMENTED]`  
The Gemini prompt template enforces strict grounding constraints: the model is instructed to provide only factual procedural guidance referencing official Indian cybercrime reporting portals (`cybercrime.gov.in`, National Toll-Free Number `1930`), and is forbidden from diagnosing or generating malicious exploit code.

---

## TOPIC 14: Testing and Evaluation

### Q51. What is the current automated test coverage across the repository?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
The project maintains **175 automated tests passing with 100% pass rate**:
1. **Spring Boot Backend:** 120 tests (`mvn test`) covering controllers, services, SSRF filters, rate limiters, multi-signal scoring, and concurrency benchmarks.
2. **FastAPI ML Service:** 35 tests (`pytest`) covering API contracts, feature extraction, edge cases, and inference benchmarks.
3. **React Frontend:** 10 Vitest unit specs verifying UI rendering, state, and API error wrappers.
4. **Browser Extension:** 10 integration specs (`test_extension.js`) verifying Manifest V3 background scripts and message passing.

### Q52. How did you test concurrency and performance under load?
**Answer:** `[IMPLEMENTED]`  
In `PerformanceAndConcurrencyBenchmarkTest.java`, a multi-threaded benchmark harness executes 10 concurrent threads submitting rapid scan requests against the backend. The test verifies that the system processes >100 scans/second with 0% data corruption, zero deadlocks, and clean connection pool recycling.

### Q53. How do you test the system without an active internet connection?
**Answer:** `[IMPLEMENTED]`  
All automated test suites use deterministic mocks (`Mockito` in Java, `unittest.mock` in Python) and local test fixtures (`media/demo_fixtures/`). No tests require live internet access, ensuring 100% reproducible test runs in isolated academic lab environments.

---

## TOPIC 15: Limitations, Weaknesses and Future Scope

### Q54. What are the main weaknesses of the current Prahar implementation?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
1. **No Dynamic JavaScript Execution:** Prahar does not run a headless browser sandbox (like Playwright/Puppeteer) to execute dynamic client-side DOM obfuscation or delayed redirects.
2. **Native OCR Dependency:** Real-time text extraction from screenshots requires Tesseract 5.x installed in the host OS PATH; on systems without it, the engine falls back to heuristic image analysis.
3. **In-Memory Default Database:** The academic development profile runs on H2; production deployment requires manual configuration of MySQL credentials in `application-prod.yml`.

### Q55. Which features are incomplete or partially verified?
**Answer:** `[IMPLEMENTED]`  
*Examiner Tough Question.*  
- External live threat intelligence is verified using local mock caches; live paid API accounts (VirusTotal Enterprise) were omitted to avoid recurring cloud charges.
- Real-time outbound SMTP email delivery requires an enterprise mail relay; notifications currently stream via in-app Server-Sent Events (SSE).

### Q56. How would you improve the project if given another 6 months?
**Answer:** `[CONCEPTUAL]`  
*Examiner Tough Question.*  
1. **Containerized Headless Sandbox:** Deploy an isolated Chromium microservice on Kubernetes to capture full DOM snapshots, evaluate post-redirect URLs, and detect HTML credential form harvesting.
2. **Multi-Modal Vision Transformer:** Replace Tesseract OCR with an embedded Vision-Language Model (like Florence-2 or ViT) to detect brand logo visual tampering directly in pixel space.
3. **Federated Threat Sharing:** Implement STIX/TAXII threat feed ingestion to automatically synchronize newly discovered phishing indicators with national cyber response centers.

### Q57. Does Prahar guarantee 100% detection of all phishing attacks?
**Answer:** `[CONCEPTUAL]`  
**No. In cybersecurity, no probabilistic detector can guarantee 100% protection.** Novel zero-day domains employing clean lexical names on trusted CDNs without prior reputation can achieve low heuristic risk scores. Prahar minimizes this window through multi-signal defense-in-depth, but user awareness and multi-factor authentication (MFA) remain essential layers.

### Q58. How do you handle adversarial attempts to evade ML feature extraction?
**Answer:** `[IMPLEMENTED]`  
Adversaries often attempt evasion by padding legitimate subdomains or appending benign tokens. Prahar counters this through:
1. Multi-signal score floors (e.g., brand tokens in unauthorized subdomains enforce a minimum score of 70 regardless of ML probability).
2. Entropy analysis that detects artificial token padding.
3. Canonical URL parsing using the Public Suffix List to identify the true registrable apex domain.

### Q59. What role does the Chrome Browser Extension play in the Prahar ecosystem?
**Answer:** `[IMPLEMENTED]`  
Located in `browser-extension/`: A lightweight Manifest V3 extension that monitors active tab navigations, queries the Spring Boot backend `/api/scans/url` endpoint in the background, and warns the user via browser popup alerts before they interact with suspicious destinations.

### Q60. How does Prahar ensure compliance with data privacy regulations (e.g., GDPR / DPDP Act)?
**Answer:** `[IMPLEMENTED]`  
1. IP addresses are anonymized/hashed in audit logs.
2. The `SensitiveDataScrubber.java` strips PANs, passwords, and payment card numbers before storage.
3. No personal cookies, browsing history, or private session tokens are transmitted or logged.

### Q61. Why is an explainable detection breakdown superior to a simple risk percentage?
**Answer:** `[IMPLEMENTED]`  
In enterprise cyber defense, an opaque risk number (e.g., "Score: 85") cannot be audited or acted upon by security analysts. Prahar provides an explicit list of triggered rules, entropy calculations, and forensic indicators, enabling analysts to verify why a domain was blocked and defend against false-positive legal challenges.

### Q62. What is your individual contribution to the technical codebase?
**Answer:** `[IMPLEMENTED]`  
*(Customize per candidate roles)*:  
Full-stack implementation encompassing Spring Boot REST microservices, the multi-signal mathematical scoring fusion engine, pre-resolution SSRF filters, Python FastAPI ML integration with 30-feature extraction, React 18 frontend dashboard, and automated test suites verifying 175 passing test cases.
