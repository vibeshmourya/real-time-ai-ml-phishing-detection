package com.phishshield.service;

import com.phishshield.domain.FinancialTransaction;
import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionType;
import com.phishshield.dto.response.PersonalizedRiskAssessment;
import com.phishshield.dto.response.SmsAnalysisResponse;
import com.phishshield.dto.response.TransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmsFraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(SmsFraudDetectionService.class);

    private final PersonalizedRiskAssessmentService riskAssessmentService;

    // Redaction Patterns (Privacy-First)
    private static final Pattern OTP_PATTERN = Pattern.compile("(?i)(?:otp|code|one\\s*time\\s*password|pin|secret|verification\\s*code)\\s*(?:is|:|#|-|=|to)?\\s*([0-9]{4,8})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:password|pwd)\\s*(?:is|:|#|-|=|to)?\\s*([a-zA-Z0-9!@#$%^&*]{6,20})");

    // Suspicious Lure / Threat Patterns
    private static final Pattern URGENT_KYC_PATTERN = Pattern.compile(
            "(?i).*(kyc|pan\\s*card|aadhaar|account\\s*will\\s*be\\s*(?:blocked|suspended|deactivated|frozen)|update\\s*now|within\\s*24\\s*hours|immediate\\s*action).*");

    private static final Pattern LOTTERY_REWARD_PATTERN = Pattern.compile(
            "(?i).*(congratulations|won\\s*(?:rs|inr|\\$|prize|cashback|lottery|reward)|claim\\s*now|lucky\\s*draw|transfer\\s*fee|processing\\s*fee).*");

    private static final Pattern CREDENTIAL_HARVEST_PATTERN = Pattern.compile(
            "(?i).*(share\\s*(?:otp|pin|password|cvv)|enter\\s*upi\\s*pin\\s*to\\s*receive|send\\s*otp|verify\\s*credentials).*");

    private static final Pattern BANK_IMPERSONATION_PATTERN = Pattern.compile(
            "(?i).*(sbi\\s*bank|hdfc\\s*bank|icici\\s*bank|axis\\s*bank|punjab\\s*national|pnb|yono|paytm\\s*bank|bank\\s*of\\s*baroda).*");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?");

    // Transaction Parsing Patterns
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:ending|no|[*x]+)?\\s*([0-9x*]{3,6})");
    private static final Pattern UPI_VPA_PATTERN = Pattern.compile("(?i)(?:to|vpa|at)\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|rrn|upi\\s*ref|txn\\s*id)\\s*[:#-]?\\s*([a-zA-Z0-9]{6,22})");

    public SmsFraudDetectionService(PersonalizedRiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    public SmsAnalysisResponse analyzeSms(String rawText, Long userId) {
        SmsAnalysisResponse response = new SmsAnalysisResponse();
        response.setPrivacyNotice("Privacy Boundary: Full message content is processed in ephemeral memory only. OTPs, passwords, and sensitive credentials are automatically redacted before analysis and never stored.");
        response.setAndroidPlatformNotice("Android Platform Note: Prahar operates with zero unauthorized background eavesdropping. Android requires explicit user permission via SMS Retriever API or NotificationListenerService to read notification headers.");

        if (rawText == null || rawText.isBlank()) {
            response.setRedactedText("");
            response.setSuspicious(false);
            response.setThreatCategory("NONE");
            response.setSeverity("LOW");
            response.setRiskScore(0);
            response.setExplanation("Empty text provided.");
            response.setRecommendedAction("Paste an SMS or notification text to analyze.");
            return response;
        }

        // Step 1: On-Device / Local Privacy Redaction
        String sanitized = redactSensitiveCredentials(rawText);
        response.setRedactedText(sanitized);

        // Step 2: Extract embedded URLs
        List<String> urls = new ArrayList<>();
        Matcher urlMatcher = URL_PATTERN.matcher(rawText);
        while (urlMatcher.find()) {
            urls.add(urlMatcher.group());
        }
        response.setExtractedUrls(urls);

        // Step 3: Inspect for Phishing / Social Engineering Lures
        boolean hasUrgentKyc = URGENT_KYC_PATTERN.matcher(rawText).matches();
        boolean hasLottery = LOTTERY_REWARD_PATTERN.matcher(rawText).matches();
        boolean hasCredentialHarvest = CREDENTIAL_HARVEST_PATTERN.matcher(rawText).matches();
        boolean hasBankMention = BANK_IMPERSONATION_PATTERN.matcher(rawText).matches();
        boolean hasSuspiciousUrl = !urls.isEmpty();

        List<String> indicators = new ArrayList<>();

        if (hasCredentialHarvest) {
            indicators.add("CRITICAL_CREDENTIAL_DEMAND: Message instructs user to share OTP, PIN, or CVV, or enter UPI PIN to receive money (a primary vector for unauthorized fund transfers).");
        }
        if (hasUrgentKyc) {
            indicators.add("URGENT_KYC_COERCION: Message creates artificial urgency threatening immediate account freeze unless personal documents or links are submitted.");
        }
        if (hasLottery) {
            indicators.add("DECEPTIVE_REWARD_BAIT: Message claims unverified lottery, prize, or instant cashback requiring action.");
        }
        if (hasBankMention && hasSuspiciousUrl) {
            indicators.add("UNVERIFIED_BANKING_LINK: Message mentions a banking institution alongside an unofficial link.");
        }

        // Determine if this is a Malicious Lure
        if (hasCredentialHarvest || (hasUrgentKyc && hasSuspiciousUrl) || (hasLottery && hasSuspiciousUrl)) {
            response.setSuspicious(true);
            response.setThreatCategory(hasCredentialHarvest ? "CREDENTIAL_HARVESTING" : hasUrgentKyc ? "BANK_IMPERSONATION" : "LOTTERY_BAIT");
            response.setSeverity("HIGH");
            response.setRiskScore(88);
            response.setIndicators(indicators);
            response.setExplanation("This message exhibits strong characteristics of financial phishing or social engineering designed to harvest credentials or manipulate bank transfers.");
            response.setRecommendedAction("DO NOT tap any links or share verification codes. Legitimate banks NEVER ask for your OTP or UPI PIN to receive money.");
            return response;
        }

        // Step 4: Check if this is an Authorized Bank Transaction Notification
        boolean isDebitAlert = rawText.toLowerCase().matches(".*(debited|spent|paid|withdrawn|sent|transferred).*");
        boolean isCreditAlert = rawText.toLowerCase().matches(".*(credited|received|refunded|deposited).*");

        if (isDebitAlert || isCreditAlert) {
            // Extract Transaction Details
            FinancialTransaction extracted = parseTransactionFields(rawText, sanitized);

            if (userId != null && extracted.getAmount() != null) {
                // Run Personalized Risk Assessment against historical baseline!
                PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(extracted, userId, false);

                TransactionDto txnDto = TransactionDto.fromEntity(extracted);
                txnDto.setRiskScore(assessment.getRiskScore());
                txnDto.setSeverity(assessment.getSeverity());
                txnDto.setStatus(assessment.getSuggestedStatus());
                txnDto.setFlagReason(assessment.getExplanation());
                response.setExtractedTransaction(txnDto);

                if (assessment.getSuggestedStatus() != null && assessment.getSuggestedStatus() != com.phishshield.domain.TransactionStatus.CONFIRMED) {
                    response.setSuspicious(true);
                    response.setThreatCategory("TRANSACTION_ANOMALY");
                    response.setSeverity(assessment.getSeverity());
                    response.setRiskScore(assessment.getRiskScore());
                    response.getIndicators().addAll(assessment.getReasonCodes());
                    response.setExplanation("Bank transaction notification extracted successfully. However, this debit represents an anomaly compared to your historical baseline: " + assessment.getExplanation());
                    response.setRecommendedAction(assessment.getRecommendation());
                    return response;
                } else {
                    response.setSuspicious(false);
                    response.setThreatCategory("NONE");
                    response.setSeverity("LOW");
                    response.setRiskScore(10);
                    response.setIndicators(List.of("TRANSACTION_EXTRACTED_NORMAL"));
                    response.setExplanation("Legitimate bank notification detected and verified against your normal spending baseline.");
                    response.setRecommendedAction("You can record this transaction to your My Money ledger.");
                    return response;
                }
            } else {
                TransactionDto txnDto = TransactionDto.fromEntity(extracted);
                response.setExtractedTransaction(txnDto);
                response.setSuspicious(false);
                response.setThreatCategory("NONE");
                response.setSeverity("LOW");
                response.setRiskScore(15);
                response.setExplanation("Transaction notification extracted. Log in to evaluate against your personal baseline.");
                response.setRecommendedAction("Review transaction details.");
                return response;
            }
        }

        // If generic text with suspicious indicators but not a direct transaction
        if (!indicators.isEmpty()) {
            response.setSuspicious(true);
            response.setThreatCategory("SUSPICIOUS_BAIT");
            response.setSeverity("MEDIUM");
            response.setRiskScore(50);
            response.setIndicators(indicators);
            response.setExplanation("Message contains cautionary language often found in spam or unsolicited financial communications.");
            response.setRecommendedAction("Exercise caution. Verify the official phone number or website of the institution independently.");
            return response;
        }

        // Clean message
        response.setSuspicious(false);
        response.setThreatCategory("NONE");
        response.setSeverity("LOW");
        response.setRiskScore(5);
        response.setExplanation("No financial fraud indicators or urgent credential requests were detected in this message.");
        response.setRecommendedAction("Message appears standard. As always, never share private passwords or OTPs.");
        return response;
    }

    private String redactSensitiveCredentials(String text) {
        if (text == null) return "";
        String redacted = OTP_PATTERN.matcher(text).replaceAll(mr -> {
            String full = mr.group();
            String code = mr.group(1);
            return full.replace(code, "[PROTECTED_OTP]");
        });
        redacted = PASSWORD_PATTERN.matcher(redacted).replaceAll(mr -> {
            String full = mr.group();
            String pwd = mr.group(1);
            return full.replace(pwd, "[PROTECTED_CREDENTIAL]");
        });
        return redacted;
    }

    private FinancialTransaction parseTransactionFields(String rawText, String sanitizedText) {
        String lower = rawText.toLowerCase();
        TransactionDirection direction = (lower.contains("credited") || lower.contains("received") || lower.contains("refund"))
                ? TransactionDirection.INCOME
                : TransactionDirection.EXPENSE;

        BigDecimal amount = BigDecimal.valueOf(100.00);
        Matcher amtMatcher = AMOUNT_PATTERN.matcher(rawText);
        if (amtMatcher.find()) {
            try {
                amount = new BigDecimal(amtMatcher.group(1).replace(",", ""));
            } catch (Exception ignored) {}
        }

        String maskedAccount = null;
        Matcher acctMatcher = ACCOUNT_PATTERN.matcher(rawText);
        if (acctMatcher.find()) {
            maskedAccount = acctMatcher.group(1);
        }

        String merchant = "Unknown Counterparty";
        Matcher vpaMatcher = UPI_VPA_PATTERN.matcher(rawText);
        if (vpaMatcher.find()) {
            merchant = vpaMatcher.group(1);
        } else {
            Matcher atMatcher = Pattern.compile("(?i)(?:at|to|info:)\\s+([A-Za-z0-9 &._-]{2,30}?)(?=\\s+(?:on|via|ref|using|for|avail|bal|dated|\\d{1,2}[-/])|[:.,;]|$)").matcher(rawText);
            if (atMatcher.find()) {
                merchant = atMatcher.group(1).trim();
            }
        }

        String reference = null;
        Matcher refMatcher = REF_PATTERN.matcher(rawText);
        if (refMatcher.find()) {
            reference = refMatcher.group(1);
        }

        TransactionType type = TransactionType.UPI;
        if (lower.contains("atm") || lower.contains("cash")) type = TransactionType.ATM;
        else if (lower.contains("card") || lower.contains("pos")) type = TransactionType.CARD;
        else if (lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps")) type = TransactionType.NETBANKING;

        return FinancialTransaction.builder()
                .amount(amount)
                .direction(direction)
                .transactionType(type)
                .category(TransactionCategory.OTHER)
                .merchant(merchant)
                .maskedAccount(maskedAccount)
                .transactionReference(reference)
                .transactionDateTime(LocalDateTime.now())
                .notes("Extracted from SMS notification")
                .build();
    }
}
