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
        // Only treat as follow-up if context has an active pending slot AND the user message answers that slot,
        // without introducing an unrelated new topic.
        if (context != null && context.getAwaitingSlot() != null) {
            String slot = context.getAwaitingSlot();

            // A. Payment Channel follow-up
            if ("payment_channel".equals(slot)) {
                if (lower.equals("upi") || lower.contains("gpay") || lower.contains("google pay") ||
                    lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bhim")) {
                    return new ClassificationResult(NlpIntent.UPI_FRAUD, 0.96, true);
                }
                if (lower.contains("card") || lower.contains("atm") || lower.contains("debit card") || lower.contains("credit card")) {
                    return new ClassificationResult(NlpIntent.ATM_CARD_FRAUD, 0.96, true);
                }
                if (lower.contains("net banking") || lower.contains("netbanking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps")) {
                    return new ClassificationResult(NlpIntent.UNKNOWN_DEBIT, 0.95, true);
                }
            }

            // B. OTP / Credential Disclosure follow-up
            if ("otp_or_link_shared".equals(slot) || "shared_otp".equals(slot)) {
                if (lower.equals("yes") || lower.equals("yes i did") || lower.equals("shared") || 
                    lower.contains("i shared") || lower.contains("gave otp") || lower.contains("i gave") || 
                    lower.contains("entered pin") || lower.contains("haan") || lower.contains("de diya") ||
                    lower.contains("shared the otp") || lower.contains("shared otp too")) {
                    return new ClassificationResult(NlpIntent.OTP_DISCLOSURE, 0.98, true);
                }
                if (lower.equals("no") || lower.equals("no i didnt") || lower.equals("never") || 
                    lower.contains("didnt share") || lower.contains("not shared") || lower.contains("nahi") || lower.contains("na")) {
                    NlpIntent previous = context.getLastIntent() != null ? context.getLastIntent() : NlpIntent.UNKNOWN_DEBIT;
                    return new ClassificationResult(previous, 0.94, true);
                }
            }

            // C. Amount follow-up
            if ("amount".equals(slot) && entities.getAmount() != null) {
                NlpIntent previous = context.getLastIntent() != null ? context.getLastIntent() : NlpIntent.UNKNOWN_DEBIT;
                return new ClassificationResult(previous, 0.95, true);
            }
        }

        // Also check conversational follow-up if user specifically says "what if i shared otp too" or "i also shared otp"
        if ((lower.contains("shared the otp") || lower.contains("shared my otp") || lower.contains("shared otp too") || lower.contains("gave the otp")) &&
            context != null && context.getLastIntent() != null) {
            return new ClassificationResult(NlpIntent.OTP_DISCLOSURE, 0.98, true);
        }

        // 3. Primary Intent Classification (Prevent keyword collision on purely informational queries)

        // Informational questions starting with "what is", "how does", "explain"
        if (lower.startsWith("what is ") || lower.startsWith("how does ") || lower.startsWith("explain ") || lower.startsWith("why is ")) {
            if (lower.contains("phishing") || lower.contains("2fa") || lower.contains("https") || 
                lower.contains("malware") || lower.contains("ransomware") || lower.contains("encryption") ||
                lower.contains("firewall") || lower.contains("antivirus") || lower.contains("safe browsing") ||
                lower.contains("otp") || lower.contains("upi") || lower.contains("cyber hygiene")) {
                return new ClassificationResult(NlpIntent.GENERAL_CYBER_SAFETY, 0.93, false);
            }
        }

        // Explain Scan verdict
        if (lower.contains("why flagged") || lower.contains("explain scan") || lower.contains("explain result") || 
            lower.contains("why score") || lower.contains("why was this link flagged") || lower.contains("why is this suspicious")) {
            return new ClassificationResult(NlpIntent.EXPLAIN_SCAN, 0.96, false);
        }

        // Phishing URL Analysis (explicit link inspection request)
        if (lower.contains("check this link") || lower.contains("is this link safe") || 
            lower.contains("verify this url") || lower.contains("check this url") || 
            lower.contains("scan this link") || lower.startsWith("http://") || lower.startsWith("https://")) {
            return new ClassificationResult(NlpIntent.PHISHING_URL_ANALYSIS, 0.96, false);
        }

        // User Scan History
        if (lower.contains("my scans") || lower.contains("scan history") || lower.contains("past scans") || lower.contains("recent scans")) {
            return new ClassificationResult(NlpIntent.USER_SCAN_HISTORY, 0.95, false);
        }

        // OTP / Password Disclosure (Incident)
        if (lower.contains("otp") || lower.contains("pin") || lower.contains("password")) {
            if (lower.contains("shar") || lower.contains("enter") || lower.contains("gave") || lower.contains("typ") || 
                lower.contains("disclos") || lower.contains("leak") || lower.contains("de diya") || lower.contains("clicked")) {
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

        // Account Compromise
        if (lower.contains("account hacked") || lower.contains("account compromised") || lower.contains("hacked my account") || 
            lower.contains("unauthorized login") || lower.contains("password changed by someone")) {
            return new ClassificationResult(NlpIntent.ACCOUNT_COMPROMISE, 0.94, false);
        }

        // UPI Fraud (Incident)
        if (lower.contains("upi fraud") || lower.contains("gpay fraud") || lower.contains("phonepe fraud") || 
            lower.contains("qr scan money debited") || (lower.contains("upi") && (lower.contains("scam") || lower.contains("lost") || lower.contains("cheat") || lower.contains("collect request")))) {
            return new ClassificationResult(NlpIntent.UPI_FRAUD, 0.96, false);
        }

        // ATM / Card Fraud
        if (lower.contains("atm fraud") || lower.contains("card fraud") || lower.contains("card cloned") || 
            lower.contains("unauthorized atm") || lower.contains("credit card fraud") || lower.contains("debit card fraud")) {
            return new ClassificationResult(NlpIntent.ATM_CARD_FRAUD, 0.95, false);
        }

        // Unknown / Unauthorized Debit
        if (lower.contains("unauthorized debit") || lower.contains("debited") || lower.contains("kat gaya") || 
            lower.contains("money deducted") || lower.contains("paise kat") || lower.contains("money lost") || 
            lower.contains("unknown debit") || (entities.getDirection() != null && "DEBIT".equals(entities.getDirection()) && entities.getAmount() != null)) {
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

        // Fraud Reporting / Complaint
        if (lower.contains("report fraud") || lower.contains("file complaint") || lower.contains("complaint draft") || 
            lower.contains("how to report") || lower.contains("cyber cell complaint") || lower.contains("1930")) {
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
}
