# Prahar AI 2.0 — Chatbot Architecture Document
**Real-Time Context-Aware Cybersecurity Intelligence & Incident Guidance Assistant**
*Status: Verified Production Architecture (Release 2.0)*

---

## 1. Executive Summary & Pipeline Overview

Prahar AI 2.0 upgrades the cybersecurity assistant into a hybrid, context-aware intelligence system powered by Google Gemini (OpenAI-compatible schema) backed by verified deterministic security rules, multi-signal phishing detection, and SSRF-hardened local execution.

### End-to-End Message Flow Architecture

```
React / Vite Frontend (PraharChatbot / ChatAssistantPage)
   │
   ▼ HTTP POST /api/chat (JSON Payload: message, history, scanContext)
ChatController.java
   │
   ▼ Input Sanitization & Entity Extraction
ChatService.java (sanitizeInput: PCI card redaction, URL extraction)
   │
   ▼ Multi-Turn Dialogue Tracking
DialogueContext.java (10-turn sliding window, slot tracking, topic shift detection)
   │
   ▼ Intent Classification
IntentClassifier.java (Deterministic heuristics + intent disambiguation)
   │
   ├─── If Provider Configured & Available (Google Gemini) ───────────┐
   │                                                                 │
   │   REST Client with Connection & Read Timeouts (15,000ms)         │
   │   Authorization: Bearer <AI_API_KEY> (Server-Side Only)         │
   │                                                                 │
   │   Function Calling / Tools Schema:                              │
   │   - `analyze_url`: Evaluates live URL via SSRF & RuleEngine     │
   │   - `explain_scan_result`: Fetches real database scan findings   │
   │                                                                 │
   │   Gemini Tool Execution Loop (Server-side validation)            │
   │   Final Natural Language Response Generation                   │
   │   Duplicate Assembly Elimination: actionChecklist = []          │
   │                                                                 │
   └─── If Provider Fails / Unconfigured (Transparent Fallback) ─────┘
       Deterministic Prahar Rule Engine & Contextual Guidances
       - General cyber safety definitions (OTP, 2FA, Phishing)
       - Financial fraud Golden Hour guidance (1930 / cybercrime.gov.in)
       - Active incident checklists (frozen for educational queries)
   │
   ▼ Unified Response Assembly (ChatResponse DTO)
Single Contract: { reply, isLiveAi, provider, suggestions, actionChecklist, entities }
   │
   ▼ Frontend Message Rendering
Clean Markdown rendering, dynamic follow-up chips, copy action, no internal labels.
```

---

## 2. Component Architecture & Responsibility Matrix

| Component | File Path | Architectural Responsibility | Status |
| :--- | :--- | :--- | :--- |
| **Frontend Chat Page** | [ChatAssistantPage.jsx](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/src/pages/ChatAssistantPage.jsx) | Full-page interactive chat interface, quick chips, copy support, clean metadata footer | `VERIFIED` |
| **Frontend Widget** | [PraharChatbot.jsx](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/src/components/PraharChatbot.jsx) | Floating global chatbot drawer across all pages with responsive states | `VERIFIED` |
| **Chat API Controller** | [ChatController.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/controller/ChatController.java) | REST endpoint (`/api/chat`, `/api/chat/status`), request mapping, validation | `VERIFIED` |
| **Chat Core Service** | [ChatService.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/ChatService.java) | Gemini provider orchestration, tool calling loop, context assembly, fallback generation | `VERIFIED` |
| **Dialogue Manager** | [DialogueContext.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/nlp/DialogueContext.java) | Multi-turn slot memory, pending details, turn history, entity accumulation | `VERIFIED` |
| **Intent Classifier** | [IntentClassifier.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/nlp/IntentClassifier.java) | Intent taxonomy, topic-change detection, slot-aware disambiguation | `VERIFIED` |
| **Entity Extractor** | [EntityExtractor.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/nlp/EntityExtractor.java) | Extraction of amounts, UPI/cards, OTP sharing, URLs, payment institutions | `VERIFIED` |
| **SSRF Protection** | [SSRFProtectionService.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/SSRFProtectionService.java) | Strict validation preventing target requests to localhost, loopback, private RFC1918, metadata | `VERIFIED` |
| **Rule Engine** | [RuleEngineService.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/RuleEngineService.java) | Multi-signal URL heuristic evaluation, brand impersonation, entropy, typosquatting | `VERIFIED` |

---

## 3. Gemini Function Calling Architecture

Gemini model (`gemini-1.5-flash` or user-specified model) is integrated via the OpenAI-compatible REST endpoint (`https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`) using server-side Bearer authorization.

### Supported Tools Specification

1. **`analyze_url(url: string)`**:
   - **Invocation trigger**: When a user pastes or requests analysis of a target web link.
   - **Server-side validation**:
     1. Validates string format and non-emptiness.
     2. Enforces SSRF boundary: rejects `127.0.0.1`, `localhost`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`.
     3. Calls [RuleEngineService.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/RuleEngineService.java) to evaluate lexical risk, domain entropy, brand impersonation, and threat indicators.
     4. Feeds real JSON evaluation findings back into conversation for Gemini to explain naturally to the user.
   - **Status**: `VERIFIED`

2. **`explain_scan_result(scan_id: integer)`**:
   - **Invocation trigger**: When a user queries why a specific scan was flagged (e.g. Scan #142).
   - **Server-side validation**:
     1. Validates positive integer identifier.
     2. Queries [URLScanRepository.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/repository/URLScanRepository.java).
     3. Returns verified database verdict, risk score, threat category, and detection explanation summary.
   - **Status**: `VERIFIED`

---

## 4. Single-Contract Response Assembly & Anti-Duplication Strategy

### Response Contract DTO ([ChatResponse.java](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/dto/response/ChatResponse.java))

```json
{
  "reply": "Markdown formatted string containing comprehensive guidance",
  "isLiveAi": false,
  "provider": "prahar_nlp",
  "followUpSuggestions": [
    "What should I do if I clicked it?",
    "Scan another URL or message"
  ],
  "actionChecklist": [],
  "extractedEntities": {
    "channel": "UPI"
  },
  "intent": "GENERAL_CYBER_SAFETY",
  "confidence": 0.95
}
```

### De-Duplication Rules
- **Rule 1 (Live AI responses)**: When `isLiveAi == true`, `response.setActionChecklist(Collections.emptyList())`. Gemini generates the complete answer; no canned checklists are attached.
- **Rule 2 (Educational queries in rule mode)**: Educational questions (`what is`, `define`, `explain`, `does https`, `how does`) return `Collections.emptyList()` for `actionChecklist`.
- **Rule 3 (Active Emergency Incidents)**: Only active fraud emergencies (`UNKNOWN_DEBIT`, `UPI_FRAUD`, `ATM_CARD_FRAUD`, `FAKE_SHOPPING_SCAM`, `OTP_DISCLOSURE`, `ACCOUNT_COMPROMISE`, `STOLEN_PHONE`, `SIM_SWAP`, `FAKE_BANK_APP`) generate structured `actionChecklist` items.
- **Rule 4 (Frontend Guard)**: [PraharChatbot.jsx](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/src/components/PraharChatbot.jsx) and [ChatAssistantPage.jsx](file:///d:/MCA%20notes/MINI%20PRO%20SIH/frontend/src/pages/ChatAssistantPage.jsx) only render the action checklist card when `actionChecklist.length > 0 && !isLiveAi`.

---

## 5. Security & Boundary Hardening

- **Credential Isolation**: AI API key is read solely from backend environment variable `AI_API_KEY` or `APP_AI_API_KEY`. It is never emitted in `/api/chat/status`, never rendered in frontend HTML/JS, and excluded from git tracking.
- **PCI-DSS Protection**: Credit and debit card numbers matching pattern `\b(?:\d[ -]*?){13,19}\b` are redacted to `[REDACTED_CARD_NUMBER]` in memory before logging or forwarding.
- **Prompt Injection Defense**: User messages are demarcated as untrusted input data; system directives mandate that user prompt overrides must be ignored.
- **SSRF Hardening**: All scanned web addresses pass through IP resolution and CIDR filtering before network probing or heuristic execution.
