package com.phishshield.service;

import com.phishshield.domain.FinancialTransaction;
import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionStatus;
import com.phishshield.dto.response.PersonalizedRiskAssessment;
import com.phishshield.repository.FinancialTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PersonalizedRiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizedRiskAssessmentService.class);

    private final FinancialTransactionRepository transactionRepository;

    private static final Pattern HIGH_RISK_KEYWORDS = Pattern.compile(
            "(?i).*(crypto|bitcoin|binance|betting|lottery|gambling|mandate auto|unknown debit|overdraft fee|telegram bot|gift card|fake reward|unauthorized).*");

    public PersonalizedRiskAssessmentService(FinancialTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public static class BaselineProfile {
        private final boolean established;
        private final BigDecimal median;
        private final BigDecimal mad;
        private final int sampleSize;
        private final Set<String> familiarMerchants;
        private final List<RecurringPattern> recurringPatterns;

        public BaselineProfile(boolean established, BigDecimal median, BigDecimal mad, int sampleSize,
                               Set<String> familiarMerchants, List<RecurringPattern> recurringPatterns) {
            this.established = established;
            this.median = median;
            this.mad = mad;
            this.sampleSize = sampleSize;
            this.familiarMerchants = familiarMerchants;
            this.recurringPatterns = recurringPatterns;
        }

        public boolean isEstablished() { return established; }
        public BigDecimal getMedian() { return median; }
        public BigDecimal getMad() { return mad; }
        public int getSampleSize() { return sampleSize; }
        public Set<String> getFamiliarMerchants() { return familiarMerchants; }
        public List<RecurringPattern> getRecurringPatterns() { return recurringPatterns; }
    }

    public static class RecurringPattern {
        private final String merchant;
        private final TransactionCategory category;
        private final BigDecimal expectedAmount;

        public RecurringPattern(String merchant, TransactionCategory category, BigDecimal expectedAmount) {
            this.merchant = merchant != null ? merchant.trim().toLowerCase() : "";
            this.category = category;
            this.expectedAmount = expectedAmount;
        }

        public boolean matches(String targetMerchant, TransactionCategory targetCategory, BigDecimal targetAmount) {
            if (targetAmount == null || expectedAmount == null) return false;
            BigDecimal diff = targetAmount.subtract(expectedAmount).abs();
            BigDecimal tolerance = expectedAmount.multiply(new BigDecimal("0.20")); // ±20% tolerance
            if (diff.compareTo(tolerance) > 0) return false;

            if (targetCategory != null && this.category != null && this.category == targetCategory) {
                return true;
            }
            if (targetMerchant != null && !this.merchant.isEmpty() && targetMerchant.toLowerCase().contains(this.merchant)) {
                return true;
            }
            return false;
        }

        public BigDecimal getExpectedAmount() { return expectedAmount; }
        public String getMerchant() { return merchant; }
        public TransactionCategory getCategory() { return category; }
    }

    public BaselineProfile computeUserProfile(Long userId) {
        List<FinancialTransaction> userTxns = transactionRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        List<BigDecimal> expenseAmounts = userTxns.stream()
                .filter(t -> t.getDirection() == TransactionDirection.EXPENSE)
                .map(FinancialTransaction::getAmount)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        Set<String> familiarMerchants = userTxns.stream()
                .map(FinancialTransaction::getMerchant)
                .filter(m -> m != null && !m.isBlank() && !"Unknown Counterparty".equalsIgnoreCase(m))
                .map(m -> m.trim().toLowerCase())
                .collect(Collectors.toSet());

        List<RecurringPattern> recurringPatterns = detectRecurringPatterns(userTxns);

        if (expenseAmounts.size() < 5) {
            return new BaselineProfile(false, BigDecimal.ZERO, BigDecimal.ZERO, expenseAmounts.size(), familiarMerchants, recurringPatterns);
        }

        BigDecimal median = calculateMedian(expenseAmounts);
        List<BigDecimal> absDeviations = expenseAmounts.stream()
                .map(a -> a.subtract(median).abs())
                .sorted()
                .collect(Collectors.toList());
        BigDecimal mad = calculateMedian(absDeviations);

        // Guard against zero MAD (if all expenses were identical amounts)
        if (mad.compareTo(BigDecimal.ZERO) == 0) {
            mad = median.multiply(new BigDecimal("0.25")).max(new BigDecimal("50.00"));
        }

        return new BaselineProfile(true, median, mad, expenseAmounts.size(), familiarMerchants, recurringPatterns);
    }

    private List<RecurringPattern> detectRecurringPatterns(List<FinancialTransaction> txns) {
        List<RecurringPattern> patterns = new ArrayList<>();
        Map<String, List<FinancialTransaction>> merchantGroups = txns.stream()
                .filter(t -> t.getDirection() == TransactionDirection.EXPENSE && t.getMerchant() != null && !t.getMerchant().isBlank())
                .collect(Collectors.groupingBy(t -> t.getMerchant().trim().toLowerCase()));

        for (Map.Entry<String, List<FinancialTransaction>> entry : merchantGroups.entrySet()) {
            List<FinancialTransaction> group = entry.getValue();
            if (group.size() >= 2) {
                group.sort(Comparator.comparing(FinancialTransaction::getTransactionDateTime));
                for (int i = 0; i < group.size() - 1; i++) {
                    FinancialTransaction t1 = group.get(i);
                    FinancialTransaction t2 = group.get(i + 1);
                    long days = ChronoUnit.DAYS.between(t1.getTransactionDateTime(), t2.getTransactionDateTime());
                    if (days >= 20 && days <= 40) { // Monthly interval (e.g. rent, bill, subscription)
                        BigDecimal diff = t1.getAmount().subtract(t2.getAmount()).abs();
                        if (diff.compareTo(t1.getAmount().multiply(new BigDecimal("0.15"))) <= 0) {
                            patterns.add(new RecurringPattern(entry.getKey(), t1.getCategory(), t1.getAmount()));
                            break;
                        }
                    }
                }
            }
        }

        // Also check rent category specifically if user logged rent > 4000
        List<FinancialTransaction> rentTxns = txns.stream()
                .filter(t -> t.getDirection() == TransactionDirection.EXPENSE)
                .filter(t -> (t.getNotes() != null && t.getNotes().toLowerCase().contains("rent"))
                        || (t.getMerchant() != null && t.getMerchant().toLowerCase().contains("rent")))
                .collect(Collectors.toList());
        if (!rentTxns.isEmpty()) {
            BigDecimal avgRent = rentTxns.stream()
                    .map(FinancialTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(rentTxns.size()), 2, RoundingMode.HALF_UP);
            patterns.add(new RecurringPattern("rent", TransactionCategory.OTHER, avgRent));
        }

        return patterns;
    }

    public PersonalizedRiskAssessment evaluateTransaction(FinancialTransaction txn, Long userId, boolean isHistorical) {
        PersonalizedRiskAssessment assessment = new PersonalizedRiskAssessment();
        assessment.setHistorical(isHistorical);

        if (txn == null || txn.getAmount() == null) {
            assessment.setRiskScore(0);
            assessment.setSeverity("LOW");
            assessment.setSuggestedStatus(TransactionStatus.CONFIRMED);
            assessment.setExplanation("Invalid transaction input");
            assessment.setRecommendation("No action needed.");
            return assessment;
        }

        BigDecimal amount = txn.getAmount();
        String merchant = txn.getMerchant() != null ? txn.getMerchant().trim() : "";
        String notes = txn.getNotes() != null ? txn.getNotes().trim() : "";
        String combinedText = (merchant + " " + notes).trim();

        // 1. High-Risk Keyword Check
        if (HIGH_RISK_KEYWORDS.matcher(combinedText).matches()) {
            assessment.setRiskScore(85);
            assessment.setSeverity("HIGH");
            assessment.setSuggestedStatus(TransactionStatus.FLAGGED_SUSPICIOUS);
            assessment.getReasonCodes().add("SUSPICIOUS_KEYWORD_MATCH");
            assessment.setExplanation("Transaction description contains high-risk terms associated with fraudulent schemes or unauthorized mandates.");
            assessment.setRecommendation("Verify this payment immediately in your bank app. Confirm if you authorized this debit.");
            return assessment;
        }

        // Income transactions are generally safe unless labeled as fraudulent credit
        if (txn.getDirection() == TransactionDirection.INCOME) {
            assessment.setRiskScore(5);
            assessment.setSeverity("LOW");
            assessment.setSuggestedStatus(TransactionStatus.CONFIRMED);
            assessment.setExplanation("Normal incoming credit transaction.");
            assessment.setRecommendation("No action required.");
            return assessment;
        }

        // Fetch User's Personalized Baseline Profile
        BaselineProfile baseline = computeUserProfile(userId);
        assessment.setPersonalizationEstablished(baseline.isEstablished());

        // Check if transaction matches a known Recurring Expense (e.g. Monthly Rent)
        for (RecurringPattern pattern : baseline.getRecurringPatterns()) {
            if (pattern.matches(merchant, txn.getCategory(), amount)) {
                assessment.setRecurringMatch(true);
                assessment.setRiskScore(10);
                assessment.setSeverity("LOW");
                assessment.setSuggestedStatus(TransactionStatus.CONFIRMED);
                assessment.getReasonCodes().add("RECURRING_EXPENSE_MATCH");
                assessment.setExplanation("Matched your normal recurring baseline (e.g. routine monthly rent or utility bill). False anomaly suppressed.");
                assessment.setRecommendation("Legitimate recurring expense verified.");
                return assessment;
            }
        }

        // 2. Unestablished Baseline Fallback (Cold start / < 5 transactions)
        if (!baseline.isEstablished()) {
            assessment.getReasonCodes().add("BASELINE_LEARNING_IN_PROGRESS");
            if (amount.compareTo(new BigDecimal("50000.00")) > 0) {
                assessment.setRiskScore(75);
                assessment.setSeverity("HIGH");
                assessment.setSuggestedStatus(TransactionStatus.FLAGGED_SUSPICIOUS);
                assessment.getReasonCodes().add("HIGH_VALUE_THRESHOLD_EXCEEDED");
                assessment.setExplanation("Personalization is still establishing. Debit exceeds the conservative platform threshold of ₹50,000.");
                assessment.setRecommendation("Review this high-value debit to ensure it corresponds to an intended major payment.");
            } else if (amount.compareTo(new BigDecimal("15000.00")) > 0 && (merchant.isEmpty() || "Unknown Counterparty".equalsIgnoreCase(merchant))) {
                assessment.setRiskScore(50);
                assessment.setSeverity("MEDIUM");
                assessment.setSuggestedStatus(TransactionStatus.NEEDS_REVIEW);
                assessment.getReasonCodes().add("MISSING_MERCHANT_LARGE_DEBIT");
                assessment.setExplanation("Large debit (₹" + amount + ") with missing counterparty details.");
                assessment.setRecommendation("Check your bank statement to identify the recipient and confirm authorization.");
            } else {
                assessment.setRiskScore(15);
                assessment.setSeverity("LOW");
                assessment.setSuggestedStatus(TransactionStatus.CONFIRMED);
                assessment.setExplanation("Transaction is within standard entry limits while your personalized spending profile is establishing.");
                assessment.setRecommendation("Routine transaction.");
            }
            return assessment;
        }

        // 3. Established Personalization Scoring using Median & MAD
        BigDecimal median = baseline.getMedian();
        BigDecimal mad = baseline.getMad();

        // Recipient Familiarity
        boolean isFamiliar = merchant.isEmpty() || baseline.getFamiliarMerchants().contains(merchant.toLowerCase());
        assessment.setRecipientFamiliarity(isFamiliar ? "KNOWN" : "NEW");

        // Calculate Modified Z-Score: M = 0.6745 * |x - median| / MAD
        BigDecimal diff = amount.subtract(median).abs();
        double zScore = 0.6745 * diff.doubleValue() / mad.doubleValue();
        assessment.setzScore(Math.round(zScore * 100.0) / 100.0);

        // Evaluate Deviations
        if (zScore >= 6.0) {
            // Extreme statistical anomaly
            assessment.setRiskScore(80);
            assessment.setSeverity("HIGH");
            assessment.setSuggestedStatus(TransactionStatus.FLAGGED_SUSPICIOUS);
            assessment.getReasonCodes().add("ANOMALY_EXTREME_DEVIATION");
            assessment.setExplanation(String.format(
                    "Expense of ₹%s significantly exceeds your typical spending baseline (median: ₹%s, dispersion: ₹%s).",
                    amount.toPlainString(), median.toPlainString(), mad.toPlainString()
            ));
            assessment.setRecommendation("Confirm whether you authorized this unusually high payment.");
        } else if (zScore >= 3.5) {
            // Moderate statistical anomaly
            int score = !isFamiliar ? 65 : 45;
            assessment.setRiskScore(score);
            assessment.setSeverity(score >= 60 ? "HIGH" : "MEDIUM");
            assessment.setSuggestedStatus(TransactionStatus.NEEDS_REVIEW);
            assessment.getReasonCodes().add("ANOMALY_MODERATE_DEVIATION");
            if (!isFamiliar) {
                assessment.getReasonCodes().add("NEW_RECIPIENT_ABOVE_BASELINE");
                assessment.setExplanation(String.format(
                        "Payment of ₹%s to new recipient '%s' exceeds your usual routine transactions (median: ₹%s).",
                        amount.toPlainString(), merchant, median.toPlainString()
                ));
                assessment.setRecommendation("Review unfamiliar recipient details to ensure it was not a misdirected UPI transfer.");
            } else {
                assessment.setExplanation(String.format(
                        "Expense of ₹%s is noticeably higher than your routine transactions (median: ₹%s).",
                        amount.toPlainString(), median.toPlainString()
                ));
                assessment.setRecommendation("Verify this above-average expenditure.");
            }
        } else if (!isFamiliar && amount.compareTo(median.multiply(new BigDecimal("2.0"))) > 0 && amount.compareTo(new BigDecimal("3000.00")) > 0) {
            // New recipient with above-average amount
            assessment.setRiskScore(40);
            assessment.setSeverity("MEDIUM");
            assessment.setSuggestedStatus(TransactionStatus.NEEDS_REVIEW);
            assessment.getReasonCodes().add("NEW_RECIPIENT_FIRST_TIME");
            assessment.setExplanation(String.format(
                    "First-time payment to '%s' (₹%s) is above your median spend of ₹%s.",
                    merchant, amount.toPlainString(), median.toPlainString()
            ));
            assessment.setRecommendation("Verify that you intentionally sent funds to this recipient.");
        } else {
            // Normal within baseline
            assessment.setRiskScore(5);
            assessment.setSeverity("LOW");
            assessment.setSuggestedStatus(TransactionStatus.CONFIRMED);
            assessment.getReasonCodes().add("WITHIN_NORMAL_BASELINE");
            assessment.setExplanation(String.format(
                    "Expense of ₹%s aligns with your personal transaction baseline (median: ₹%s).",
                    amount.toPlainString(), median.toPlainString()
            ));
            assessment.setRecommendation("Normal transaction.");
        }

        return assessment;
    }

    private BigDecimal calculateMedian(List<BigDecimal> sortedList) {
        if (sortedList == null || sortedList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int size = sortedList.size();
        if (size % 2 == 1) {
            return sortedList.get(size / 2);
        } else {
            BigDecimal m1 = sortedList.get((size / 2) - 1);
            BigDecimal m2 = sortedList.get(size / 2);
            return m1.add(m2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
    }
}
