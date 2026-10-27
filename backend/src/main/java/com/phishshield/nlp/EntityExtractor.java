package com.phishshield.nlp;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractor {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)|\\b([0-9]+(?:,[0-9]+)?)\\s*(?:rs|rupees|inr)\\b");
    private static final Pattern SHORT_AMOUNT_PATTERN = Pattern.compile("\\b([0-9]+)\\s*k\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|rrn|upi\\s*ref|txn\\s*id|reference)\\s*[:#-]?\\s*([a-zA-Z0-9]{6,22})");

    public static ExtractedEntities extract(String normalizedText) {
        ExtractedEntities entities = new ExtractedEntities();
        if (normalizedText == null || normalizedText.isBlank()) return entities;

        String lower = normalizedText.toLowerCase();

        // 1. Language detection
        entities.setDetectedLanguage(NlpNormalizer.detectLanguageSignal(normalizedText));

        // 2. Extract Amount
        Matcher amtMatcher = AMOUNT_PATTERN.matcher(lower);
        if (amtMatcher.find()) {
            String rawVal = amtMatcher.group(1) != null ? amtMatcher.group(1) : amtMatcher.group(2);
            if (rawVal != null) {
                try {
                    entities.setAmount(new BigDecimal(rawVal.replace(",", "")));
                    entities.setCurrency("INR");
                } catch (Exception ignored) {}
            }
        } else {
            // Check short format like "5k"
            Matcher shortMatcher = SHORT_AMOUNT_PATTERN.matcher(lower);
            if (shortMatcher.find()) {
                try {
                    long kVal = Long.parseLong(shortMatcher.group(1)) * 1000;
                    entities.setAmount(BigDecimal.valueOf(kVal));
                    entities.setCurrency("INR");
                } catch (Exception ignored) {}
            }
        }

        // 3. Payment Channel
        if (lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bhim")) {
            entities.setPaymentChannel("UPI");
        } else if (lower.contains("atm") || lower.contains("cash withdrawal")) {
            entities.setPaymentChannel("ATM");
        } else if (lower.contains("card") || lower.contains("debit card") || lower.contains("credit card") || lower.contains("pos")) {
            entities.setPaymentChannel("CARD");
        } else if (lower.contains("qr") || lower.contains("qr code")) {
            entities.setPaymentChannel("QR");
        } else if (lower.contains("netbanking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps")) {
            entities.setPaymentChannel("NETBANKING");
        }

        // 4. Direction
        if (lower.contains("debited") || lower.contains("spent") || lower.contains("kat gaya") || lower.contains("deducted") || lower.contains("lost") || lower.contains("sent") || lower.contains("stole") || lower.contains("stolen") || lower.contains("taken")) {
            entities.setDirection("DEBIT");
        } else if (lower.contains("credited") || lower.contains("received") || lower.contains("refund") || lower.contains("got money")) {
            entities.setDirection("CREDIT");
        }

        // 5. Institution / Service Name
        if (lower.contains("sbi") || lower.contains("state bank")) entities.setInstitution("State Bank of India (SBI)");
        else if (lower.contains("hdfc")) entities.setInstitution("HDFC Bank");
        else if (lower.contains("icici")) entities.setInstitution("ICICI Bank");
        else if (lower.contains("axis")) entities.setInstitution("Axis Bank");
        else if (lower.contains("kotak")) entities.setInstitution("Kotak Mahindra Bank");
        else if (lower.contains("pnb")) entities.setInstitution("Punjab National Bank");
        else if (lower.contains("airtel")) entities.setInstitution("Airtel");
        else if (lower.contains("jio")) entities.setInstitution("Jio");
        else if (lower.contains("income tax")) entities.setInstitution("Income Tax Dept");
        else if (lower.contains("bescom") || lower.contains("electricity")) entities.setInstitution("Electricity Board");
        else if (lower.contains("amazon")) entities.setInstitution("Amazon");
        else if (lower.contains("flipkart")) entities.setInstitution("Flipkart");

        // 6. Security compromise & incident facts
        if (lower.contains("clicked") || lower.contains("opened the link") || lower.contains("tapped the link") || lower.contains("visited")) {
            entities.setClickedLink(true);
        }

        // Bank details disclosure (confirmed fact)
        if (lower.contains("entered my bank") || lower.contains("entered bank") || lower.contains("shared my bank") ||
            lower.contains("entered bank details") || lower.contains("shared bank details") ||
            ((lower.contains("bank detail") || lower.contains("banking detail") || lower.contains("account detail") ||
              lower.contains("card detail") || lower.contains("debit card") || lower.contains("credit card") ||
              lower.contains("cvv") || lower.contains("account number")) &&
             (lower.contains("enter") || lower.contains("shar") || lower.contains("gave") || lower.contains("typ") ||
              lower.contains("provid") || lower.contains("put") || lower.contains("fill")))) {
            entities.setBankDetailsEntered(true);
        }

        // OTP / PIN disclosure (confirmed fact)
        if ((lower.contains("otp") || lower.contains("verification code") || lower.contains("one time password") ||
             lower.contains("upi pin") || lower.contains("atm pin") || lower.contains("security code")) &&
            (lower.contains("shar") || lower.contains("gave") || lower.contains("enter") || lower.contains("typ") ||
             lower.contains("provid") || lower.contains("sent") || lower.contains("told") || lower.contains("leak") ||
             lower.contains("de diya") || lower.contains("shared code"))) {
            entities.setSharedOtp(true);
        } else if (lower.equals("shared otp") || lower.equals("gave otp") || lower.equals("entered otp") ||
                   lower.contains("shared the otp") || lower.contains("shared otp too")) {
            entities.setSharedOtp(true);
        }

        // Password / login credentials disclosure
        if ((lower.contains("password") || lower.contains("credentials") || lower.contains("login details") ||
             lower.contains("netbanking password")) &&
            (lower.contains("enter") || lower.contains("shar") || lower.contains("gave") || lower.contains("typ") ||
             lower.contains("fill") || lower.contains("provid"))) {
            entities.setCredentialsDisclosed(true);
        }

        // Malicious App / APK / Remote access
        if (lower.contains("apk") || lower.contains("installed app") || lower.contains("downloaded app") ||
            lower.contains("anydesk") || lower.contains("teamviewer") || lower.contains("rustdesk")) {
            entities.setInstalledApk(true);
        }

        // Unauthorized debit / money loss (confirmed fact)
        if (lower.contains("money deducted") || lower.contains("money was deducted") || lower.contains("debited") ||
            lower.contains("kat gaya") || lower.contains("paise kat") || lower.contains("money lost") ||
            lower.contains("unauthorized debit") || lower.contains("someone stole") ||
            lower.contains("transferred money to a scammer") || lower.contains("sent money to a scammer")) {
            entities.setMoneyLost(true);
        }

        // Protective action already taken by user
        if (lower.contains("blocked my card") || lower.contains("froze my card") || lower.contains("froze my account") ||
            lower.contains("changed my password") || lower.contains("already reported") || lower.contains("already called") ||
            lower.contains("already blocked") || lower.contains("locked my card")) {
            entities.setProtectiveActionTaken(true);
        }

        // 7. Transaction Reference
        Matcher refMatcher = REF_PATTERN.matcher(lower);
        if (refMatcher.find()) {
            entities.setTransactionRef(refMatcher.group(1));
        }

        return entities;
    }
}
