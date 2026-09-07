# System Architecture & Technical Design

## 1. Executive Summary

PhishShield is a multi-tier, high-throughput cybersecurity platform engineered to protect internet users from phishing campaigns, social engineering attacks, and deceptive digital assets.

Unlike traditional static blacklists or binary ML classifiers, PhishShield implements a **Hybrid Detection & Cyber Awareness Engine**:
- **Deterministic Rule Engine & SSRF Shield** (sub-millisecond evaluation)
- **High-Dimensional Machine Learning Service** (lexical, domain, and behavioral feature inference)
- **Explainable AI (XAI) Synthesis** (human-readable risk factors)
- **Contextual Incident Response & Multimedia Education** (dynamic video/audio guides)

---

## 2. Component Topology

```text
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|  +---------------------------+  +--------------------------+  +----------------+  |
|  |  React Web App (SPA)      |  | Chrome Extension (MV3)   |  | API Consumers  |  |
|  |  - Public URL Scanner     |  | - Active Tab Inspector   |  | (Third-party)  |  |
|  |  - Threat Visualizer      |  | - Real-time Warning Badge|  |                |  |
|  |  - Incident Wizard        |  | - Low-overhead Payload   |  |                |  |
|  +-------------+-------------+  +------------+-------------+  +-------+--------+  |
+----------------|-----------------------------|------------------------|-----------+
                 |                             |                        |
                 +-----------------------------+------------------------+
                                               | HTTPS (TLS 1.3) / JSON
                                               v
+-----------------------------------------------------------------------------------+
|                        SPRING BOOT BACKEND APPLICATION                            |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                             Security & Gateway                              |  |
|  |  - JWT Stateless Auth Filter     - Rate Limiter       - CORS & CSRF Config  |  |
|  |  - SSRF Protection Shield        - Request Validator  - Global Exception    |  |
|  +---------------------------------------+-------------------------------------+  |
|                                          |                                        |
|  +---------------------------------------v-------------------------------------+  |
|  |                             Application Services                            |  |
|  |  - URLScanService        - RuleEngineService        - ThreatDetectionService|  |
|  |  - UserReportService     - PreventionContentService - SecurityAlertService  |  |
|  |  - DashboardService      - AuditLogService          - AuthService           |  |
|  +-------------------+-----------------------------------+---------------------+  |
|                      |                                   |                        |
+----------------------|-----------------------------------|------------------------+
                       | Internal REST                     | Spring Data JPA
                       v                                   v
+------------------------------------+   +------------------------------------------+
|      PYTHON ML MICROSERVICE        |   |             MYSQL 8.0 DATABASE           |
|                                    |   |                                          |
|  - FastAPI HTTP Engine             |   |  - users & roles (RBAC)                  |
|  - Feature Extraction Pipeline     |   |  - url_scans & threat_detections         |
|  - Lexical, Domain & NLP Extractors|   |  - threat_indicators & model_predictions |
|  - XGBoost & Random Forest Ensembles|  |  - user_reports & security_alerts        |
|  - SHAP / Feature Explainer        |   |  - prevention_content & audit_logs       |
+------------------------------------+   +------------------------------------------+
```

---

## 3. Threat Processing Lifecycle

```text
[Input URL / Message / Email]
           |
           v
[SSRF Shield & Protocol Validation] ---> (Private IP / Invalid Scheme) ---> [BLOCK / 400 Bad Request]
           | (Valid Public URL)
           v
[URL Canonicalization & Normalization]
           |
           +---------------------------------------------+
           |                                             |
           v                                             v
[Deterministic Rule Engine]                   [Python ML Microservice]
  - IP Address in host                          - Lexical features
  - Punycode / Homoglyph check                  - Entropy calculation
  - Suspicious TLD detection                    - Structural ratios
  - Brand impersonation heuristics              - ML Probability (0.0 to 1.0)
  - Keyword density & entropy                   - Model Feature Importance
           |                                             |
           +----------------------+----------------------+
                                  |
                                  v
                   [Hybrid Score Fusion Engine]
                                  |
            Final Risk Score = (w_ml * S_ml) + (w_rule * S_rule)
                                  |
                                  v
                   [Classification & XAI Mapping]
                     - SAFE (0 - 39)
                     - SUSPICIOUS (40 - 69)
                     - HIGH_RISK (70 - 100)
                                  |
                                  v
                 [Threat Category Classification]
         (Banking, Credential Theft, KYC, Shopping, etc.)
                                  |
                                  v
                   [Contextual Prevention Match]
                 - 30-60s Targeted Safety Video
                 - Audio Warning Narration
                 - "I Already Clicked" Checklist
                                  |
                                  v
                 [Persist Audit Log & Scan Record]
                                  |
                                  v
                  [JSON Response Delivered to UI]
```

---

## 4. Scalability & Resilience Considerations

1. **Stateless Backend Architecture**: Spring Boot runs statelessly with JWT tokens, allowing horizontal scaling behind load balancers (e.g. NGINX, AWS ALB).
2. **Circuit Breaker / Fallback for ML Service**: If the Python ML microservice is unreachable or times out, the backend falls back gracefully to the deterministic Rule Engine with a transparent indicator flag (`fallback_mode=true`).
3. **Database Indexing**: All high-cardinality search fields (`normalized_url`, `domain`, `created_at`, `user_id`, `threat_category`) are indexed with B-Tree indexes for sub-5ms query times.
4. **Zero-Trust SSRF Protection**: Any outbound DNS resolution or HTTP fetching explicitly blocks RFC 1918 private ranges, AWS/GCP metadata endpoints (`169.254.169.254`), and loopback interfaces before socket initiation.
