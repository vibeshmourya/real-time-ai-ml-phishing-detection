# PRAHAR CHATBOT QUALITY AUDIT & ROOT-CAUSE RESOLUTION

**Document Version:** 1.0  
**Status:** `VERIFIED`  
**System:** Prahar — Real-Time AI/ML-Based Phishing Detection and Prevention System  
**Test Suite Status:** 92 Backend Tests Passed (0 Failures, 0 Errors) | 25 Frontend Tests Passed | Production Build Clean

---

## 1. Executive Summary

A comprehensive engineering audit and root-cause repair was conducted on the Prahar Chatbot subsystem to transform it into a concise, context-aware cybersecurity assistant. Prior to this intervention:
1. **Verbosity & Bloat:** The offline fallback engine generated overly lengthy responses (>250 words) with multi-level headings and redundant action checklists even for simple definitional questions (e.g., *"What is phishing?"*, *"What is OTP?"*).
2. **Intent Classification Misroutes:** Inquiries such as *"Give me five ways to identify a fake website."* bypassed educational checks because the phrase `identify a fake website` (with the article 'a') did not match `identify fake website`, causing it to drop into the `FAKE_SHOPPING_SCAM` incident questionnaire asking for payment details.
3. **Missing Definitional Handling:** Common technical cybersecurity terms like *"What is UPI?"* and *"What is a firewall?"* lacked dedicated educational handlers and fell back to ambiguous general triage.
4. **Scammer Transfer Incident Guidance:** Phrases like *"I already transferred money to a scammer."* were not prioritizing urgent emergency containment steps (calling 1930 / bank freeze) ahead of generic incident triage.
5. **Redundant UI Cards:** Educational queries appended duplicate static action checklists beneath already comprehensive textual answers.

All root causes were diagnosed, repaired in source code, verified with 92 automated backend tests and 25 frontend tests, and validated against the strict Adaptive Response Length policy.

---

## 2. Request Flow & Execution Path Inspection

The complete end-to-end request flow was audited:

```
[Frontend User Input: PraharChatbot.jsx / ChatAssistantPage.jsx]
       │
       ▼ (HTTP POST /api/chat)
[Spring Boot Controller: ChatController.java]
       │
       ▼ (ChatRequest DTO)
[ChatService.java: processChat()]
       │
       ├─► [DialogueContextManager.java]: Retrieves/creates conversation session state & pending slot
       ├─► [IntentClassifier.java]: Multi-signal intent classification & slot resolution
       │      ├─► Topic change check (isTopicChangeOrNewQuestion)
       │      ├─► Educational intent checks (fake website identification, definitions)
       │      └─► Incident routing (UNKNOWN_DEBIT, UPI_FRAUD, OTP_DISCLOSURE)
       │
       ├─► [AI Provider Evaluation]
       │      ├─ If AI_PROVIDER == "none" or API key empty:
       │      │      └─► Executes generateNlpResponse() (Prahar Rule & Safety Engine)
       │      ├─ If AI_PROVIDER == "gemini" / "openai" / "anthropic" and configured:
       │      │      ├─► Evaluates determineDetailLevel(userMessage) -> adjusts max_tokens dynamically
       │      │      ├─► Builds prompt via buildSystemPrompt() with Adaptive Length directives
       │      │      ├─► RestClient call with server-side tool execution support
       │      │      └─► On provider failure/timeout -> graceful fallback to generateNlpResponse()
       │      └─ Returns ChatResponse DTO with single assistant reply & context-sensitive suggestions
       ▼
[Frontend Render: Message bubble with single assistant answer & follow-up suggestions]
```

### Active Provider Verification
- In test and offline environments, `AI_PROVIDER=none` or empty `AI_API_KEY` activates the Prahar Rule & Safety Engine.
- The system returns `"prahar_nlp"` or `"<provider>_fallback"` transparently, never claiming an answer originated from Gemini when generated locally.

---

## 3. Root Cause Analysis & Code Repairs

### Defect 1: Trapped in Incident Questionnaire on Educational Questions
- **Observed Behavior:** User asked *"Give me five ways to identify a fake website."* and received *"Did you pay online (via UPI, debit/credit card, NetBanking), or is it Cash on Delivery (COD)?"*
- **Root Cause:** In [`IntentClassifier.java`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/nlp/IntentClassifier.java), line 165 only inspected `lower.contains("identify fake website")`. The presence of the word "a" caused a mismatch, dropping down to line 171 `lower.contains("fake website")` which mapped to `FAKE_SHOPPING_SCAM`.
- **Repair:** Refactored line 163–167 to intercept `identify fake`, `identify a fake`, `spot a fake`, `ways to identify`, and `how to identify`, properly mapping to `NlpIntent.GENERAL_CYBER_SAFETY`.

### Defect 2: Unnecessarily Long Responses on Simple Definitions
- **Observed Behavior:** Asking *"What is phishing?"* produced a 250+ word mini-essay with 4 markdown headings (`### Understanding Phishing`, `**Practical Example**`, `**Common Delivery Methods**`, `**Essential Phishing Defenses**`) even when a simple definition was requested.
- **Root Cause:** [`ChatService.java`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/ChatService.java) had only a static detailed template for phishing, OTP, and Prahar identity.
- **Repair:** 
  1. Created `ResponseDetailLevel` enum (`SHORT`, `DEFAULT`, `DETAILED`) and `determineDetailLevel(String userMessage)`.
  2. Implemented concise 1–3 sentence (~20–60 words) answers by default for:
     - *"What is phishing?"*: Concise 40-word definition explaining impersonation, fake messages/websites, and credential safety.
     - *"What is OTP?"*: Concise 35-word definition explaining One-Time Password, 2FA identity confirmation, and never-share rule.
     - *"What is UPI?"*: Concise 37-word definition explaining instant bank transfers and the UPI PIN deduction rule.
     - *"What is a firewall?"*: Concise 43-word definition explaining incoming/outgoing network filtering and barrier protection.
     - *"What is Prahar?"*: Concise 24-word definition detailing real-time phishing detection and incident guidance.
  3. Preserved deep, multi-section explanations when users explicitly request `"in detail"`, `"step by step"`, `"assignment"`, or `"comprehensive"`.

### Defect 3: Scammer Money Transfer Lacked Immediate Urgent Action
- **Observed Behavior:** When a user reported *"I already transferred money to a scammer."*, the system returned a multi-paragraph questionnaire about payment channels.
- **Root Cause:** [`IntentClassifier.java`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/nlp/IntentClassifier.java) lacked phrase matching for `transferred money to a scammer` in `UNKNOWN_DEBIT`, and [`ChatService.java`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/ChatService.java) lacked early containment routing.
- **Repair:** 
  1. Added `transferred money to a scammer`, `sent money to a scammer`, and `paid a scammer` to `UNKNOWN_DEBIT` in `IntentClassifier.java`.
  2. In `buildUnknownDebitResponse`, placed immediate containment first: contact bank immediately + call 1930 / cybercrime.gov.in + preserve transaction ID and messages + do not send more money/OTP, asking only which payment method was used.

### Defect 4: Redundant Duplicate Action Checklist Card on Educational Queries
- **Observed Behavior:** Educational and definitional responses rendered a secondary static bullet list of action items beneath the answer.
- **Root Cause:** `generateActionChecklist()` in [`ChatService.java`](file:///d:/MCA%20notes/MINI%20PRO%20SIH/backend/src/main/java/com/phishshield/service/ChatService.java) did not suppress checklists for all educational query variations.
- **Repair:** Broadened `isEducationalQuery` in `generateActionChecklist()` to return `Collections.emptyList()` for all educational definitions (phishing, OTP, UPI, firewall, fake website detection, 2FA, session hijacking).

---

## 4. Adaptive Response Length Policy Matrix

| Request Category | Query Example | Detail Level | Response Length Target | Structure |
| :--- | :--- | :--- | :--- | :--- |
| **Simple Definition** | *"What is phishing?"* | `DEFAULT` | 20–60 words | 1–3 sentences, direct answer, no headings, no checklists |
| **Short Request** | *"Explain phishing in one sentence."* | `SHORT` | 15–35 words | 1 concise sentence |
| **Moderate Detail** | *"Give me five ways to identify a fake website."* | `DEFAULT` | 60–140 words | Brief introduction + 5 bullet points |
| **Comprehensive Guide**| *"Explain phishing in detail."* | `DETAILED` | 150–300 words | Multi-section structured guide with practical examples |
| **Incident Emergency** | *"I already transferred money to a scammer."* | `INCIDENT` | 40–80 words | Urgent actions first (1930, bank freeze, dispute reference) |

---

## 5. Verification & Test Evidence

### Backend Test Suite
Command: `mvn test "-Dtest=ChatServiceTest,NlpEngineTest,GeminiIntegrationTest"`
- **Total Tests Run:** 92
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Execution Time:** ~32 seconds

Key Tests Verified:
- `testQualityA1_WhatIsPhishingConcise` (passed)
- `testQualityA2_WhatIsOtpConcise` (passed)
- `testQualityA3_WhatIsUpiConcise` (passed)
- `testQualityA4_WhatIsFirewallConcise` (passed)
- `testQualityA5_WhatIsPraharConcise` (passed)
- `testQualityB1_PhishingOneSentence` (passed)
- `testQualityB2_PhishingInDetail` (passed)
- `testQualityB3_FiveWaysToIdentifyFakeWebsite` (passed)
- `testQualityB4_OtpInSimpleWords` (passed)
- `testQualityC1_TopicChangePhishingToOtp` (passed)
- `testQualityC2_PronounResolution` (passed)
- `testQualityD1_TransferredMoneyToScammer` (passed)
- `testQualityE1_EducationalChecklistSuppression` (passed)

### Frontend Test Suite & Build
Command: `npm test -- --watchAll=false`
- **Total Tests Run:** 25
- **Pass:** 25
- **Fail:** 0

Command: `npm run build`
- **Modules Transformed:** 1604
- **Build Status:** Clean production bundle in `dist/` (29.71s)
