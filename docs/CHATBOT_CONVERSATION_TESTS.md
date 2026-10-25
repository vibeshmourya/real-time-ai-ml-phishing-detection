# PRAHAR — CHATBOT MULTI-TURN CONVERSATION TEST LOGS

**Date**: October 2026  
**Test Suite**: Prahar Conversational Intelligence, Context Awareness & Response Generation  
**Status**: ALL TESTS & SCENARIOS VERIFIED  

---

## 1. Overview

This document records the exact conversation traces, input messages, classified intents, assistant responses, and verification outcomes for the 10 automated regression tests and conversational scenarios (Phases 5 & 8).

---

## 2. Phase 8 Automated Regression Tests (Tests 1–10)

### TEST 1: Assistant Identity & Introduction
- **User Message**: `"Tell me about yourself."`
- **Intent**: `ASSISTANT_IDENTITY`
- **Response**:
  > "I'm Prahar AI Assistant, your dedicated cybersecurity and digital fraud response advisor.
  > 
  > Here is how I can assist you:
  > - **Suspicious Links & QR Codes**: Paste any link, email, SMS, or QR code into Prahar's **Scan** tab to check for malware, phishing, and domain spoofing.
  > - **Scam Containment**: Provide step-by-step emergency actions for fake shopping sites, unexpected debits, UPI scams, and credential disclosures.
  > - **Account Security**: Guide you on setting up two-factor authentication (2FA), securing leaked credentials, and managing lost devices.
  > - **Incident Reporting**: Prepare formal dispute drafts and guide you through reporting on India's National Cyber Crime Helpline (**1930**) and [cybercrime.gov.in](https://cybercrime.gov.in/).
  > 
  > What issue or question can I help you with right now?"
- **Outcome**: **VERIFIED** — Direct introduction, no generic loops, no raw enum exposed in UI.

---

### TEST 2: Fake Shopping Website with Completed Payment
- **Turn 1 (User)**: `"I ordered something from a fake shopping website."`
- **Assistant**:
  > "Don't panic - you're taking the right step by addressing this immediately. Do not make any further payments or submit additional personal details on that webpage.
  > 
  > To help me give you the exact protective steps:
  > Did you pay online (via UPI, debit/credit card, NetBanking), or is it Cash on Delivery (COD)? Also, did you enter any passwords, card details, or OTPs on that site?"
- **Turn 2 (User)**: `"But I have already paid to the seller online."`
- **Intent**: `FAKE_SHOPPING_SCAM` (Follow-up Turn: True)
- **Assistant**:
  > "Since you have already made the payment online, act immediately to dispute the transaction and limit further damage:
  > 
  > 1. **Contact Your Bank or Payment App Immediately**: Open your payment app or call your bank's 24x7 helpline right now to dispute the fraudulent charge and request a chargeback or payment recall.
  > 2. **Report on 1930 & Cyber Crime Portal**: In India, call the National Cyber Crime Helpline (**1930**) immediately within the Golden Hour (first 2–3 hours) and file a complaint at [cybercrime.gov.in](https://cybercrime.gov.in/). Timely reporting enables authorities to freeze funds before scammers cash out.
  > 3. **Preserve All Evidence**: Save the transaction ID / UTR, payment receipt, website URL, order confirmation emails, WhatsApp or SMS chats, and screenshots.
  > 4. **Beware of Recovery Scams**: Never pay any 'processing fee', 'refund clearance charge', or disclose OTPs or UPI PINs to anyone claiming they can recover your money.
  > 
  > To help me guide your next steps:
  > - Which payment method did you use: **UPI, Debit/Credit Card, or Net Banking**?
  > - Did you enter any account passwords, PINs, or card details on that fake site?"
- **Outcome**: **VERIFIED** — Acknowledges that payment has been made; does not repeat the initial online vs COD question; prioritizes bank dispute, 1930, evidence preservation; asks for payment method without requesting secrets.

---

### TEST 3: UPI in Context
- **Context**: User was discussing fake shopping scam in previous turn.
- **User Message**: `"I paid through UPI."`
- **Intent**: `FAKE_SHOPPING_SCAM` (Follow-up Turn: True)
- **Assistant**:
  > "Since you paid via UPI, act quickly to contain the fraud:
  > 
  > 1. **Dispute in UPI App**: Open Google Pay, PhonePe, Paytm, or BHIM, select the payment transaction, copy down the **12-digit UTR reference number**, and tap 'Report Fraud' or 'Dispute Transaction'.
  > 2. **Call 1930 Helpline**: Report immediately to India's National Cyber Crime Helpline (**1930**) or register a complaint at [cybercrime.gov.in](https://cybercrime.gov.in/). Reporting within the **Golden Hour** (2–3 hours) enables the authorities to alert the recipient bank and freeze the funds.
  > 3. **Notify Your Bank**: Call your bank's 24x7 customer care to report the unauthorized or fraudulent merchant transaction.
  > 
  > Did you enter your UPI PIN on that website, or was it entered only inside your legitimate UPI app?"
- **Outcome**: **VERIFIED** — Retains incident context; delivers UPI-specific instructions (12-digit UTR, in-app dispute, 1930 Golden Hour).

---

### TEST 4: Entered OTP / Credential Disclosure
- **User Message**: `"I entered my OTP."`
- **Intent**: `OTP_DISCLOSURE`
- **Assistant**:
  > "Treat this as urgent: Stop sharing any more codes with that person right now.
  > 
  > Take these immediate actions to secure your assets:
  > 1. **Freeze or Lock Affected Service**: If this was for your bank, UPI, or card, contact your bank's customer care immediately or lock your accounts through your official banking app.
  > 2. **Change Credentials**: Update the password or PIN for the affected service from a secure device, and choose *'Log out of all existing sessions'*.
  > 3. **Call 1930 Helpline**: Report immediately to India's National Cyber Crime Helpline (**1930**) so any fraudulent transfer can be frozen within the Golden Hour.
  > 
  > Which type of account was the OTP for? (Bank account, UPI app, WhatsApp, email, or a shopping service?)"
- **Outcome**: **VERIFIED** — Urgent safety advice; never requests the OTP itself; asks which service was affected.

---

### TEST 5: General Online Fraud Prevention
- **User Message**: `"How do I prevent online fraud?"`
- **Intent**: `GENERAL_CYBER_SAFETY`
- **Assistant**:
  > "Here is an essential checklist to protect yourself from online frauds, cyber attacks, and scams:
  > 
  > 1. **Strong Passwords & 2FA**: Use unique passwords for your banking and email accounts. Enable two-factor authentication (prefer Authenticator apps like Google or Microsoft Authenticator over SMS).
  > 2. **Never Share Secrets**: Legitimate banks, police, or government officials will **never** ask for your OTP, UPI PIN, ATM PIN, CVV, or passwords over call, SMS, or WhatsApp.
  > 3. **Inspect Links & Messages**: Do not click links claiming urgent electricity disconnection, bank account suspension, or lottery wins. Verify links in Prahar's **Scan** tab.
  > 4. **Safe Online Shopping**: Only shop from verified e-commerce sites. Check for authentic domain spellings and realistic pricing. For unfamiliar sellers, prefer Cash on Delivery (COD).
  > 5. **UPI Golden Rule**: Entering your UPI PIN is **only for sending money**, never for receiving payments, refunds, or lottery prizes.
  > 6. **Keep Software Updated**: Regularly install official OS, browser, and antivirus updates on your smartphone and computer to patch known vulnerabilities.
  > 7. **Emergency Action (1930)**: If unauthorized money transfer occurs, call India's National Cyber Crime Helpline (**1930**) immediately within the Golden Hour (2–3 hours) or report at [cybercrime.gov.in](https://cybercrime.gov.in/).
  > 
  > Is there a specific situation you would like advice on, such as securing an account or verifying a suspicious link?"
- **Outcome**: **VERIFIED** — Structured, actionable 7-point preventive checklist without forcing category selection.

---

### TEST 6: Unauthorized Debit
- **User Message**: `"My bank account has an unauthorized debit."`
- **Intent**: `UNKNOWN_DEBIT`
- **Assistant**:
  > "### Immediate Steps for Unrecognized Debit
  > 1. **Lock/Freeze Your Card or Account**: Open your banking app and immediately freeze the affected card, disable international/online transactions, or block the account.
  > 2. **Contact Bank Fraud Cell**: Call your bank's 24x7 fraud reporting helpline immediately to report an unauthorized debit and request a chargeback. Note your incident reference number.
  > 3. **National Cyber Crime Helpline (1930)**: In India, call **1930** immediately or report at [cybercrime.gov.in](https://cybercrime.gov.in/) within the **Golden Hour** (2–3 hours) to freeze transited funds.
  > 4. **Check for Compromised Channels**: Did you share an OTP, click a link, or install any remote desktop app (e.g. AnyDesk, TeamViewer) recently?"
- **Outcome**: **VERIFIED** — Immediate financial fraud response; not a generic phishing checklist.

---

### TEST 7: Explanation of Phishing
- **User Message**: `"What is phishing?"`
- **Intent**: `GENERAL_CYBER_SAFETY`
- **Assistant**:
  > "### What is Phishing?
  > **Phishing** is a form of social engineering where attackers impersonate trustworthy entities (such as banks, government departments, delivery services, or trusted brands) to deceive you into disclosing confidential information.
  > 
  > **Common Vectors:**
  > - **Email Phishing**: Fake invoices, account suspension alerts, or tax refund lures.
  > - **Smishing (SMS)**: Electricity bill cut-off notices, e-challan warnings, or lottery messages with shortened links.
  > - **Vishing (Voice Call)**: Impersonation of bank managers or law enforcement threatening arrest or account block.
  > - **Quishing (QR Code)**: Fraudulent QR codes claiming to send money to your account (remember: scanning QR codes only sends money).
  > 
  > **How to Stay Safe:**
  > - Always inspect sender email addresses and website URLs carefully.
  > - Never enter credentials on links received via SMS, WhatsApp, or unsolicited emails.
  > - You can check any suspicious link using Prahar's **Scan** tab.
  > - Report suspicious phishing SMS to **1909** (Telecom Regulator) and cybercrime portals."
- **Outcome**: **VERIFIED** — Clear educational explanation of phishing vectors and defense.

---

### TEST 8: Session Hijacking & Cookie Theft Defense
- **User Message**: `"Explain session hijacking and how to defend against it."`
- **Intent**: `GENERAL_CYBER_SAFETY`
- **Assistant**:
  > "### Session Hijacking & Cookie Theft: What It Is and How to Defend
  > 
  > **Session Hijacking** occurs when an attacker obtains your active session token or authentication cookie to impersonate you on a web service without needing your username or password.
  > 
  > **How Attackers Steal Sessions:**
  > - **Cross-Site Scripting (XSS)**: Injecting malicious scripts into web pages that read cookies from `document.cookie`.
  > - **Man-in-the-Middle (MitM) Attacks**: Intercepting unencrypted web traffic on open, public Wi-Fi networks.
  > - **Session Side-jacking & Sniffing**: Capturing session tokens transmitted over insecure HTTP.
  > - **Malware & Infostealers**: Malicious browser extensions or infostealer trojans that extract saved session cookies from browser profiles.
  > 
  > **Key Defenses (For Users & Developers):**
  > 1. **HttpOnly & Secure Cookie Flags**: Web developers must set `HttpOnly` (prevents JavaScript access via XSS) and `Secure` (ensures cookies are only sent over HTTPS) with `SameSite=Lax` or `Strict`.
  > 2. **Enforce HTTPS (HSTS)**: Protects transmission from eavesdropping on public Wi-Fi.
  > 3. **Log Out When Done**: Explicitly logging out destroys the session token on the server.
  > 4. **Avoid Untrusted Wi-Fi**: Use a secure VPN or mobile data when accessing sensitive accounts in public.
  > 5. **Enable Multi-Factor Authentication (MFA)**: Limits the persistence of stolen sessions."
- **Outcome**: **VERIFIED** — Defensive educational explanation explaining risks and technical/user countermeasures.

---

### TEST 9: Two Unrelated Questions in Succession
- **Turn 1 (User)**: `"What is a DDoS attack?"`
- **Assistant**: Explains Distributed Denial of Service attacks and prevention.
- **Turn 2 (User)**: `"How do I report a lost phone in India?"`
- **Assistant**: Explains CEIR (Central Equipment Identity Register) portal (`ceir.gov.in`) and blocking IMEI numbers.
- **Outcome**: **VERIFIED** — Assistant cleanly addresses each question independently without mixing contexts.

---

### TEST 10: Provider Failure Simulation
- **Simulation**: Missing API key, 401 Unauthorized, 429 Rate Limit/Quota, 504 Timeout, Invalid JSON.
- **Outcome**:
  - Catches provider error without crashing.
  - Server logs sanitized diagnostic warning without revealing credentials.
  - Returns `ChatResponse.status = "FALLBACK"` (or `"FALLBACK_401"`, `"FALLBACK_429"`).
  - Supplies verified domain guidance (e.g. 1930 / bank freeze) without pretending an AI model responded.
- **Outcome**: **VERIFIED** — Transparent, resilient failure handling.

---

## 3. Summary of Test Results

| Test Category | Total Tests | Passed | Failures | Status |
| :--- | :--- | :--- | :--- | :--- |
| Phase 8 Tests (1 to 10) | 10 | 10 | 0 | **VERIFIED** |
| Full JUnit 5 Backend Suite | 42 | 42 | 0 | **VERIFIED** |
| Frontend Suite (`npm test`) | 20 | 20 | 0 | **VERIFIED** |
| Production Build (`npm run build`) | 1 | 1 | 0 | **VERIFIED** |
