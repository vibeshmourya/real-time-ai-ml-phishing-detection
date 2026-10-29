# PRAHAR AI — Chatbot Root Cause & Audit Diagnostic Report

**System**: Prahar Real-Time AI/ML-Based Phishing Detection & Prevention System  
**Component**: Conversational Cybersecurity Pipeline (`IntentClassifier.java`, `ChatService.java`, `EntityExtractor.java`, `GeminiService.java`, React UI)  
**Date of Audit**: October 2026  
**Status**: Root Cause Isolated, Addressed, and Fully Verified across 261 Backend & 29 Frontend Tests  

---

## 1. Executive Summary

A comprehensive architectural and behavioral audit was conducted on the Prahar Chatbot system to resolve why typed questions received inappropriate responses (e.g. generic triage checklists, repetitive answers, misunderstandings of user intent, or unsolicited clarification queries) while strictly preserving all predefined suggestions, quick-reply buttons, and incident-response workflows.

The audit revealed that the root cause was not a single bug, but a collection of structural keyword collisions, grammar prefix gaps, educational vs. incident confusion, shadowed comparative queries, non-deterministic database ordering, and state loss in multi-turn follow-ups.

---

## 2. Root Cause Breakdown

### A. Educational Keyword Traps & False Incident Classification
- **Mechanism**: The keyword matcher evaluated incident patterns before educational patterns. For example, queries containing `"fake website"` (e.g., *"How do I recognize a replica website?"* or *"How do I recognize a fake shopping site?"*) triggered `NlpIntent.FAKE_SHOPPING_SCAM`.
- **Impact**: Rather than explaining how to spot imitation websites (domain typosquatting, invalid SSL context, copied logos), the bot assumed an active financial fraud incident had already occurred and prompted the user for payment details and bank dispute steps.
- **Resolution**: Implemented `isFakeWebsiteEducation` in `IntentClassifier.java` before incident matching. It isolates educational intent when users inquire about recognizing, identifying, spotting, or checking replica or fake websites without stating that they entered credentials or suffered losses.

### B. Missing Natural Language Prefix Patterns ("What are...")
- **Mechanism**: The educational intent detector checked `lower.startsWith("what is ")` or `lower.startsWith("how do i ")`, but lacked `lower.startsWith("what are ")`.
- **Impact**: Inquiries such as *"What are online scams?"* and *"What are suspicious links?"* failed the educational matcher and cascaded to `NlpIntent.AMBIGUOUS_QUERY`, prompting the bot to respond with generic triage (*"Could you share a few more details about what you're dealing with?"*).
- **Resolution**: Expanded lexical triggers in `IntentClassifier.java` to support `"what are"`, plural forms, and explicit keywords for `"online scams"` and `"suspicious links"`.

### C. Shadowing of Comparative Queries
- **Mechanism**: The definition handler for `"phishing"` or `"otp"` evaluated individual token presence (`lower.contains("phishing")` or `lower.contains("otp")`) before evaluating comparative expressions.
- **Impact**: When a user asked *"What is the difference between phishing and online scams?"* or *"What is the difference between an OTP and a UPI PIN?"*, the classifier saw `"phishing"` or `"otp"` and returned the baseline standalone definition, completely ignoring the comparison.
- **Resolution**: Prioritized comparative evaluation (`lower.contains("difference between")`) at the top of the educational router in `IntentClassifier.java` and `ChatService.java`.

### D. Multi-Turn Incident Context Loss on Follow-Up Steps
- **Mechanism**:
  1. In Turn 1, the user reported *"I entered my details on a fake website."* (classified as `ACCOUNT_COMPROMISE` / `details_type`).
  2. In Turn 2, the user said *"I paid using UPI."* The classifier checked whether the slot was `"payment_method"` or `"payment_channel"`. Because Turn 1 set `awaitingSlot = "details_type"`, Turn 2 failed the transition and reverted back to `ACCOUNT_COMPROMISE`.
  3. In Turn 3, when the user asked *"What should I do now?"*, `buildFakeShoppingScamResponse()` lacked an `isFollowUpQuery()` branch, causing it to blindly repeat the initial question asking if the user paid online or via COD.
- **Impact**: The bot asked repetitive, frustrating clarification questions rather than tracking the accumulating incident facts.
- **Resolution**:
  - Expanded `IntentClassifier.java` to allow payment channel transitions across all active incident intents (`isIncidentIntent(context.getLastIntent())`) and handle the `details_type` slot.
  - Added `isFollowUpQuery(lower)` in `ChatService.java` to recognize `"what should i do now"`, `"what now"`, and `"next step"` and deliver an immediate prioritized incident containment plan (12-digit UTR dispute, 1930 Golden Hour freeze, bank block, credential reset).

### E. Card Blocking Routing Gap
- **Mechanism**: The predefined button *"How do I block my debit or credit card?"* was intended to route to `ATM_CARD_FRAUD`. However, `IntentClassifier.java` did not explicitly check `"block my debit"`, `"block my card"`, or `"freeze card"`.
- **Impact**: Without explicit keyword coverage, typed or clicked variants could fall through to ambiguous query handling.
- **Resolution**: Added explicit keyword rules for card blocking and locking under `NlpIntent.ATM_CARD_FRAUD`.

### F. Non-Deterministic History Pagination Ordering in Tests
- **Mechanism**: `ChatMessageRepository.findByConversationOrderByCreatedAtAsc` ordered messages solely by `createdAt ASC`. When automated tests inserted multiple dialogue turns in rapid succession, identical millisecond timestamps caused the database to return messages in non-deterministic row order.
- **Impact**: Conversational context tests periodically failed or dropped previous turns.
- **Resolution**: Updated `ChatMessageRepository.java` and `ChatHistoryService.java` to order by `createdAt ASC, id ASC`, ensuring strict deterministic chronological ordering.

---

## 3. Preserved Predefined Functionality

All existing predefined questions, suggested prompt chips, quick replies, and incident response flows remain untouched and fully operational:
1. `₹5,000 was debited from my account.`
2. `How do I identify a phishing message?`
3. `What should I do if my phone is stolen?`
4. `What is the 1930 helpline and Golden Hour?`
5. `Summarise my recent transactions.`
6. `What should I do if I shared my OTP?`
7. `How do I recognize a fake shopping site?`
8. `How do I dispute a UPI payment?`
9. `How do I block my debit or credit card?`
10. `What if I shared my OTP or UPI PIN?`
11. `How do I report a scammer website?`

No predefined items were removed or replaced. Predefined quick-replies route directly to their deterministic, certified safety workflows.

---

## 4. Verification & Testing

- **Backend Test Suite**: 261/261 tests passed (`mvn test`), including 26 dedicated scenarios in `PraharChatbotScenariosTest`.
- **Frontend Test Suite**: 29/29 tests passed (`npm test`).
- **Regression Safety**: All predefined question flows, mock provider edge-cases (missing API key, rate limits, timeouts), and multi-turn scenarios verified green.
