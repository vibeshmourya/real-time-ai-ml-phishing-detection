# PRAHAR GEMINI INTEGRATION & CHATBOT AUDIT REPORT

**Project:** Prahar — Real-Time AI/ML-Based Phishing Detection & Personal Financial Security System  
**Audit Scope:** AI Chatbot Service, LLM Connectivity, Provider Configuration, Dialogue State Tracking, and Indian Incident Response  
**Status:** Audit Completed & Fully Remediated  

---

## 1. Executive Summary

A comprehensive forensic audit of the Prahar AI Chatbot revealed that the chatbot's reported inconsistency, loss of context, repetitive checklist output, and inability to handle follow-up queries were caused by structural design gaps across four layers:

1. **Provider Inactivity & Endpoint Incompatibility:** The Google Gemini integration defaulted to `AI_PROVIDER=none`. When activated with a key, it dispatched requests to an OpenAI-compatible endpoint rather than Google's native Generative Language v1beta `:generateContent` endpoint using header `x-goog-api-key`.
2. **Context Loss & Lack of Incident State Machine:** Dialogue history was reconstructed only as generic turns without an accumulated `IncidentState`. Multi-turn interactions (e.g., clicking a link $\rightarrow$ entering bank details) failed to maintain the confirmed incident profile, causing the chatbot to repeatedly ask the user for facts they had already disclosed.
3. **Overly Restrictive Follow-Up Classifier:** Follow-up query detection was constrained to an exact list of 20 literal phrases. Common phrasing such as *"Will changing my password help?"*, *"What if I entered my details?"*, or *"How can I report it?"* failed to be detected as follow-ups, causing the engine to misclassify follow-ups as ambiguous or unhandled queries.
4. **Repetitive Action Checklists:** The rule-based engine unconditionally appended a static 5-bullet checklist to conversational responses, including simple definitions like *"What is phishing?"*, leading to a robotic user experience.

All identified root causes have been systematically addressed with a dedicated `GeminiService`, native Google Generative Language v1beta dual-protocol client, structured `IncidentState` tracking, expanded conversational intent classifier, and adaptive response conciseness.

---

## 2. Root Cause Analysis & Forensic Findings

| Component / File | Identified Flaw | Impact on User Experience |
| :--- | :--- | :--- |
| `backend/src/main/resources/application.yml` | `app.ai.provider` defaulted to `none`; `api-key` was empty; `gemini-1.5-flash` model mapping was absent. | The chatbot never reached Gemini in standard deployments; it silently ran the rule engine without notifying operators or users. |
| `backend/src/main/java/com/phishshield/service/ChatService.java` | Only supported OpenAI completion protocol (`https://generativelanguage.googleapis.com/v1beta/openai/chat/completions` with `Bearer <key>`). | Standard Google Gemini API keys failed with HTTP 401 or HTTP 404 when projects lacked OpenAI-compatibility flags. |
| `backend/src/main/java/com/phishshield/service/GeminiService.java` | Did not exist. All HTTP logic was bundled into `ChatService.java` with insufficient error categorization and lack of active probe verification. | Inability to distinguish between `UNCONFIGURED`, `MISSING_CREDENTIALS`, `CONFIGURED_NOT_TESTED`, `PROVIDER_AVAILABLE`, and `PROVIDER_ERROR`. |
| `backend/src/main/java/com/phishshield/nlp/DialogueContext.java` | Lacked structured `IncidentState` tracking; only stored raw entities and last intent. | When a user reported an incident over 2–3 turns, previous incident flags were lost, triggering repetitive questionnaire prompts. |
| `backend/src/main/java/com/phishshield/nlp/IntentClassifier.java` | Exact string matching in `isFollowUpQuery` missed contextual inquiries. `account_type` and `status` slots had no resolution handlers. | Questions like *"Will changing my password help?"* or reporting a caller claimed to be from SBI fell through to `AMBIGUOUS_QUERY`. |
| `backend/src/main/java/com/phishshield/service/ChatService.java` (Response Builders) | Repetitive checklists appended to simple queries; lacked direct answers for password change effectiveness and official reporting. | Users received unhelpful generic lectures rather than direct, actionable answers. |

---

## 3. Before vs. After Code Path Comparison

### 3.1 AI Provider Invocation Architecture

#### Before:
```
User Message -> ChatService -> OpenAI Endpoint (Bearer Token)
                                   |
                     (Fails on standard Gemini keys)
                                   |
                             Generic Fallback
```

#### After:
```
User Message -> ChatService -> GeminiService
                                   |
                ---------------------------------------
               |                                       |
      Native Google v1beta                      OpenAI Proxy
  (:generateContent + x-goog-api-key)      (/openai/chat/completions)
               |                                       |
                ---------------------------------------
                                   |
                         Success / Fail-Safe
                                   |
                     [If Offline / Key Missing]
                                   |
              Context-Aware Verified Prahar Safety Engine
                   (with Structured IncidentState)
```

### 3.2 Multi-Turn Incident Context Retention

#### Before:
- **Turn 1:** *"I gave my OTP to a caller"* $\rightarrow$ Assistant: *"Which type of account was it for?"*
- **Turn 2:** *"They said they were from SBI"* $\rightarrow$ Classifier: `AMBIGUOUS_QUERY` (Missing slot resolution). Assistant: *"Could you share a few more details about what you're dealing with?"* (Context lost!).

#### After:
- **Turn 1:** *"I gave my OTP to a caller"* $\rightarrow$ `IncidentState: otpOrPinShared=true`, `incidentType=OTP_PIN_DISCLOSED`. Assistant asks for account type.
- **Turn 2:** *"They said they were from SBI"* $\rightarrow$ `IncidentState: institution=SBI`. Classifier detects `OTP_DISCLOSURE` follow-up. Assistant advises:
  1. Immediately contact SBI 24x7 customer care to freeze NetBanking and debit cards.
  2. Report to **1930 Cyber Helpline** to trigger fund freezing in the Golden Hour.
  3. Reset all banking passwords from a clean device.

---

## 4. Security & Secret Protection Verification

1. **Header Sanitization:** API keys are never appended to request URLs or logs.
2. **Error Scrubbing:** `GeminiService.sanitizeError()` redacts any pattern matching tokens, API keys, or bearer credentials before logging or exposing error diagnostics to clients.
3. **Card Masking:** `ChatService.sanitizeInput()` uses regex `SENSITIVE_CARD_PATTERN` to redact full card numbers before NLP analysis or prompt injection.
4. **Environment Isolation:** Secrets are read strictly from environment variables (`GEMINI_API_KEY`, `AI_PROVIDER`, `AI_MODEL`). No credentials exist in source code, committed files, or test fixtures.

---

## 5. Verification Results

| Test Suite | Total Tests | Passed | Failed | Status |
| :--- | :--- | :--- | :--- | :--- |
| `com.phishshield.GeminiIntegrationTest` | 6 | 6 | 0 | **PASSED** |
| `com.phishshield.ChatServiceTest` | 82 | 82 | 0 | **PASSED** |
| `com.phishshield.PraharChatbotScenariosTest` | 8 | 8 | 0 | **PASSED** |
| `com.phishshield.ChatHistoryTest` | 4 | 4 | 0 | **PASSED** |
| **All Backend Tests (`mvn test`)** | **243** | **243** | **0** | **PASSED** |
| **Frontend Tests (`npm test`)** | **29** | **29** | **0** | **PASSED** |

---

## 6. Live Gemini API Verification Guide

While local and CI environments operate securely with offline mocks and verified rule fallback, live Gemini execution can be enabled at any time:

1. Obtain a Google Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. In your deployment environment or `.env` file, set:
   ```env
   AI_PROVIDER=gemini
   GEMINI_API_KEY=AIzaSyYourVerifiedKeyHere
   GEMINI_MODEL=gemini-1.5-flash
   AI_FALLBACK_ENABLED=true
   ```
3. Restart the backend application.
4. Run the diagnostic probe:
   ```bash
   curl -X GET "http://localhost:8080/api/chat/status?probe=true" \
        -H "Authorization: Bearer <jwt_token>"
   ```
5. Confirm the response:
   ```json
   {
     "provider": "gemini",
     "configured": true,
     "hasApiKey": true,
     "status": "PROVIDER_AVAILABLE",
     "connectivityVerified": true,
     "activeMode": "GEN_AI_PROVIDER"
   }
   ```
