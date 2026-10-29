# PRAHAR AI — Chatbot Comprehensive Verification & Test Report

**System**: Prahar Real-Time AI/ML-Based Phishing Detection & Prevention System  
**Test Suite**: Spring Boot Surefire Test Runner & Node Test Runner  
**Date**: October 2026  
**Status**: 100% Tests Passing (261 Backend, 29 Frontend)  

---

## 1. Test Execution Summary

| Test Domain | Executed Command | Tests Run | Passed | Failed | Errors | Skipped | Status |
|---|---|---|---|---|---|---|---|
| **Backend Core & Chatbot** | `mvn test` | 261 | 261 | 0 | 0 | 0 | **PASSED** |
| **Frontend UI & Components** | `npm test -- --watchAll=false` | 29 | 29 | 0 | 0 | 0 | **PASSED** |
| **Dedicated Chatbot Scenarios** | `mvn test -Dtest=PraharChatbotScenariosTest` | 26 | 26 | 0 | 0 | 0 | **PASSED** |

---

## 2. Section 10 Natural-Language Test Matrix

All 12 natural-language questions and multi-turn workflows specified in Section 10 of the specification were executed and verified:

| # | User Query | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|---|
| 1 | *"What are online scams?"* | Direct definition, examples (phishing, fake lottery, tech support), practical avoidance; no generic triage. | Returns structured definition with 4 scam categories and Prahar safety tips. | **PASS** |
| 2 | *"What are suspicious links?"* | Explains suspicious links, warning signs, and safe scanning with Prahar; no incident checklist. | Returns detailed warning signs, HTTPS caveats, and Scanner reference. | **PASS** |
| 3 | *"How do I recognize a replica website?"* | Explains imitation signs: misleading domains, copied branding, SSL caveats, payment irregularities. | Returns 5-point verification framework including typosquatting and SSL certificate facts. | **PASS** |
| 4 | *"What is OTP?"* | Defines OTP, explains its purpose, and delivers clear warning never to share it. | Returns direct explanation of One-Time Password and explicit sharing prohibition. | **PASS** |
| 5 | *"What is the difference between phishing and online scams?"* | Explains difference (online scams is the broad umbrella; phishing is the social-engineering delivery vector). | Returns clear distinction distinguishing broad cyber fraud from targeted impersonation. | **PASS** |
| 6 | *"How can I stay safe online?"* | Provides concise practical safety recommendations without overwhelming checklist. | Returns focused practical cyber hygiene rules (2FA, link verification, unique passwords). | **PASS** |
| 7 | *"What is the 1930 helpline?"* | Explains Citizen Financial Cyber Fraud Reporting System, Golden Hour, and reporting process. | Returns 1930 operation details, Golden Hour timeframe, and cybercrime.gov.in links. | **PASS** |
| 8 | Turn 1: *"What is phishing?"*<br>Turn 2: *"How can I identify it?"* | Resolves pronoun `"it"` back to phishing, explains identification cues without resetting context. | Identifies pronoun reference, maintains phishing context, and details identification tactics. | **PASS** |
| 9 | Turn 1: *"I entered my details on a fake website."* | Initial protective steps, checks exposed credentials, preserves active incident status. | Classifies as `ACCOUNT_COMPROMISE`, advises immediate password change and 2FA reset. | **PASS** |
| 10 | Turn 2: *"I paid using UPI."* | Updates incident with payment method, provides UPI-specific dispute guidance and 12-digit UTR instructions. | Seamlessly links payment method into incident context, explains UTR retrieval and app dispute. | **PASS** |
| 11 | Turn 3: *"What should I do now?"* | Uses aggregated conversation context to recommend immediate containment without repeating previous questions. | Delivers prioritized next steps: 1930 freeze, bank dispute, and portal filing. | **PASS** |
| 12 | *"What is the difference between an OTP and a UPI PIN?"* | Highlights single-use dynamic auth for receiving/verifying vs. secret static PIN for debiting money. | Clear technical differentiation: OTP authorizes logins/charges; UPI PIN debits your account. | **PASS** |

---

## 3. Predefined Questions & Quick-Reply Regression Matrix

Verification that existing suggested questions and quick replies remain fully functional:

| Predefined Question | Classification Target | Verification Test | Result |
|---|---|---|---|
| `₹5,000 was debited from my account.` | `NlpIntent.UNKNOWN_DEBIT` | `testPredefined_UnauthorizedDebit` | **PASS** |
| `What should I do if my phone is stolen?` | `NlpIntent.LOST_PHONE` | `testPredefined_StolenPhone` | **PASS** |
| `What should I do if I shared my OTP?` | `NlpIntent.OTP_DISCLOSURE` | `testPredefined_SharedOtp` | **PASS** |
| `How do I dispute a UPI payment?` | `NlpIntent.UPI_FRAUD` | `testPredefined_DisputeUpiPayment` | **PASS** |
| `How do I block my debit or credit card?` | `NlpIntent.ATM_CARD_FRAUD` | `testPredefined_BlockDebitOrCreditCard` | **PASS** |
| `What is the 1930 helpline and Golden Hour?` | `NlpIntent.FRAUD_REPORTING` | `testNaturalLanguage7_WhatIs1930Helpline` | **PASS** |
| `How do I recognize a fake shopping site?` | `NlpIntent.GENERAL_CYBER_SAFETY` | `testQualityB3_FiveWaysToIdentifyFakeWebsite` | **PASS** |

---

## 4. Provider Edge-Case Tests

| Test Case | Scenario Description | Tested Condition | Result |
|---|---|---|---|
| `testProvider_MissingApiKey` | Gemini API key is missing or blank | System marks health as `MISSING_CREDENTIALS` and gracefully falls back to deterministic engine. | **PASS** |
| `testProvider_RateLimitExceeded` | Gemini returns HTTP 429 Too Many Requests | System marks health as `RATE_LIMITED` and transparently services request via local safety rules. | **PASS** |
| `testProvider_SuccessfulLiveResponse` | Live Gemini returns valid payload | Response marked as `isLiveAi: true` with provider set to `gemini`. | **PASS** |

---

## 5. Summary Conclusion

All regression and natural-language test scenarios execute with 0 failures, 0 errors, and 0 flaky runs. The chatbot reliably serves both fixed predefined suggestions and arbitrary natural language user inputs.
