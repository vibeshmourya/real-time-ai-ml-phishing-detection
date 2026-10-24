package com.phishshield.nlp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class NlpNormalizer {

    private static final Map<String, String> COMMON_MISSPELLINGS = new HashMap<>();

    static {
        // Spelling variations & common typo corrections
        COMMON_MISSPELLINGS.put("froud", "fraud");
        COMMON_MISSPELLINGS.put("fraude", "fraud");
        COMMON_MISSPELLINGS.put("debitt", "debit");
        COMMON_MISSPELLINGS.put("debitedd", "debited");
        COMMON_MISSPELLINGS.put("creditt", "credit");
        COMMON_MISSPELLINGS.put("creadit", "credit");
        COMMON_MISSPELLINGS.put("acount", "account");
        COMMON_MISSPELLINGS.put("accound", "account");
        COMMON_MISSPELLINGS.put("passward", "password");
        COMMON_MISSPELLINGS.put("passwrod", "password");
        COMMON_MISSPELLINGS.put("mesage", "message");
        COMMON_MISSPELLINGS.put("msg", "message");
        COMMON_MISSPELLINGS.put("sms", "sms");
        COMMON_MISSPELLINGS.put("phising", "phishing");
        COMMON_MISSPELLINGS.put("phisin", "phishing");
        COMMON_MISSPELLINGS.put("apkk", "apk");
        COMMON_MISSPELLINGS.put("opt", "otp");
        COMMON_MISSPELLINGS.put("otpp", "otp");
        COMMON_MISSPELLINGS.put("upii", "upi");
        COMMON_MISSPELLINGS.put("gpayy", "gpay");
        COMMON_MISSPELLINGS.put("phonpe", "phonepe");
        COMMON_MISSPELLINGS.put("paytmm", "paytm");
        COMMON_MISSPELLINGS.put("kat gaya", "debited");
        COMMON_MISSPELLINGS.put("kat gye", "debited");
        COMMON_MISSPELLINGS.put("paise kat", "debited money");
        COMMON_MISSPELLINGS.put("paisa kat", "debited money");
        COMMON_MISSPELLINGS.put("sim band", "sim swap");
        COMMON_MISSPELLINGS.put("phone chori", "stolen phone");
        COMMON_MISSPELLINGS.put("mobile kho gaya", "stolen phone");
    }

    private static final Pattern CLEAN_PUNCTUATION = Pattern.compile("[^a-zA-Z0-9\\s₹.,@\\-_/]");

    public static String normalize(String raw) {
        return normalizeText(raw);
    }

    public static String normalizeText(String raw) {
        if (raw == null) return "";
        String text = raw.trim().toLowerCase();

        // Standardize currency symbols
        text = text.replace("rs.", "₹ ").replace("rs ", "₹ ").replace("inr ", "₹ ").replace("rupees", "₹");

        // Clean extra strange punctuation but keep meaningful symbols
        text = CLEAN_PUNCTUATION.matcher(text).replaceAll(" ");

        // Replace known slang/spelling variations
        for (Map.Entry<String, String> entry : COMMON_MISSPELLINGS.entrySet()) {
            text = text.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }

        // Collapse multiple spaces
        return text.replaceAll("\\s+", " ").trim();
    }

    public static String detectLanguageSignal(String text) {
        if (text == null) return "en";
        String lower = text.toLowerCase();
        if (lower.contains("kya") || lower.contains("kaise") || lower.contains("paise") || 
            lower.contains("batao") || lower.contains("kare") || lower.contains("gaya") || lower.contains("chori")) {
            return "hi-en"; // Hinglish signal
        }
        return "en";
    }
}
