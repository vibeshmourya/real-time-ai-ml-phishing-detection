package com.phishshield.util;

import java.util.regex.Pattern;

/**
 * Utility for scrubbing payment card numbers, OTPs, PINs, CVVs, and auth tokens
 * prior to database persistence and audit logging.
 */
public final class SensitiveDataScrubber {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern OTP_PIN_PATTERN = Pattern.compile("(?i)\\b(?:otp|pin|cvv|code|password)\\s*(?:is|was|[:=])?\\s*(\\d{4,8})\\b");
    private static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("(?i)Bearer\\s+[a-zA-Z0-9_.-]+");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(?:key|secret|token)=[a-zA-Z0-9_.-]+");

    private SensitiveDataScrubber() {}

    public static String scrub(String input) {
        if (input == null) return "";
        String sanitized = input.trim();
        sanitized = CARD_NUMBER_PATTERN.matcher(sanitized).replaceAll("[REDACTED_CARD_NUMBER]");
        sanitized = OTP_PIN_PATTERN.matcher(sanitized).replaceAll("OTP/PIN: [REDACTED_SECRET]");
        sanitized = AUTH_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("key=[REDACTED]");
        return sanitized;
    }
}
