package com.phishshield.nlp;

import java.util.regex.Pattern;

public class IntentClassifier {

    public static class ClassificationResult {
        private final NlpIntent intent;
        private final double confidence;
        private final boolean isFollowUp;

        public ClassificationResult(NlpIntent intent, double confidence, boolean isFollowUp) {
            this.intent = intent;
            this.confidence = confidence;
            this.isFollowUp = isFollowUp;
        }

        public NlpIntent getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        public boolean isFollowUp() { return isFollowUp; }
    }

    private static final Pattern UNRELATED_PATTERNS = Pattern.compile(
            "(?i)\\b(capital of|weather in|how to bake|recipe for|tell me a joke|who is the prime minister|who won the match|write python code|play a song|movie recommendations|lyrics of)\\b"
    );

    public static ClassificationResult classify(String normalizedText, DialogueContext context, ExtractedEntities entities) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return new ClassificationResult(NlpIntent.AMBIGUOUS_QUERY, 0.30, false);
        }

        String lower = normalizedText.toLowerCase();

        // 1. Check for explicit unrelated queries
        if (UNRELATED_PATTERNS.matcher(lower).find() || lower.equals("tell me a joke") || lower.startsWith("recipe ") || lower.contains("weather today")) {
            return new ClassificationResult(NlpIntent.UNRELATED_QUERY, 0.95, false);
        }

        // 2. Multi-turn Follow-up Handling
        // Only treat as follow-up if context has an active pending slot AND the user message actually answers that slot,
        // without introducing a new question, topic shift, or educational query.
        boolean isTopicChangeOrQuestion = isTopicChangeOrNewQuestion(lower);

        if (!isTopicChangeOrQuestion && context != null && context.getAwaitingSlot() != null) {
            String slot = context.getAwaitingSlot();

            // A. Shopping scam payment & follow-up
            if ("shopping_payment_details".equals(slot) || 
                ("payment_channel".equals(slot) && context.getLastIntent() == NlpIntent.FAKE_SHOPPING_SCAM)) {
                boolean isPaymentAnswer = lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") ||
                        lower.contains("paytm") || lower.contains("bhim") || lower.contains("card") || lower.contains("debit") ||
                        lower.contains("credit") || lower.contains("cod") || lower.contains("cash on delivery") ||
                        lower.contains("netbanking") || lower.contains("net banking") || lower.contains("paid") ||
                        lower.contains("pay") || lower.equals("yes") || lower.equals("no") || lower.contains("entered it") ||
                        lower.contains("already") || lower.contains("havent") || lower.contains("haven't") ||
                        lower.contains("didnt") || lower.contains("didn't") || lower.contains("money") || lower.contains("transferred");
                if (isPaymentAnswer) {
                    return new ClassificationResult(NlpIntent.FAKE_SHOPPING_SCAM, 0.96, true);
                }
            }

            // B. Payment Channel follow-up
            if ("payment_channel".equals(slot) || (context.getLastIntent() == NlpIntent.UNKNOWN_DEBIT && 
                (lower.contains("upi") || lower.contains("card") || lower.contains("atm") || lower.contains("net banking") || lower.contains("netbanking")))) {
                if (lower.contains("upi") || lower.contains("gpay") || lower.contains("google pay") ||
                    lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bhim")) {
                    if (context.getLastIntent() == NlpIntent.FAKE_SHOPPING_SCAM) {
                        return new ClassificationResult(NlpIntent.FAKE_SHOPPING_SCAM, 0.96, true);
                    }
                    return new ClassificationResult(NlpIntent.UPI_FRAUD, 0.96, true);
                }
                if (lower.contains("card") || lower.contains("atm") || lower.contains("debit card") || lower.contains("credit card")) {
                    return new ClassificationResult(NlpIntent.ATM_CARD_FRAUD, 0.96, true);
                }
                if (lower.contains("net banking") || lower.contains("netbanking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps")) {
                    return new ClassificationResult(NlpIntent.UNKNOWN_DEBIT, 0.95, true);
                }
            }

            // C. OTP / Credential / Bank Details Disclosure follow-up
            if ("otp_or_link_shared".equals(slot) || "shared_otp".equals(slot) || "debit_or_otp_status".equals(slot)) {
                // User enters bank or card details
                if (lower.contains("entered my bank") || lower.contains("entered bank") || lower.contains("shared my bank") ||
                    lower.contains("bank details") || lower.contains("banking details") || lower.contains("account details") ||
                    lower.contains("card details") || lower.contains("entered card") || lower.contains("entered cvv") ||
                    Boolean.TRUE.equals(entities.getBankDetailsEntered())) {
                    return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.98, true);
                }

                // User entered password / credentials
                if (lower.contains("password") || lower.contains("credentials") || lower.contains("login details") ||
                    Boolean.TRUE.equals(entities.getCredentialsDisclosed())) {
                    return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.98, true);
                }

                // User entered OTP or PIN
                if (lower.equals("yes") || lower.equals("yes i did") || lower.equals("shared") || 
                    lower.contains("i shared") || lower.contains("gave otp") || lower.contains("i gave") || 
                    lower.contains("entered pin") || lower.contains("entered password") || lower.contains("typed password") ||
                    lower.contains("haan") || lower.contains("de diya") ||
                    lower.contains("shared the otp") || lower.contains("shared otp too") ||
                    lower.contains("entered it") || lower.equals("i entered it") ||
                    lower.contains("entered my otp") || lower.contains("gave my otp") || lower.contains("i gave it") ||
                    lower.contains("typed it") || lower.contains("shared it") || lower.contains("i typed it") ||
                    lower.equals("entered otp") || lower.equals("i entered my otp") ||
                    Boolean.TRUE.equals(entities.getSharedOtp())) {
                    return new ClassificationResult(NlpIntent.OTP_DISCLOSURE, 0.98, true);
                }

                if (lower.equals("no") || lower.equals("no i didnt") || lower.equals("never") || 
                    lower.contains("didnt share") || lower.contains("not shared") || lower.contains("nahi") || lower.contains("na") ||
                    lower.contains("only opened") || lower.contains("just opened") || lower.contains("only clicked") || 
                    lower.contains("just clicked") || lower.contains("only visited") || lower.contains("nothing entered") ||
                    lower.contains("did not enter") || lower.contains("did not share")) {
                    if (context.getLastIntent() == NlpIntent.FAKE_SHOPPING_SCAM) {
                        return new ClassificationResult(NlpIntent.FAKE_SHOPPING_SCAM, 0.96, true);
                    }
                    return new ClassificationResult(NlpIntent.PHISHING_URL_ANALYSIS, 0.96, true);
                }
            }

            // D. Account safety follow-up
            if ("account_safety_context".equals(slot)) {
                if (lower.contains("login") || lower.contains("hacked") || lower.contains("suspicious") || 
                    lower.contains("unknown") || lower.contains("stolen") || lower.contains("yes") || lower.contains("someone accessed")) {
                    return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.96, true);
                }
                return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.95, true);
            }

            // E. Amount follow-up
            if ("amount".equals(slot) && entities.getAmount() != null) {
                NlpIntent previous = context.getLastIntent() != null ? context.getLastIntent() : NlpIntent.UNKNOWN_DEBIT;
                return new ClassificationResult(previous, 0.95, true);
            }
        }

        // Conversational follow-up: Payment channel for ongoing unauthorized debit incident
        if (context != null && context.getLastIntent() == NlpIntent.UNKNOWN_DEBIT &&
            (lower.contains("upi") || lower.contains("card") || lower.contains("atm") || lower.contains("netbanking") || lower.contains("net banking"))) {
            if (lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bhim")) {
                return new ClassificationResult(NlpIntent.UPI_FRAUD, 0.96, true);
            }
            if (lower.contains("card") || lower.contains("atm")) {
                return new ClassificationResult(NlpIntent.ATM_CARD_FRAUD, 0.96, true);
            }
        }

        // Conversational pronoun resolution / continuation follow-up (e.g., "What is phishing?" -> "How do I identify it?")
        if (context != null && (context.getLastIntent() == NlpIntent.GENERAL_CYBER_SAFETY || context.getLastIntent() == NlpIntent.PHISHING_URL_ANALYSIS)) {
            if (lower.contains("identify it") || lower.contains("spot it") || lower.contains("recognize it") || 
                lower.contains("detect it") || lower.contains("avoid it") || lower.contains("prevent it") ||
                lower.contains("how do i know") || lower.contains("how can i tell") || lower.contains("how to identify it") ||
                lower.contains("how do i spot it") || lower.contains("how to spot it") || lower.contains("how do i recognize it")) {
                return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.96, true);
            }
        }

        // Conversational follow-up if user specifically says "what if i shared otp too" or "i also shared otp"
        if ((lower.contains("shared the otp") || lower.contains("shared my otp") || lower.contains("shared otp too") || lower.contains("gave the otp")) &&
            context != null && context.getLastIntent() != null) {
            return new ClassificationResult(NlpIntent.OTP_DISCLOSURE, 0.98, true);
        }

        // Incident continuation for follow-up questions (e.g. "What should I do now?", "Is it dangerous?", "What happens next?")
        if (isFollowUpQuery(lower) && context != null && isIncidentIntent(context.getLastIntent())) {
            return new ClassificationResult(context.getLastIntent(), 0.96, true);
        }

        // 3. Primary Intent Classification (Prevent keyword collision on purely informational queries)

        // Assistant Identity & Capabilities
        if (lower.contains("tell me about you") || lower.contains("tell me about u") || lower.contains("who are you") ||
            lower.contains("what are you") || lower.contains("what can you do") || lower.contains("what do you do") ||
            lower.contains("introduce yourself") || lower.equals("about you") || lower.equals("who made you") ||
            lower.contains("what is prahar") || lower.contains("about prahar") || lower.contains("how can you help") ||
            lower.contains("your features") || lower.contains("your capabilities") ||
            lower.contains("tell me about this app") || lower.contains("tell me about the app") ||
            lower.contains("about this app") || lower.contains("what does this app do") ||
            lower.contains("what is this app") || lower.contains("about this application") ||
            lower.contains("can you tell me about this app") || lower.contains("explain this app")) {
            return new ClassificationResult(NlpIntent.ASSISTANT_IDENTITY, 0.96, false);
        }

        // Educational: Identifying / detecting fake websites
        if (lower.contains("detect fake") || lower.contains("how do we detect") || lower.contains("how to detect fake") ||
            lower.contains("identify fake") || lower.contains("identify a fake") || lower.contains("spot a fake") ||
            lower.contains("spot fake") || lower.contains("ways to identify") || lower.contains("how to spot fake") ||
            lower.contains("how to identify")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.96, false);
        }

        // Fake Shopping & E-Commerce Scams (Orders, fake merchant websites, suspicious sellers)
        if (lower.contains("fake shopping") || lower.contains("shopping scam") || lower.contains("fake store") ||
            lower.contains("fake ecommerce") || lower.contains("fake website") || lower.contains("fake webpage") ||
            (lower.contains("shopping") && (lower.contains("fake") || lower.contains("scam") || lower.contains("fraud") || lower.contains("cheat") || lower.contains("suspicious"))) ||
            ((lower.contains("order") || lower.contains("ordered") || lower.contains("bought") || lower.contains("purchased") || lower.contains("item") || lower.contains("product")) &&
             (lower.contains("fake") || lower.contains("scam") || lower.contains("fraud") || lower.contains("suspicious") || lower.contains("bogus") || lower.contains("shopping") || lower.contains("unknown site"))) ||
            ((lower.contains("seller") || lower.contains("merchant") || lower.contains("store") || lower.contains("vendor")) &&
             (lower.contains("fake") || lower.contains("scam") || lower.contains("fraud") || lower.contains("suspicious") || lower.contains("cheat") || lower.contains("paid")))) {
            return new ClassificationResult(NlpIntent.FAKE_SHOPPING_SCAM, 0.96, false);
        }

        // Standalone educational terms
        if (lower.equals("phishing") || lower.equals("phishing?") || lower.equals("phishing!") ||
            lower.startsWith("phishing ") || lower.contains("about phishing") || lower.contains("explain phishing") ||
            lower.equals("otp") || lower.equals("otp?") || lower.equals("otp!") ||
            lower.equals("upi") || lower.equals("upi?") || lower.equals("upi!") ||
            lower.equals("firewall") || lower.equals("firewall?") || lower.equals("firewall!")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.95, false);
        }

        // Golden Hour inquiry
        if (lower.contains("golden hour")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.95, false);
        }

        // Fraud Reporting / Helpline / Complaint (Checked before general advice to catch helpline inquiries)
        if (lower.contains("report fraud") || lower.contains("file complaint") || lower.contains("complaint draft") || 
            lower.contains("how to report") || lower.contains("cyber cell complaint") || lower.contains("1930") ||
            lower.contains("helpline") || lower.contains("help line") || lower.contains("cyber helpline") ||
            lower.contains("helpline number") || lower.contains("emergency number") || lower.contains("toll free")) {
            return new ClassificationResult(NlpIntent.FRAUD_REPORTING, 0.94, false);
        }

        // Bank Frauds & Financial Cyber Fraud Inquiry
        if (lower.contains("bank fraud") || lower.contains("banking fraud") || lower.contains("financial fraud") || 
            lower.contains("bank frauds") || lower.contains("banking frauds") || lower.contains("financial frauds") ||
            lower.contains("help me with bank") || lower.contains("help with bank fraud")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.95, false);
        }

        // General Online Safety, Cyber Defense Advice & Core Educational Definitions
        if (lower.contains("how can i be safe") || lower.contains("how to be safe") || lower.contains("stay safe") ||
            lower.contains("safe from any attacks") || lower.contains("safe from attacks") || lower.contains("online frauds") ||
            lower.contains("online fraud") || lower.contains("protect myself online") || lower.contains("protect myself from") ||
            lower.contains("avoid scams") || lower.contains("prevent fraud") || lower.contains("prevent online fraud") ||
            lower.contains("how do i prevent") || lower.contains("safety tips") ||
            lower.contains("cyber hygiene") || lower.contains("safe online") || lower.contains("how does prahar work") || 
            lower.contains("antivirus") || lower.contains("what is phishing") || lower.contains("cyber safety") || lower.contains("cybersecurity") ||
            lower.contains("what is otp") || lower.contains("what is an otp") || lower.contains("define otp") || lower.contains("explain otp") ||
            lower.contains("what is upi") || lower.contains("what is a upi") || lower.contains("explain upi") || lower.contains("define upi") ||
            lower.contains("what is a firewall") || lower.contains("what is firewall") || lower.contains("explain firewall") || lower.contains("define firewall") ||
            lower.contains("what is 2fa") || lower.contains("two-factor") || lower.contains("two factor") ||
            lower.contains("session hijack") || lower.contains("session cookie") || lower.contains("cookie theft") ||
            lower.contains("steal a session") || lower.contains("steal session") ||
            (lower.contains("safe") && (lower.contains("attack") || lower.contains("fraud") || lower.contains("hack") || lower.contains("scam")))) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.95, false);
        }

        // Account Safety inquiry (e.g. "can u help me with account safety", "how to secure my account")
        if (lower.contains("account safety") || lower.contains("secure my account") || lower.contains("protect my account") ||
            lower.contains("account security") || lower.contains("help me with account safety") || lower.contains("keep account safe") ||
            (lower.contains("account") && (lower.contains("safety") || lower.contains("secure") || lower.contains("safe")))) {
            return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.95, false);
        }

        // Clicked suspicious link (e.g. "bro i clicked a sus link am i cooked", "clicked a link", "opened a suspicious link")
        if (lower.contains("clicked a") || lower.contains("clicked the link") || lower.contains("clicked suspicious") ||
            lower.contains("opened a link") || lower.contains("opened the link") || lower.contains("opened suspicious") ||
            lower.contains("compromised") || lower.contains("am i cooked") ||
            (lower.contains("clicked") && (lower.contains("link") || lower.contains("url") || lower.contains("website") || lower.contains("webpage"))) ||
            (lower.contains("opened") && (lower.contains("link") || lower.contains("url") || lower.contains("website") || lower.contains("webpage")))) {
            if (!lower.contains("otp") && !lower.contains("pin") && !lower.contains("password")) {
                return new ClassificationResult(NlpIntent.PHISHING_URL_ANALYSIS, 0.97, false);
            }
        }

        // Detecting fake websites / scam stores / identifying fake sites
        if (lower.contains("detect fake") || lower.contains("how do we detect") || lower.contains("how to detect fake") ||
            lower.contains("identify fake website") || lower.contains("spot a fake website") || lower.contains("spot fake website")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.96, false);
        }

        // Website Security & Defensive Architecture
        if (lower.contains("protect my website") || lower.contains("protect our website") || lower.contains("defend my website") ||
            lower.contains("website against phishing") || lower.contains("protect website from phishing")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.96, false);
        }

        // False Positive / Legitimate Website Flagged Investigation
        if (lower.contains("my website was flagged") || lower.contains("my legitimate website") || lower.contains("false positive") ||
            lower.contains("falsely flagged") || lower.contains("flagged as phishing")) {
            return new ClassificationResult(NlpIntent.EXPLAIN_SCAN, 0.96, false);
        }

        // Informational questions starting with "what is", "how does", "explain", "how can", etc.
        if (lower.startsWith("what is ") || lower.startsWith("how does ") || lower.startsWith("explain ") || lower.startsWith("why is ") ||
            lower.startsWith("how can ") || lower.startsWith("how to ") || lower.startsWith("how do i ") || lower.contains("defend against") ||
            lower.contains("machine learning") || lower.contains("what is ml") || lower.contains("how ml")) {
            if (lower.contains("phishing") || lower.contains("2fa") || lower.contains("https") || 
                lower.contains("malware") || lower.contains("ransomware") || lower.contains("encryption") ||
                lower.contains("firewall") || lower.contains("antivirus") || lower.contains("safe browsing") ||
                lower.contains("otp") || lower.contains("upi") || lower.contains("cyber hygiene") ||
                lower.contains("session") || lower.contains("cookie") || lower.contains("hijack") ||
                lower.contains("fraud") || lower.contains("scam") || lower.contains("spoof") ||
                lower.contains("machine learning") || lower.contains("ml")) {
                return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.93, false);
            }
        }

        // Explain Scan verdict
        if (lower.contains("why flagged") || lower.contains("explain scan") || lower.contains("explain result") || 
            lower.contains("why score") || (lower.contains("why") && lower.contains("flagged")) || lower.contains("why is this suspicious") ||
            lower.contains("why did prahar flag") || lower.contains("explain detection")) {
            return new ClassificationResult(NlpIntent.EXPLAIN_SCAN, 0.96, false);
        }

        // Phishing URL Analysis (explicit link inspection request or URL in message)
        if (lower.contains("http://") || lower.contains("https://") ||
            lower.contains("check this link") || lower.contains("is this link safe") || 
            lower.contains("verify this url") || lower.contains("check this url") || 
            lower.contains("scan this link") || lower.contains("analyze this url") ||
            lower.contains("can you analyze") || lower.contains("can you check") || lower.contains("can you scan") ||
            lower.contains("check this site") || lower.contains("scan this site") || lower.contains("scan http") ||
            lower.contains("check whether this url") || lower.contains("is this url safe")) {
            return new ClassificationResult(NlpIntent.PHISHING_URL_ANALYSIS, 0.96, false);
        }

        // User Scan History
        if (lower.contains("my scans") || lower.contains("scan history") || lower.contains("past scans") || lower.contains("recent scans")) {
            return new ClassificationResult(NlpIntent.USER_SCAN_HISTORY, 0.95, false);
        }

        // OTP / Password Disclosure (Incident)
        if (lower.contains("otp") || lower.contains("pin") || lower.contains("password") || lower.contains("verification code")) {
            if (lower.contains("shar") || lower.contains("enter") || lower.contains("gave") || lower.contains("typ") || 
                lower.contains("disclos") || lower.contains("leak") || lower.contains("de diya") || lower.contains("clicked") ||
                lower.contains("told") || lower.contains("sent") || lower.contains("provided")) {
                return new ClassificationResult(NlpIntent.OTP_DISCLOSURE, 0.97, false);
            }
        }

        // Stolen / Lost Phone
        if ((lower.contains("phone") || lower.contains("mobile") || lower.contains("handset") || lower.contains("device")) &&
            (lower.contains("stolen") || lower.contains("lost") || lower.contains("chori") || lower.contains("kho gaya") || lower.contains("theft"))) {
            return new ClassificationResult(NlpIntent.STOLEN_PHONE, 0.98, false);
        }

        // SIM Swap
        if ((lower.contains("sim") || lower.contains("network")) &&
            (lower.contains("swap") || lower.contains("deactivat") || lower.contains("no service") || lower.contains("no network") || lower.contains("blocked") || lower.contains("band"))) {
            return new ClassificationResult(NlpIntent.SIM_SWAP, 0.95, false);
        }

        // Malicious / Fake Bank App / APK
        if (Boolean.TRUE.equals(entities.getInstalledApk()) || lower.contains("fake app") || lower.contains("fake banking app") || 
            lower.contains("apk") || lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("screen share")) {
            return new ClassificationResult(NlpIntent.FAKE_BANK_APP, 0.94, false);
        }

        // Account Compromise & Banking Details Exposure
        if (Boolean.TRUE.equals(entities.getBankDetailsEntered()) ||
            lower.contains("entered my bank") || lower.contains("entered bank") || lower.contains("shared my bank") ||
            ((lower.contains("bank details") || lower.contains("banking details") || lower.contains("account details") ||
              lower.contains("card details")) &&
             (lower.contains("enter") || lower.contains("shar") || lower.contains("gave") || lower.contains("typ") ||
              lower.contains("provid") || lower.contains("fill") || lower.contains("put"))) ||
            lower.contains("account hacked") || lower.contains("account compromised") || lower.contains("hacked my account") || 
            lower.contains("unauthorized login") || lower.contains("password changed by someone")) {
            return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.96, false);
        }

        // UPI Fraud (Incident)
        if (lower.contains("upi fraud") || lower.contains("gpay fraud") || lower.contains("phonepe fraud") || 
            lower.contains("qr scan money debited") || (lower.contains("upi") && (lower.contains("scam") || lower.contains("lost") || lower.contains("cheat") || lower.contains("collect request") || lower.contains("unauthorized debit") || lower.contains("debited") || lower.contains("took money") || lower.contains("deducted")))) {
            return new ClassificationResult(NlpIntent.UPI_FRAUD, 0.97, false);
        }

        // ATM / Card Fraud
        if (lower.contains("atm fraud") || lower.contains("card fraud") || lower.contains("card cloned") || 
            lower.contains("unauthorized atm") || lower.contains("credit card fraud") || lower.contains("debit card fraud")) {
            return new ClassificationResult(NlpIntent.ATM_CARD_FRAUD, 0.95, false);
        }

        // Unknown / Unauthorized Debit
        if (Boolean.TRUE.equals(entities.getMoneyLost()) ||
            lower.contains("unauthorized debit") || lower.contains("debited") || lower.contains("deducted") ||
            lower.contains("kat gaya") || lower.contains("money deducted") || lower.contains("paise kat") ||
            lower.contains("money lost") || lower.contains("unknown debit") || lower.contains("stole") ||
            lower.contains("stolen from") || lower.contains("someone stole") ||
            lower.contains("transferred money to a scammer") || lower.contains("transferred money to scammer") ||
            lower.contains("sent money to a scammer") || lower.contains("paid a scammer") ||
            (entities.getDirection() != null && "DEBIT".equals(entities.getDirection()) && entities.getAmount() != null)) {
            return new ClassificationResult(NlpIntent.UNKNOWN_DEBIT, 0.93, false);
        }

        // Unknown Credit
        if (lower.contains("unknown credit") || lower.contains("unexpected credit") || lower.contains("money credited by mistake") || 
            lower.contains("wrong transfer received")) {
            return new ClassificationResult(NlpIntent.UNKNOWN_CREDIT, 0.92, false);
        }

        // Transaction Summary / Financial questions
        if (lower.contains("my money") || lower.contains("spending summary") || lower.contains("how much spent") || 
            lower.contains("total expense") || lower.contains("income summary") || lower.contains("transaction summary") || lower.contains("my transactions")) {
            return new ClassificationResult(NlpIntent.TRANSACTION_SUMMARY, 0.92, false);
        }

        // Transaction Categorization
        if (lower.contains("categorize") || lower.contains("category of") || lower.contains("which category")) {
            return new ClassificationResult(NlpIntent.TRANSACTION_CATEGORIZE, 0.88, false);
        }

        // Transaction Discrepancy
        if (lower.contains("discrepancy") || lower.contains("duplicate charge") || lower.contains("charged twice") || lower.contains("double debit")) {
            return new ClassificationResult(NlpIntent.TRANSACTION_DISCREPANCY, 0.91, false);
        }

        // Suspicious SMS / Message
        if (lower.contains("sms") || lower.contains("message") || lower.contains("kyc message") || lower.contains("whatsapp scam") || lower.contains("electricity cutoff")) {
            return new ClassificationResult(NlpIntent.SUSPICIOUS_MESSAGE, 0.93, false);
        }

        // Suspicious QR Code
        if (lower.contains("qr") || lower.contains("qr code") || lower.contains("scan qr")) {
            return new ClassificationResult(NlpIntent.SUSPICIOUS_QR, 0.93, false);
        }

        // Fraud Reporting / Helpline / Complaint
        if (lower.contains("report fraud") || lower.contains("file complaint") || lower.contains("complaint draft") || 
            lower.contains("how to report") || lower.contains("cyber cell complaint") || lower.contains("1930") ||
            lower.contains("helpline") || lower.contains("help line") || lower.contains("cyber helpline") ||
            lower.contains("helpline number") || lower.contains("emergency number") || lower.contains("toll free")) {
            return new ClassificationResult(NlpIntent.FRAUD_REPORTING, 0.94, false);
        }

        // Complaint Follow-up
        if (lower.contains("complaint status") || lower.contains("track complaint") || lower.contains("follow up on incident")) {
            return new ClassificationResult(NlpIntent.COMPLAINT_STATUS, 0.90, false);
        }

        // General Cyber Safety
        if (lower.contains("https") || lower.contains("safe online") || lower.contains("how does prahar work") || 
            lower.contains("antivirus") || lower.contains("what is phishing") || lower.contains("cyber safety") || lower.contains("cybersecurity")) {
            return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.90, false);
        }

        // Greetings / Pleasantries
        if (lower.equals("hi") || lower.equals("hello") || lower.equals("hey") || lower.equals("namaste") || lower.startsWith("hello ") || lower.startsWith("hi ")) {
            return new ClassificationResult(NlpIntent.AMBIGUOUS_QUERY, 0.60, false);
        }

        // Ambiguous Query
        return new ClassificationResult(NlpIntent.AMBIGUOUS_QUERY, 0.40, false);
    }

    public static boolean isFollowUpQuery(String lower) {
        if (lower == null) return false;
        String trimmed = lower.trim().replaceAll("[?.!]+$", "").trim();
        return trimmed.equals("what should i do now") ||
               trimmed.equals("what should i do next") ||
               trimmed.equals("what should i do") ||
               trimmed.equals("what do i do now") ||
               trimmed.equals("what do i do next") ||
               trimmed.equals("what do i do") ||
               trimmed.equals("what to do now") ||
               trimmed.equals("what to do next") ||
               trimmed.equals("what to do") ||
               trimmed.equals("what happens next") ||
               trimmed.equals("what next") ||
               trimmed.equals("now what") ||
               trimmed.equals("is it dangerous") ||
               trimmed.equals("is it risky") ||
               trimmed.equals("am i safe") ||
               trimmed.equals("am i cooked") ||
               trimmed.equals("what if i already entered it") ||
               trimmed.equals("what if i entered it") ||
               trimmed.equals("how can i protect my account") ||
               trimmed.equals("how do i protect my account") ||
               trimmed.equals("how to protect my account") ||
               trimmed.equals("what are the next steps") ||
               trimmed.equals("what should be my next step") ||
               trimmed.equals("what are my next steps") ||
               trimmed.equals("next steps") ||
               trimmed.equals("next step");
    }

    public static boolean isIncidentIntent(NlpIntent intent) {
        if (intent == null) return false;
        return intent == NlpIntent.PHISHING_URL_ANALYSIS ||
               intent == NlpIntent.ACCOUNT_COMPROMISE ||
               intent == NlpIntent.OTP_DISCLOSURE ||
               intent == NlpIntent.UNKNOWN_DEBIT ||
               intent == NlpIntent.UPI_FRAUD ||
               intent == NlpIntent.ATM_CARD_FRAUD ||
               intent == NlpIntent.FAKE_SHOPPING_SCAM ||
               intent == NlpIntent.FAKE_BANK_APP ||
               intent == NlpIntent.SIM_SWAP ||
               intent == NlpIntent.STOLEN_PHONE;
    }

    private static boolean isTopicChangeOrNewQuestion(String lower) {
        if (lower == null) return false;
        // Follow-up queries should NOT be considered a topic change
        if (isFollowUpQuery(lower)) {
            return false;
        }
        // Pronoun or continuation queries should NOT be considered a topic change
        if (lower.contains("identify it") || lower.contains("spot it") || lower.contains("recognize it") || lower.contains("detect it")) {
            return false;
        }
        return lower.startsWith("how do we detect") || lower.startsWith("how to detect") ||
                lower.startsWith("how can we detect") || lower.startsWith("how do i detect") ||
                lower.contains("detect fake websites") || lower.contains("detect fake website") ||
                lower.contains("identify fake") || lower.contains("spot a fake") ||
                lower.contains("ways to identify") || lower.contains("five ways") ||
                lower.contains("identify a fake") ||
                lower.startsWith("what is ") || lower.startsWith("what are ") ||
                lower.startsWith("how does ") || lower.startsWith("explain ") ||
                lower.startsWith("define ") ||
                lower.startsWith("why did prahar") || lower.startsWith("why was this") ||
                lower.startsWith("why is this") || lower.startsWith("why flagged") ||
                lower.startsWith("tell me about") || lower.contains("tell me about") ||
                lower.contains("about this app") || lower.contains("what does this app do") ||
                lower.startsWith("who are you") ||
                lower.contains("what is otp") || lower.contains("what is an otp") ||
                lower.contains("what is upi") || lower.contains("what is a upi") ||
                lower.contains("what is a firewall") || lower.contains("what is firewall") ||
                lower.contains("what is machine learning") || lower.contains("what is ml") ||
                lower.contains("does https") || lower.contains("is https") ||
                lower.contains("how can i protect my website") || lower.contains("protect our website") ||
                lower.contains("my website was flagged") || lower.contains("my legitimate website") ||
                lower.contains("false positive") ||
                lower.contains("golden hour") || lower.contains("helpline") || lower.contains("bank fraud") ||
                lower.contains("analyze this url") || lower.contains("check this url") ||
                lower.contains("can you analyze") || lower.contains("can you check") ||
                lower.contains("scan this url") || lower.contains("check this link") ||
                lower.contains("analyze this link") || lower.contains("scan this link");
    }
}
