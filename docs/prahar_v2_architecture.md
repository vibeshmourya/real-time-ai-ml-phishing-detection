# Prahar 2.0 — Architectural Upgrade & System Documentation
> **Tagline:** Detect. Understand. Stay Safe.  
> **System Scope:** AI/ML-based phishing detection, personal cybersecurity assistance, fraud-response management, and authorised financial activity monitoring.

---

## Executive Summary of Upgrades

Prahar 2.0 upgrades the existing platform into an authoritative, human-friendly personal digital safety assistant that helps citizens detect online threats, understand deceptive mechanisms, organize personal financial activity without pretending to have unauthorized banking access, and respond decisively to cyber fraud incidents within the crucial Indian **Golden Hour** window.

---

## 1. Audit & Root Cause Analysis of Previous Chatbot Behavior (Phase 1)

### Root Cause of Identical Responses:
1. **Disabled / Unconfigured Provider:** The system property `app.ai.provider` defaulted to `none` with empty API keys (`app.ai.api-key: ""`).
2. **Brittle Heuristic Fallback:** When `hasAiConfig` evaluated to `false`, the controller fell back to `generateOfflineFaqResponse()` in `ChatService.java`. That implementation contained only 5 static `if (lower.contains("why")) ... else if (lower.contains("clicked"))` blocks.
3. **No Intent or Slot Memory:** Queries about unauthorized debits, UPI fraud, lost phones, or SIM swaps fell through to the default branch, which returned the exact same generic welcome string regardless of user input.
4. **Disconnected Service Boundary:** The assistant was completely decoupled from scan history, transaction ledgers, and incident workflows.

---

## 2. Advanced NLP Prahar Assistant Pipeline (Phase 2)

```
[Raw User Input]
       │
       ▼
[NlpNormalizer] ──> Typo correction, Hinglish signal handling, currency standardization (₹/Rs./INR)
       │
       ▼
[EntityExtractor] ──> Regex & dictionary extraction (amount, channel, institution, direction, OTP/leak flags)
       │
       ▼
[DialogueContext] ──> Multi-turn slot memory, pending slot tracking (e.g., asking payment channel after debit)
       │
       ▼
[IntentClassifier] ──> 22-intent taxonomy with confidence scoring and follow-up disambiguation
       │
       ├─────────────────────────────────┬────────────────────────────────┐
       ▼                                 ▼                                ▼
[Live LLM Provider]             [Authoritative NLP Engine]         [Authorized Tool Calls]
(OpenAI/Gemini if configured)   (Contextual, slot-filled, offline)  (Scan explanation, txn summary,
                                                                    cybercrime complaint drafting)
```

### Supported Intents:
- `PHISHING_URL_ANALYSIS` & `EXPLAIN_SCAN`
- `USER_SCAN_HISTORY`
- `SUSPICIOUS_MESSAGE` & `SUSPICIOUS_QR`
- `UNKNOWN_DEBIT` & `UNKNOWN_CREDIT`
- `UPI_FRAUD` & `ATM_CARD_FRAUD`
- `OTP_DISCLOSURE` & `ACCOUNT_COMPROMISE`
- `STOLEN_PHONE` & `SIM_SWAP`
- `FAKE_BANK_APP`
- `TRANSACTION_SUMMARY`, `TRANSACTION_CATEGORIZE`, `TRANSACTION_DISCREPANCY`
- `FRAUD_REPORTING` & `COMPLAINT_STATUS`
- `GENERAL_CYBER_SAFETY` & `CLARIFICATION_ANSWER`

---

## 3. Personal Finance & Transaction Monitor (Phase 3)

### Data Integrity & Safety Guardrails:
- **No Fictitious Live Bank Feeds:** Prahar clearly labels data sources as `MANUAL`, `STATEMENT_CSV`, or `NOTIFICATION_SMS`.
- **SHA-256 Duplicate Fingerprinting:** Generated across `(userId, date, amount, merchant, reference)` to prevent double-counting when users re-upload CSV statements.
- **Explainable Anomaly Alerts:** Flags debits exceeding 3x the user's historical average, identical amounts within 24 hours (duplicate charge risk), or missing merchant details on large debits.
- **Decimal-Safe Calculations:** Uses Java `BigDecimal` with `RoundingMode.HALF_UP` to prevent floating-point rounding errors.

---

## 4. Fraud Incident Response Centre (Phase 4)

### Indian Cybercrime Protocol Integration:
- **National Cybercrime Helpline:** Prominent access to **1930** (24x7 helpline).
- **National Portal Submission:** Direct integration guidance for [https://cybercrime.gov.in/](https://cybercrime.gov.in/).
- **Formal Complaint Generator:** Automatically drafts formal dispute letters quoting:
  - RBI Circular on Limiting Customer Liability in Unauthorized Electronic Banking Transactions (`RBI/2017-18/15`).
  - Relevant provisions of the Information Technology Act 2000 (`Section 43A`, `Section 66C`, `Section 66D`).

---

## 5. Personal Mobile Safety (Phase 5)

### Guided Incident Containment:
1. **Lost or Stolen Device:**
   - SIM suspension numbers for Indian carriers (Jio, Airtel, Vi, BSNL).
   - Official IMEI blacklisting via the Central Equipment Identity Register ([ceir.gov.in](https://www.ceir.gov.in/)).
   - Remote wipe via Google Find My Device or Apple iCloud.
2. **SIM Swap Fraud:** Immediate telecom carrier verification and bank account freeze protocols.
3. **Malicious APK / Screen Share:** Airplane mode isolation, Safe Mode boot procedures, and Device Admin revocation.

---

## 6. Android Platform & SMS Integration Feasibility (Phase 3.1 & 5)

### Regulatory and Technical Analysis:
- **Google Play SMS & Call Log Policy:** Google Play strictly restricts `READ_SMS` and `RECEIVE_SMS` permissions to default SMS apps. Distribution policy prohibits general finance or security apps from broad background SMS interception.
- **Prahar 2.0 Feasibility Architecture:**
  - **Implemented Method:** On-device notification pasting and file statement upload (zero elevated OS permissions required; complies fully with privacy laws and app store policies).
  - **Android Companion App (Future):** Implemented via `NotificationListenerService` with explicit, granular per-app consent (e.g. only listening to verified bank sender handles like `VK-HDFCBK` or `AX-SBIINB`), ensuring no personal chat messages are touched.

---

## 7. Automated Testing & Verification (Phase 9)

### Backend Test Results (Maven surefire):
- **Total Tests Run:** 30
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Test Suites:**
  - `NlpEngineTest`: Verifies typo normalization, entity extraction, multi-turn slot tracking, and intent classification.
  - `ChatServiceTest`: Verifies offline NLP guidance, scan explanation wiring, slot filling, and 1930 helpline emergency containment.
  - `SsrfProtectionServiceTest`: Verifies blocking of loopback addresses, private RFC-1918 subnets, cloud metadata endpoints, and DNS rebinding.
  - `URLScanControllerTest`: Verifies URL scanning endpoints and input validation.
  - `AuthControllerTest` & `JwtTokenProviderTest`: Verifies JWT issuance, revocation, and per-user isolation.

### Frontend Test Results (Vite build):
- **Modules Transformed:** 1,595
- **Output:** Clean bundle generated in `dist/` with 0 syntax or JSX errors.

---

## 8. Database Migrations (Phase 10)

Located at `database/migration_v2.sql`:
- `financial_transactions`: Stores user-isolated transaction records with decimal-safe amounts, status enum, category enum, and SHA-256 fingerprint constraint.
- `fraud_incidents`: Stores incident workflow state, status enum, checklist JSON, and external complaint references.
