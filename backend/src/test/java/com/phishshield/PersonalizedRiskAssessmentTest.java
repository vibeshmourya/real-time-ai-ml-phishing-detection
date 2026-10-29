package com.phishshield;

import com.phishshield.domain.*;
import com.phishshield.dto.response.PersonalizedRiskAssessment;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.service.PersonalizedRiskAssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PersonalizedRiskAssessmentTest {

    private FinancialTransactionRepository transactionRepository;
    private PersonalizedRiskAssessmentService riskService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(FinancialTransactionRepository.class);
        riskService = new PersonalizedRiskAssessmentService(transactionRepository);
    }

    @Test
    @DisplayName("RISK-01: Cold-start user with < 5 transactions uses conservative rules and does NOT claim personalization")
    void testColdStartBehavior() {
        Long userId = 101L;
        // User has only 2 past transactions
        List<FinancialTransaction> history = List.of(
                createTxn(userId, new BigDecimal("350.00"), "Swiggy", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(3)),
                createTxn(userId, new BigDecimal("800.00"), "Amazon", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(1))
        );
        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(history));

        // Evaluate normal expense
        FinancialTransaction normalTxn = createTxn(userId, new BigDecimal("500.00"), "Cafe Coffee", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment normalRes = riskService.evaluateTransaction(normalTxn, userId, false);

        assertThat(normalRes.isPersonalizationEstablished()).isFalse();
        assertThat(normalRes.getSuggestedStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(normalRes.getReasonCodes()).contains("BASELINE_LEARNING_IN_PROGRESS");

        // Evaluate large unverified debit (> ₹50,000) during cold start
        FinancialTransaction hugeTxn = createTxn(userId, new BigDecimal("65000.00"), "Jewellers", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment hugeRes = riskService.evaluateTransaction(hugeTxn, userId, false);

        assertThat(hugeRes.isPersonalizationEstablished()).isFalse();
        assertThat(hugeRes.getSuggestedStatus()).isEqualTo(TransactionStatus.FLAGGED_SUSPICIOUS);
        assertThat(hugeRes.getReasonCodes()).contains("HIGH_VALUE_THRESHOLD_EXCEEDED");
    }

    @Test
    @DisplayName("RISK-02: Established user profile calculates Median & MAD correctly")
    void testEstablishedBaselineCalculation() {
        Long userId = 202L;
        // Historical daily routine expenses: 200, 300, 400, 450, 500, 600, 700 (Median = 450)
        List<FinancialTransaction> history = new ArrayList<>();
        double[] amounts = {200, 300, 400, 450, 500, 600, 700};
        for (int i = 0; i < amounts.length; i++) {
            history.add(createTxn(userId, BigDecimal.valueOf(amounts[i]), "Known Merchant " + i, TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(i + 1)));
        }

        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(history));

        PersonalizedRiskAssessmentService.BaselineProfile profile = riskService.computeUserProfile(userId);

        assertThat(profile.isEstablished()).isTrue();
        assertThat(profile.getMedian()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(profile.getMad()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("RISK-03: Legitimate monthly rent payment (₹15,000) is recognized as normal recurring baseline, suppressing false anomaly")
    void testRecurringRentExpenseNotFlagged() {
        Long userId = 303L;
        // Student / citizen spending history: ~₹300-600 routine meals, plus recurring ₹15,000 monthly rent
        List<FinancialTransaction> history = new ArrayList<>();
        double[] routine = {250, 320, 410, 290, 500, 350, 480};
        for (int i = 0; i < routine.length; i++) {
            history.add(createTxn(userId, BigDecimal.valueOf(routine[i]), "Food Stalls", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(i + 2)));
        }
        // Past month rent payment
        history.add(createTxn(userId, new BigDecimal("15000.00"), "Landlord House Rent", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(31)));

        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(history));

        // Current month rent payment
        FinancialTransaction currentRent = createTxn(userId, new BigDecimal("15000.00"), "Landlord House Rent", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment assessment = riskService.evaluateTransaction(currentRent, userId, false);

        assertThat(assessment.isRecurringMatch()).isTrue();
        assertThat(assessment.getSuggestedStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(assessment.getReasonCodes()).contains("RECURRING_EXPENSE_MATCH");
        assertThat(assessment.getRiskScore()).isLessThanOrEqualTo(15);
    }

    @Test
    @DisplayName("RISK-04: Unusually large transaction (> 6x MAD) is flagged as FLAGGED_SUSPICIOUS with explainability")
    void testUnusuallyLargeTransactionFlagged() {
        Long userId = 404L;
        // Historical expenses centered around ₹400
        List<FinancialTransaction> history = new ArrayList<>();
        double[] amounts = {300, 350, 400, 450, 500, 420, 380};
        for (int i = 0; i < amounts.length; i++) {
            history.add(createTxn(userId, BigDecimal.valueOf(amounts[i]), "Regular Store", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(i + 1)));
        }

        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(history));

        // Sudden debit of ₹25,000 to an unknown luxury outlet
        FinancialTransaction suddenBigDebit = createTxn(userId, new BigDecimal("25000.00"), "Luxury Electronic Outlet", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment assessment = riskService.evaluateTransaction(suddenBigDebit, userId, false);

        assertThat(assessment.getSuggestedStatus()).isEqualTo(TransactionStatus.FLAGGED_SUSPICIOUS);
        assertThat(assessment.getSeverity()).isEqualTo("HIGH");
        assertThat(assessment.getReasonCodes()).contains("ANOMALY_EXTREME_DEVIATION");
        assertThat(assessment.getExplanation()).contains("significantly exceeds your typical spending baseline");
    }

    @Test
    @DisplayName("RISK-05: First-time recipient with above-average amount triggers NEEDS_REVIEW")
    void testFirstTimeRecipientReview() {
        Long userId = 505L;
        List<FinancialTransaction> history = new ArrayList<>();
        double[] amounts = {500, 600, 700, 800, 900, 650, 750};
        for (int i = 0; i < amounts.length; i++) {
            history.add(createTxn(userId, BigDecimal.valueOf(amounts[i]), "Usual Grocery", TransactionDirection.EXPENSE, LocalDateTime.now().minusDays(i + 1)));
        }

        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(history));

        // First-time recipient with ₹1,400 payment (2x median of ₹700, moderate deviation)
        FinancialTransaction newRecipientTxn = createTxn(userId, new BigDecimal("1400.00"), "Unfamiliar Merchant XYZ", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment assessment = riskService.evaluateTransaction(newRecipientTxn, userId, false);

        assertThat(assessment.getRecipientFamiliarity()).isEqualTo("NEW");
        assertThat(assessment.getSuggestedStatus()).isEqualTo(TransactionStatus.NEEDS_REVIEW);
        assertThat(assessment.getReasonCodes()).contains("NEW_RECIPIENT_ABOVE_BASELINE");
    }

    @Test
    @DisplayName("RISK-06: High-risk keywords trigger immediate FLAGGED_SUSPICIOUS verdict")
    void testHighRiskKeywords() {
        Long userId = 606L;
        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FinancialTransaction cryptoTxn = createTxn(userId, new BigDecimal("1200.00"), "Binance Crypto Deposit", TransactionDirection.EXPENSE, LocalDateTime.now());
        PersonalizedRiskAssessment assessment = riskService.evaluateTransaction(cryptoTxn, userId, false);

        assertThat(assessment.getSuggestedStatus()).isEqualTo(TransactionStatus.FLAGGED_SUSPICIOUS);
        assertThat(assessment.getSeverity()).isEqualTo("HIGH");
        assertThat(assessment.getReasonCodes()).contains("SUSPICIOUS_KEYWORD_MATCH");
    }

    private FinancialTransaction createTxn(Long userId, BigDecimal amount, String merchant, TransactionDirection dir, LocalDateTime dt) {
        return FinancialTransaction.builder()
                .amount(amount)
                .merchant(merchant)
                .direction(dir)
                .transactionDateTime(dt)
                .category(merchant.toLowerCase().contains("rent") ? TransactionCategory.OTHER : TransactionCategory.SHOPPING)
                .status(TransactionStatus.CONFIRMED)
                .build();
    }
}
