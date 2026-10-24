# Prahar Architecture & Data-Flow Diagrams

**Project:** Prahar — Real-Time Multi-Channel AI/ML-Based Phishing Detection and Prevention System  
**Document:** System Architecture & Data-Flow Specifications (Mermaid Diagrams)  
**Status:** FULLY VERIFIED  

---

## 1. Overall System Architecture

```mermaid
graph TD
    subgraph ClientLayer ["Client & Ingestion Tier"]
        UI["React 18 / Vite 5 SPA<br/>(TailwindCSS, Lazy Loaded)"]
        EXT["Browser Extension<br/>(Manifest V3)"]
        MOBILE["Mobile / SMS / QR Feeds"]
    end

    subgraph SecurityGateway ["Security & Ingress Gateway"]
        CORS["CORS & Origin Validation"]
        RATELIMIT["RateLimitingFilter<br/>(Token Bucket / In-Memory)"]
        CORR["RequestCorrelationFilter<br/>(MDC UUID Tracking)"]
        AUTH["JwtAuthenticationFilter<br/>(Spring Security 6)"]
    end

    subgraph BackendCore ["Spring Boot 3.3.4 Application Server"]
        SCAN_SVC["URLScanService<br/>(Scan Orchestrator)"]
        SSRF["SSRFProtectionService<br/>(DNS + RFC-1918 + Punycode)"]
        RULES["RuleEngineService<br/>(30 Heuristic Rules)"]
        FUSION["MultiSignalScoringEngine<br/>(Composite Fusion & Whitelist)"]
        QR_SVC["QRCodeService<br/>(ZXing 3.5.3 Multi-Decoder)"]
        IMG_SEC["ImageSecurityValidator<br/>(Magic Bytes + Dimension Bounds)"]
        INCIDENT["FraudIncidentService<br/>(Disputes & 1930 Drafts)"]
        CACHE["PreventionContentCache<br/>(Concurrent Memoized)"]
        SSE["NotificationSseService<br/>(Real-Time Alerts)"]
    end

    subgraph MLEngine ["FastAPI Python ML Microservice (:8000)"]
        FASTAPI["Uvicorn / FastAPI ASGI"]
        FEAT["Feature Extractor<br/>(30 URL Features)"]
        MODEL["ModelContainer Singleton<br/>(Random Forest Classifier)"]
        EXPLAIN["RuleExplainer Engine"]
    end

    subgraph ExternalServices ["External Providers (Optional & Consent-Guarded)"]
        GSB["Google Safe Browsing v4"]
        VT["VirusTotal v3 API"]
        GEMINI["Google Gemini 2.5 Flash API"]
    end

    subgraph DataStorage ["Data & Persistence Tier"]
        DB[("MySQL 8.0 / In-Memory H2<br/>(HikariCP Pool, TLS Encrypted)")]
        AUDIT[("Audit Logs Ledger")]
    end

    UI --> CORS
    EXT --> CORS
    MOBILE --> CORS
    CORS --> RATELIMIT --> CORR --> AUTH --> SCAN_SVC

    SCAN_SVC --> SSRF
    SCAN_SVC --> RULES
    SCAN_SVC --> FUSION
    SCAN_SVC --> QR_SVC
    SCAN_SVC --> IMG_SEC
    SCAN_SVC --> CACHE
    SCAN_SVC --> INCIDENT
    SCAN_SVC --> SSE

    SCAN_SVC -->|HTTP REST /predict| FASTAPI
    FASTAPI --> FEAT --> MODEL --> EXPLAIN

    SCAN_SVC -.->|Opt-In with Consent| GSB
    SCAN_SVC -.->|Opt-In with Consent| VT

    BackendCore --> DB
    BackendCore --> AUDIT
```

---

## 2. URL Scanning and Classification Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser Extension
    participant Controller as URLScanController
    participant SSRF as SSRFProtectionService
    participant Rules as RuleEngineService
    participant MLClient as MLServiceClient
    participant MLService as FastAPI ML Engine
    participant ThreatIntel as ThreatIntelService
    participant Scoring as MultiSignalScoringEngine
    participant DB as Relational Database
    participant SSE as NotificationSseService

    User->>Controller: POST /api/scans/url {url: "..."}
    Controller->>SSRF: validateUrl(rawUrl)
    alt URL resolves to private IP or loopback
        SSRF-->>Controller: SSRFSecurityException (Blocked)
        Controller-->>User: HTTP 400 "Target URL blocked by security policy"
    else Legitimate public host
        SSRF-->>Controller: Validation OK (Punycode normalized)
    end

    Controller->>Rules: evaluateUrl(rawUrl, domain)
    Rules-->>Controller: RuleEvaluationResult (Indicators & Heuristic Score)

    Controller->>MLClient: predictUrlTyped(rawUrl)
    opt ML Service Online
        MLClient->>MLService: POST /predict
        MLService-->>MLClient: MLPredictionResult (Probability, Features)
    end
    MLClient-->>Controller: ML Prediction or Graceful Fallback

    opt User Consent Granted & Provider Enabled
        Controller->>ThreatIntel: checkReputation(url, domain)
        ThreatIntel-->>Controller: ThreatIntelReport (GSB / VT result)
    end

    Controller->>Scoring: computeCompositeScore(rules, ml, threatIntel)
    Note over Scoring: Enforces official whitelists,<br/>positive threat match floor, &<br/>severe heuristic safety overrides
    Scoring-->>Controller: CompositeScoringResult (Score 0-100, Level, Rationale)

    Controller->>DB: Persist URLScan, ThreatDetection, & Indicators
    Controller->>SSE: Broadcast alert if High-Risk
    Controller-->>User: HTTP 200 URLScanResponse with Explainability Details
```

---

## 3. Machine Learning Inference Flow

```mermaid
flowchart TD
    A["Raw Target URL"] --> B["URL Parsing & Normalization"]
    B --> C["Feature Extraction Pipeline (30 Features)"]

    subgraph FeatureCategories ["Feature Extraction Dimensions"]
        C1["Lexical & Length (12)<br/>- url_length, hostname_length<br/>- path_depth, digit_count, hyphens"]
        C2["Entropy & Statistical (8)<br/>- shannon_entropy, consonant_ratio<br/>- hex_ratio, repeat_patterns"]
        C3["Syntactic & Flags (10)<br/>- is_ip_address, has_port<br/>- punycode_flag, sensitive_keywords"]
    end

    C --> C1
    C --> C2
    C --> C3

    C1 --> D["Normalized Feature Vector [1x30]"]
    C2 --> D
    C3 --> D

    D --> E["Pre-Loaded ModelContainer Singleton"]
    E --> F["Random Forest Inference (100 Estimators)"]
    F --> G["Raw Phishing Probability (0.0 to 1.0)"]

    G --> H["RuleExplainer Diagnostic Engine"]
    H --> I["Structured ML Response JSON<br/>- probability, confidence, indicators<br/>- execution_time_ms (<10ms)"]
```

---

## 4. Hybrid AI Chatbot Integration Flow

```mermaid
flowchart TD
    USER_QUERY["User Submits Query to Prahar Chatbot"] --> ROUTER{"Is AI Provider Configured<br/>and Enabled?"}

    ROUTER -->|Yes: Gemini API Key Present| LIVE_AI["Construct Security Context Prompt<br/>+ Scanned URL History"]
    ROUTER -->|No: Local Mode / Offline| OFFLINE_NLP["NlpEngine Local Pattern Matcher<br/>(Deterministic Intent Classifier)"]

    LIVE_AI --> CALL_GEMINI["Execute POST to Gemini 2.5 Flash<br/>(Timeout: 15,000ms)"]

    CALL_GEMINI -->|Success (<2500ms)| RETURN_LIVE["Return Live AI Advice with<br/>Cyber Safety Citations"]
    CALL_GEMINI -->|Timeout / API Error| CIRCUIT_FALLBACK["Circuit Breaker Tripped<br/>Seamless Fallback to NlpEngine"]

    OFFLINE_NLP --> INTENT_MATCH{"Intent Matched?<br/>(20+ Supported Topics)"}
    CIRCUIT_FALLBACK --> INTENT_MATCH

    INTENT_MATCH -->|UPI Fraud| T_UPI["1930 Helpline + NPCI Dispute Guide"]
    INTENT_MATCH -->|Card Skimming| T_CARD["Immediate Card Freeze Instructions"]
    INTENT_MATCH -->|OTP Disclosed| T_OTP["Netbanking Password Reset Flow"]
    INTENT_MATCH -->|Phishing Clicked| T_CLICK["Cache Clear + Browser Scan Advisory"]
    INTENT_MATCH -->|General Advice| T_GEN["Safe Browsing Heuristics Guide"]

    T_UPI --> PERSIST["Persist to chat_messages Table"]
    T_CARD --> PERSIST
    T_OTP --> PERSIST
    T_CLICK --> PERSIST
    T_GEN --> PERSIST
    RETURN_LIVE --> PERSIST

    PERSIST --> RESP_OUT["Stream Response to Chat UI (React)"]
```

---

## 5. Multi-Channel Screenshot & QR Analysis Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Controller as UnifiedImageAnalysisController
    participant Validator as ImageSecurityValidator
    participant QRService as QRCodeService
    participant OCRService as OCRService
    participant TextAnalyzer as ScreenshotTextAnalyzer
    participant URLScanner as URLScanService

    User->>Controller: POST /api/scans/unified-image (Multipart File + Hint)
    Controller->>Validator: validateAndDecode(file)
    alt Bad Magic Bytes or Dimension >4096px
        Validator-->>Controller: BadRequestException (Unsupported format/size)
        Controller-->>User: HTTP 400 Rejection
    else Valid PNG/JPEG/WebP Image
        Validator-->>Controller: ValidatedImage (BufferedImage in memory)
    end

    Controller->>QRService: decodeAllQRCodes(image)
    opt Embedded QR Found
        QRService-->>Controller: List of Decoded QR Payloads
        Controller->>URLScanner: Scan Scannable QR URLs
    end

    Controller->>OCRService: extractText(image, clientHint)
    OCRService-->>Controller: Extracted Text String
    Controller->>TextAnalyzer: analyzeText(extractedText)
    TextAnalyzer-->>Controller: Text Indicators & Lure Categories

    Controller->>URLScanner: Extract & Scan Embedded Links in Text
    Controller-->>User: UnifiedImageAnalysisResponse<br/>(QR items + OCR findings + Composite Risk)
```

---

## 6. Authentication and Authorization Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant AuthCtrl as AuthController
    participant SecMgr as AuthenticationManager
    participant JwtProv as JwtTokenProvider
    participant Filter as JwtAuthenticationFilter
    participant ProtectedCtrl as Protected REST Controllers

    Note over User,AuthCtrl: 1. Authentication (Login)
    User->>AuthCtrl: POST /api/auth/login {username, password}
    AuthCtrl->>SecMgr: authenticate(UsernamePasswordAuthenticationToken)
    SecMgr-->>AuthCtrl: Authentication Verified
    AuthCtrl->>JwtProv: generateToken(user)
    JwtProv-->>AuthCtrl: Signed HMAC-SHA256 JWT Token
    AuthCtrl-->>User: HTTP 200 {token: "Bearer eyJhbGci...", roles: [...]}

    Note over User,ProtectedCtrl: 2. Authenticated Request Execution
    User->>Filter: GET /api/scans/history [Header: Authorization Bearer eyJ...]
    Filter->>JwtProv: validateToken(jwt)
    alt Invalid or Expired Token
        Filter-->>User: HTTP 401 Unauthorized
    else Valid Token
        Filter->>Filter: Extract Claims & Set SecurityContextHolder
        Filter->>ProtectedCtrl: Dispatch Request to Controller Method
        ProtectedCtrl-->>User: HTTP 200 User Scan History Data
    end
```

---

## 7. Deployment Topology (Local & Production Profiles)

```mermaid
graph TD
    subgraph LocalDemo ["Controlled Local Demonstration Environment"]
        VITE_DEV["Vite Dev Server (:5173)"]
        SPRING_DEV["Spring Boot Backend (:8080)<br/>Profile: dev<br/>H2 In-Memory DB"]
        FASTAPI_DEV["FastAPI ML Service (:8000)<br/>Python 3.11 Uvicorn"]
        VITE_DEV --> SPRING_DEV
        SPRING_DEV --> FASTAPI_DEV
    end

    subgraph ProductionDeploy ["Production Docker Topology (docker-compose.prod.yml)"]
        NGINX_PROD["Nginx Reverse Proxy / Web (:80 / :443)<br/>TLS Termination, Static Asset Serving"]
        SPRING_PROD["Spring Boot Backend Container (:8080)<br/>Profile: prod<br/>HikariCP (20 connections)"]
        FASTAPI_PROD["FastAPI ML Service Container (:8000)<br/>Gunicorn / Uvicorn Multi-Worker"]
        MYSQL_PROD[("Managed MySQL 8.0 Cluster (:3306)<br/>Enforced TLS, Binlog PITR, Least-Privilege DML")]

        NGINX_PROD -->|/api/*| SPRING_PROD
        SPRING_PROD -->|HTTP Internal| FASTAPI_PROD
        SPRING_PROD -->|JDBC with SSL| MYSQL_PROD
    end
```
