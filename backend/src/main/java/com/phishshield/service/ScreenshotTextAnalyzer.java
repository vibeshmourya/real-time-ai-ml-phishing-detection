package com.phishshield.service;

import com.phishshield.domain.IndicatorSeverity;
import com.phishshield.domain.IndicatorType;
import com.phishshield.dto.response.OCRTextAnalysisResult;
import com.phishshield.dto.response.ThreatIndicatorResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ScreenshotTextAnalyzer implements explainable heuristic rules for text extracted from screenshots:
 * 1. Credential harvesting (passwords, PINs, NetBanking credentials).
 * 2. OTP solicitation (one-time passwords, verification codes).
 * 3. Coercive urgency language (threats of 24h account blockage).
 * 4. Financial lures or unauthorized transaction warnings.
 * 5. Brand impersonation cues.
 * 6. Obfuscated URL shorteners.
 * 
 * Safety Rule: A single indicator (e.g. standard login form text) does NOT classify an image as malicious.
 */
@Component
public class ScreenshotTextAnalyzer {

    private static final Pattern PATTERN_CREDENTIAL = Pattern.compile(
            "(?i)\\b(enter|provide|update|verify|confirm|input)\\s+(your\\s+)?(password|netbanking password|passcode|login credentials|pin|cvv)\\b"
    );

    private static final Pattern PATTERN_OTP = Pattern.compile(
            "(?i)\\b(enter|share|submit|verify|forward|send|provide)\\s+(your\\s+|the\\s+)?([0-9a-zA-Z-]+\\s+)*(otp|one-time password|verification code|security code|passcode)\\b|" +
            "(?i)\\b(otp|one-time password)\\s+(verification\\s+)?(code|pin)\\b"
    );

    private static final Pattern PATTERN_URGENCY = Pattern.compile(
            "(?i)\\b(within 24 hours|within 12 hours|immediate action required|account (will be )?blocked|permanently suspended|action required immediately|access blocked|suspended immediately)\\b"
    );

    private static final Pattern PATTERN_FINANCIAL = Pattern.compile(
            "(?i)\\b(unauthorized transaction|debited from your account|income tax refund|lottery winner|claim reward|cashback credited|reward points expiring|account deactivated)\\b"
    );

    private static final Pattern PATTERN_BRAND = Pattern.compile(
            "(?i)\\b(hdfc( bank)?|sbi|state bank( of india)?|icici( bank)?|axis bank|paytm( payments bank)?|phonepe|google pay|jio payment)\\b"
    );

    private static final Pattern PATTERN_SHORTENER = Pattern.compile(
            "(?i)\\b(bit\\.ly|tinyurl\\.com|is\\.gd|t\\.co|cutt\\.ly|rb\\.gy|shorturl\\.at)\\b"
    );

    /**
     * Evaluates text for visual phishing indicators and returns an OCRTextAnalysisResult.
     */
    public OCRTextAnalysisResult analyzeText(String text, String extractionStatus, String ocrEngine) {
        if (text == null || text.trim().isEmpty()) {
            return OCRTextAnalysisResult.builder()
                    .extractedText("")
                    .characterCount(0)
                    .extractionStatus(extractionStatus != null ? extractionStatus : "EMPTY_INPUT")
                    .ocrEngine(ocrEngine != null ? ocrEngine : "None")
                    .heuristicRiskScore(0)
                    .heuristicRiskLevel("SAFE")
                    .summary("No readable text content was detected in this screenshot.")
                    .textIndicators(new ArrayList<>())
                    .build();
        }

        String content = text.trim();
        List<ThreatIndicatorResponse> indicators = new ArrayList<>();
        double accumulatedWeight = 0;

        // 1. Credential Solicitation Rule
        if (PATTERN_CREDENTIAL.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("Credential Solicitation")
                    .description("Screenshot text explicitly requests confidential passwords, credentials, or security PINs.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_OCR_CREDENTIAL_SOLICITATION")
                    .weight(25.0)
                    .build());
            accumulatedWeight += 25.0;
        }

        // 2. OTP Solicitation Rule
        if (PATTERN_OTP.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("One-Time Password (OTP) Solicitation")
                    .description("Screenshot text requests sharing or submitting a one-time authorization code.")
                    .severity(IndicatorSeverity.CRITICAL)
                    .ruleTriggered("RULE_OCR_OTP_SOLICITATION")
                    .weight(35.0)
                    .build());
            accumulatedWeight += 35.0;
        }

        // 3. Urgent Coercion Rule
        if (PATTERN_URGENCY.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("High-Urgency Suspension Language")
                    .description("Text employs coercive countdown threats (e.g. 24 hours) threatening account termination.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_OCR_URGENCY_COERCION")
                    .weight(20.0)
                    .build());
            accumulatedWeight += 20.0;
        }

        // 4. Financial Lure or Unauthorized Debit Rule
        if (PATTERN_FINANCIAL.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("Financial Lure / Fraudulent Alert")
                    .description("Text references unauthorized debits, refunds, or lottery prizes to induce hasty action.")
                    .severity(IndicatorSeverity.MEDIUM)
                    .ruleTriggered("RULE_OCR_FINANCIAL_LURE")
                    .weight(15.0)
                    .build());
            accumulatedWeight += 15.0;
        }

        // 5. Brand Impersonation Cue
        if (PATTERN_BRAND.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.CONTENT)
                    .indicatorName("Brand Identification")
                    .description("Text mentions prominent financial institutions or telecom brands.")
                    .severity(IndicatorSeverity.LOW)
                    .ruleTriggered("RULE_OCR_BRAND_REFERENCE")
                    .weight(10.0)
                    .build());
            accumulatedWeight += 10.0;
        }

        // 6. Suspicious Shortened URL Rule
        if (PATTERN_SHORTENER.matcher(content).find()) {
            indicators.add(ThreatIndicatorResponse.builder()
                    .indicatorType(IndicatorType.LEXICAL)
                    .indicatorName("Obfuscated URL Shortener")
                    .description("Screenshot displays shortened link concealing actual destination address.")
                    .severity(IndicatorSeverity.HIGH)
                    .ruleTriggered("RULE_OCR_SHORTENED_URL")
                    .weight(20.0)
                    .build());
            accumulatedWeight += 20.0;
        }

        // Defensive normalization: A single isolated indicator does not imply high malicious intent
        int finalScore;
        if (indicators.isEmpty()) {
            finalScore = 0;
        } else if (indicators.size() == 1) {
            finalScore = Math.min(25, (int) Math.round(accumulatedWeight * 0.7));
        } else {
            finalScore = Math.min(100, (int) Math.round(accumulatedWeight));
        }

        String riskLevel = finalScore >= 70 ? "HIGH_RISK" : (finalScore >= 35 ? "SUSPICIOUS" : "SAFE");

        String summary = generateSummary(indicators, finalScore, riskLevel);

        return OCRTextAnalysisResult.builder()
                .extractedText(content)
                .characterCount(content.length())
                .extractionStatus(extractionStatus != null ? extractionStatus : "SUCCESS")
                .ocrEngine(ocrEngine != null ? ocrEngine : "Heuristic Text Scanner")
                .heuristicRiskScore(finalScore)
                .heuristicRiskLevel(riskLevel)
                .summary(summary)
                .textIndicators(indicators)
                .build();
    }

    private String generateSummary(List<ThreatIndicatorResponse> indicators, int score, String riskLevel) {
        if (indicators.isEmpty()) {
            return "No suspicious phishing or social-engineering language patterns were identified in the extracted text.";
        }
        if (score >= 70) {
            return "High-risk social-engineering patterns detected: text exhibits correlated signals of urgent coercion, credential demand, or OTP solicitation.";
        }
        if (score >= 35) {
            return "Caution advised: screenshot text contains notable risk cues that warrant verification before submitting credentials.";
        }
        return "Low risk: minor keywords detected (e.g. standard login or brand reference), but lacking aggressive phishing indicators.";
    }
}
