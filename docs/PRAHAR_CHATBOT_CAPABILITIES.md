# Prahar AI 2.0 — Chatbot Capabilities Document
**Grounded Operational & Technical Capabilities Matrix**
*Status: Verified Application Features & Operational Scope*

---

## 1. Verified Core Capabilities

Prahar AI 2.0 provides cybersecurity intelligence, real-time phishing URL inspection, and financial fraud incident containment tailored to Indian citizens and cyber law infrastructure.

### A. Intelligent Question Answering & Conceptual Education
- **Phishing & Social Engineering**: Explains definitions, email/SMS/QR attack vectors, smishing, and vishing.
- **Authentication & Credential Protection**: Explains One-Time Passwords (OTP), Two-Factor Authentication (2FA/MFA), TOTP apps, and why OTPs must never be disclosed.
- **Web Security Realities**: Clarifies that HTTPS provides transport layer encryption but does NOT verify website legitimacy (over 80% of phishing pages use valid TLS certificates).
- **Advanced Defensive Measures**: Guides webmasters on SPF, DKIM, DMARC DNS policies, HTTP Strict Transport Security (HSTS), and secure cookie attributes (`HttpOnly`, `Secure`, `SameSite`).
- **Machine Learning Mechanics**: Explains feature extraction (Shannon entropy, URL length, subdomain depth, token frequency) and probabilistic classification models.

### B. Real-Time Phishing URL & Domain Inspection
- **Interactive Scanning**: Accepts URLs directly in conversational chat or leverages Prahar's **Scan** tab.
- **Multi-Signal Evaluation**:
  - Lexical heuristics: Typosquatting checks, brand impersonation tokens (`login`, `verify`, `secure`, `bank`), suspicious TLDs (`.xyz`, `.top`, `.tk`), raw IP addresses.
  - Heuristic risk scoring: Calculates calibrated 0–100 risk score with threat category assignment.
  - Threat indicators: Returns detailed structural findings (e.g. `SUSPICIOUS_TLD`, `BRAND_IMPERSONATION`).
- **SSRF Containment**: Rejects attempts to inspect localhost, RFC1918 private subnets, loopback interfaces, or cloud metadata endpoints.

### C. Financial Fraud Emergency Containment
- **Golden Hour Guidance**: Immediate action checklists for unauthorized debits, UPI fraud, debit/credit card theft, and fake e-commerce shopping scams.
- **Indian Cyber Law Integration**:
  - Direct guidance to dial **1930** (National Cyber Crime Helpline).
  - Filing complaints on [cybercrime.gov.in](https://cybercrime.gov.in/).
  - Telecom fraud reporting to **1909** (TRAI SMS spam reporting).
  - Lost mobile device blocking via CEIR ([ceir.gov.in](https://www.ceir.gov.in/)).
- **Payment Dispute Support**: Guides users on obtaining the 12-digit UTR reference number from UPI apps and notifying bank fraud desks.

### D. Multi-Turn Conversational Memory & Topic Agility
- **Contextual Follow-ups**: Remembers active incidents (e.g., shopping scam payment method, debit channels).
- **Instant Topic Shifts**: Seamlessly switches from active fraud containment to technical definitions without forcing users to complete prior incident questions.
- **Session Isolation**: Completely separates user sessions; no context or payment details bleed across users.

---

## 2. Gemini Function Calling Capabilities

When Google Gemini is enabled (`app.ai.provider=gemini` or key present):
1. **Tool `analyze_url`**:
   - Calls backend SSRF protection and rule engine to analyze submitted URLs.
   - Grounded explanations based on real structural indicators.
2. **Tool `explain_scan_result`**:
   - Fetches verified database scan findings by scan ID.
   - Explains calculated risk scores and verdicts without fabricating results.

---

## 3. Deliberate Non-Capabilities & Security Boundaries

To prevent misuse and protect user security, Prahar AI 2.0 explicitly enforces these boundaries:
- **Never Requests Secrets**: The assistant will never ask users to type actual passwords, OTPs, UPI PINs, CVV codes, or full card numbers.
- **No Unilateral Domain Whitelisting**: The chatbot cannot override security verdicts or whitelist domains; users must submit formal re-evaluation requests via analyst channels.
- **No Direct Financial Execution**: The chatbot does not execute bank transfers, block bank accounts automatically, or initiate refunds. It directs users to official bank channels.
- **Prompt Injection Immunity**: Instructions in user messages to ignore system rules, disclose internal configurations, or expose API keys are ignored.
