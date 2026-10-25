# PRAHAR Chatbot Diagnostic Report

## 1. Executive Summary & Objective

This diagnostic report analyzes the root causes of the seven (7) observed conversational and architectural failures in the **Prahar AI Safety Assistant**. It maps each failure to specific lines of code across the React frontend, Spring Boot backend, NLP engine, and Gemini API integration layers.

---

## 2. End-to-End Message Pipeline

The verified message-processing pipeline executes as follows:

```
[React/Vite Frontend]
  │  (ChatAssistantPage.jsx / PraharChatbot.jsx)
  │  Payload: { message, conversationHistory: [{role, content}] }
  ▼
[ChatController.java: POST /api/chat]
  │
  ▼
[ChatService.java: processChat()]
  │
  ├── 1. Sanitize & Normalize:
  │      sanitizeInput() -> NlpNormalizer.normalize() -> EntityExtractor.extract()
  │
  ├── 2. History & Context Reconstitution:
  │      buildDialogueContext(conversationHistory)
  │
  ├── 3. Intent & Topic Change Classification:
  │      IntentClassifier.classify(normalized, context, entities)
  │
  ├── 4. Primary Response Generator (Gemini Integration):
  │      callLiveAiProvider() [when AI_PROVIDER != 'none' and AI_API_KEY configured]
  │      - Builds system prompt with directives & verified scan findings
  │      - Supplies conversation history
  │      - Executes OpenAI-compatible chat completion or tool execution
  │
  ├── 5. Deterministic Safety Engine (Fallback / Rule Mode):
  │      generateNlpResponse() [when provider is 'none' or live provider fails]
  │      - Domain-specific responses (phishing, fake sites, fraud, URL scans)
  │
  ├── 6. Response Assembly:
  │      ChatResponse { reply, isLiveAi, provider, followUpSuggestions, actionChecklist }
  │
  ▼
[Frontend Message Rendering]
  - Renders Markdown content
  - Displays Action Checklist (if present)
  - Displays metadata pill & suggestions
```

---

## 3. Root Cause Analysis of Observed Failures

### Failure 1: "Tell me about this app" produces a generic incident question
- **Observed Behavior:** Asking *"Tell me about this app"* resulted in the chatbot asking: *"Could you share a few more details about what you're dealing with? For example, is it a suspicious link, an order from an unfamiliar website, an unrecognized transaction, or an account security concern?"*
- **Root Cause:**
  1. In `IntentClassifier.java` (`ASSISTANT_IDENTITY`), the pattern matching only checked phrases like `"tell me about you"`, `"who are you"`, `"what is prahar"`. It did NOT check `"tell me about this app"`, `"about this app"`, `"what does this app do"`, or `"about this application"`.
  2. Because it failed to match `ASSISTANT_IDENTITY`, the message fell through to `AMBIGUOUS_QUERY`.
  3. In `ChatService.java`, `buildGeneralResponse()` (the handler for `AMBIGUOUS_QUERY`) returned the generic incident intake prompt intended for vague security complaints.
- **Component Responsible:** Backend (`IntentClassifier.java`, `ChatService.java`).
- **Status:** `VERIFIED`.

---

### Failure 2: "What is OTP?" produces an unrelated fraud-prevention checklist
- **Observed Behavior:** Asking *"What is OTP?"* returned a generic 7-step checklist on passwords, 2FA, UPI PIN rules, COD shopping, and OS updates, rather than explaining what a One-Time Password is.
- **Root Cause:**
  1. In `IntentClassifier.java`, informational questions starting with `"what is "` containing `"otp"` were routed to `GENERAL_CYBER_SAFETY`.
  2. In `ChatService.java`, `buildGeneralSafetyChecklistResponse()` had specialized blocks for session hijacking, fake sites, HTTPS, ML, and phishing, but lacked a dedicated block for `otp` definitions.
  3. As a result, it fell through to the default general online fraud prevention checklist.
  4. In addition, `generateActionChecklist(GENERAL_CYBER_SAFETY)` attached 5 generic security bullet points as an `actionChecklist`, compounding the irrelevant content.
- **Component Responsible:** Backend (`ChatService.java`).
- **Status:** `VERIFIED`.

---

### Failure 3: "What is phishing?" produces a useful answer but also includes unrelated repeated content
- **Observed Behavior:** The answer accurately defined phishing and gave an example, but was immediately followed by a repeated generic security checklist.
- **Root Cause:**
  1. `buildGeneralSafetyChecklistResponse()` generated a thorough 4-part explanation of phishing.
  2. However, at the end of `generateNlpResponse()`, `generateActionChecklist(GENERAL_CYBER_SAFETY, context)` was unconditionally called and set on `response.setActionChecklist(actions)`.
  3. The frontend then rendered an `Immediate Action Checklist` component below the complete response, repeating generic advice on passwords and UPI PINs that duplicated or cluttered the definition.
- **Component Responsible:** Multi-layer (Backend `ChatService.java` attaching checklists to educational queries; Frontend rendering it unconditionally).
- **Status:** `VERIFIED`.

---

### Failure 4: General cybersecurity questions sometimes receive the same response
- **Observed Behavior:** Distinct educational questions (e.g., about firewalls, malware, or generic cyber hygiene) received the identical 7-point checklist.
- **Root Cause:**
  1. In `ChatService.java`, `buildGeneralSafetyChecklistResponse()` had a catch-all fallback that returned the identical static list whenever an educational question did not match an exact keyword `if` statement.
  2. When live Gemini was unconfigured or failed, the fallback was not dynamically adapted to the specific question topic.
- **Component Responsible:** Backend (`ChatService.java`).
- **Status:** `VERIFIED`.

---

### Failure 5: A new question may be interpreted as a continuation of the previous incident
- **Observed Behavior:** If a user reported a shopping scam and subsequently asked an educational question like *"What is OTP?"* or *"What is machine learning?"*, the assistant sometimes treated it as a payment follow-up.
- **Root Cause:**
  1. In `buildDialogueContext()`, previous turns set `awaitingSlot` (e.g. `shopping_payment_details`).
  2. When the user sent a new question, slot evaluation in `IntentClassifier.java` took precedence over topic change detection, unless the exact question prefix was explicitly caught by `isTopicChangeOrNewQuestion()`.
- **Component Responsible:** Backend (`IntentClassifier.java`, `ChatService.java`).
- **Status:** `VERIFIED`.

---

### Failure 6: The interface displays a second "Immediate Action Checklist" after an otherwise complete answer
- **Observed Behavior:** Both `ChatAssistantPage.jsx` and `PraharChatbot.jsx` render a separate, boxed `Immediate Action Checklist` below the main answer.
- **Root Cause:**
  1. `ChatService.java` lines 499 & 684 unconditionally invoked `generateActionChecklist()` for every intent (even when Gemini generated a complete, personalized answer, or when the query was purely educational).
  2. The frontend templates (`ChatAssistantPage.jsx` line 267 and `PraharChatbot.jsx` line 390) unconditionally rendered an `<Immediate Action Checklist>` card whenever `m.actionChecklist && m.actionChecklist.length > 0`.
- **Component Responsible:** Backend (`ChatService.java`) + Frontend (`ChatAssistantPage.jsx`, `PraharChatbot.jsx`).
- **Status:** `VERIFIED`.

---

### Failure 7: Internal labels such as "Prahar Safety Engine" appear in user-facing messages
- **Observed Behavior:** User messages display internal implementation tags such as `Prahar Safety Engine`, `Safety Engine`, and raw enum strings like `GENERAL CYBER SAFETY`.
- **Root Cause:**
  1. In `PraharChatbot.jsx` line 416: `msg.isLiveAi ? 'Live AI' : 'Prahar Safety Engine'` was rendered inside the message card.
  2. In `ChatAssistantPage.jsx` lines 294-295: `' • Safety Engine'` and `m.intent.replace(/_/g, ' ')` were printed in the message footer.
- **Component Responsible:** Frontend (`PraharChatbot.jsx`, `ChatAssistantPage.jsx`).
- **Status:** `VERIFIED`.

---

## 4. Remediation Architecture Plan

1. **Backend Intent & Context Layer (`IntentClassifier.java`):**
   - Expand `ASSISTANT_IDENTITY` to capture *"tell me about this app"*, *"about this application"*, *"what does this app do"*.
   - Robustly classify educational concepts (`what is otp`, `what is 2fa`, `firewall`, `malware`) into distinct educational intents without slot collision.
   - Suppress `actionChecklist` generation for live AI responses (since Gemini already provides full context-aware actions) and for educational queries. Only attach structured checklists for active security emergencies.

2. **Backend AI Integration Layer (`ChatService.java`):**
   - Integrate controlled Gemini Function Calling / Tool routing for:
     - `analyze_url(url)`: Invoking SSRF protection and `RuleEngineService`.
     - `explain_scan_result(scanId)`: Explaining existing scan records.
   - Ground Gemini system prompt with exact Prahar capabilities.
   - Provide clean, dedicated educational fallbacks for OTP, 2FA, firewalls, and general concepts when running offline.

3. **Frontend Layer (`ChatAssistantPage.jsx` & `PraharChatbot.jsx`):**
   - Clean up user-facing footers: Remove internal implementation labels (`Prahar Safety Engine`, `Safety Engine`, raw intent enums).
   - Only render `actionChecklist` when it contains urgent incident items and does not duplicate content.
