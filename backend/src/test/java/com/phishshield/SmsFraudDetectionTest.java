package com.phishshield;

import com.phishshield.domain.FinancialTransaction;
import com.phishshield.domain.TransactionStatus;
import com.phishshield.dto.response.PersonalizedRiskAssessment;
import com.phishshield.dto.response.SmsAnalysisResponse;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.service.PersonalizedRiskAssessmentService;
import com.phishshield.service.SmsFraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SmsFraudDetectionTest {

    private FinancialTransactionRepository mockRepo;
    private PersonalizedRiskAssessmentService riskAssessmentService;
    private SmsFraudDetectionService smsService;

    @BeforeEach
    void setUp() {
        mockRepo = Mockito.mock(FinancialTransactionRepository.class);
        riskAssessmentService = new PersonalizedRiskAssessmentService(mockRepo);
        smsService = new SmsFraudDetectionService(riskAssessmentService);
    }

    @Test
    @DisplayName("SMS-01: Sensitive OTPs and passwords are automatically redacted on-device before inspection")
    void testPrivacyCredentialRedaction() {
        String rawSms = "Dear Customer, your SBI OTP is 849201 for payment of Rs 1,499.00. Never share your OTP with anyone. Your pwd is Secret@99";
        SmsAnalysisResponse response = smsService.analyzeSms(rawSms, null);

        assertThat(response.getRedactedText()).doesNotContain("849201");
        assertThat(response.getRedactedText()).contains("[PROTECTED_OTP]");
        assertThat(response.getRedactedText()).doesNotContain("Secret@99");
        assertThat(response.getRedactedText()).contains("[PROTECTED_CREDENTIAL]");
    }

    @Test
    @DisplayName("SMS-02: Urgent KYC expiry threat with unverified link is classified as high-risk bank impersonation")
    void testUrgentKycPhishingLure() {
        String kycSms = "SBI Alert: Your YONO account will be blocked within 24 hours due to pending KYC. Click http://sbi-update-kyc.xyz/auth immediately to avoid suspension.";
        SmsAnalysisResponse response = smsService.analyzeSms(kycSms, null);

        assertThat(response.isSuspicious()).isTrue();
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getRiskScore()).isGreaterThanOrEqualTo(75);
        assertThat(response.getIndicators()).anyMatch(i -> i.contains("URGENT_KYC_COERCION"));
        assertThat(response.getExtractedUrls()).contains("http://sbi-update-kyc.xyz/auth");
    }

    @Test
    @DisplayName("SMS-03: Deceptive lottery / reward bait with payment demand is flagged as malicious")
    void testLotteryBait() {
        String lotterySms = "Congratulations! You have won Rs 50,000 cash prize in lucky draw. Claim now at https://reward-claim.online/sbi after paying Rs 500 processing fee.";
        SmsAnalysisResponse response = smsService.analyzeSms(lotterySms, null);

        assertThat(response.isSuspicious()).isTrue();
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getIndicators()).anyMatch(i -> i.contains("DECEPTIVE_REWARD_BAIT"));
    }

    @Test
    @DisplayName("SMS-04: Demand to enter UPI PIN to receive money is detected as critical credential harvesting")
    void testEnterUpiPinToReceiveMoney() {
        String scamSms = "Dear user, Rs 25,000 is approved. Enter UPI PIN to receive money in your account now.";
        SmsAnalysisResponse response = smsService.analyzeSms(scamSms, null);

        assertThat(response.isSuspicious()).isTrue();
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getIndicators()).anyMatch(i -> i.contains("CRITICAL_CREDENTIAL_DEMAND"));
    }

    @Test
    @DisplayName("SMS-05: Legitimate bank debit notification is parsed and evaluated against baseline")
    void testLegitimateBankNotification() {
        Long userId = 707L;
        String bankSms = "Rs 450.00 debited from A/C XX4819 at SWIGGY on 25-Oct-26 via UPI Ref 418291039120. Avail Bal Rs 18,500.00.";

        org.springframework.data.domain.Page<FinancialTransaction> emptyPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(mockRepo.findByUserId(eq(userId), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        SmsAnalysisResponse response = smsService.analyzeSms(bankSms, userId);

        assertThat(response.isSuspicious()).isFalse();
        assertThat(response.getExtractedTransaction()).isNotNull();
        assertThat(response.getExtractedTransaction().getMerchant()).isEqualTo("SWIGGY");
    }

    @Test
    @DisplayName("SMS-06: Benign personal text produces clean verdict with 0 false alarm")
    void testBenignText() {
        String normalMsg = "Hey Rahul, let's meet tomorrow evening at the library for project discussion.";
        SmsAnalysisResponse response = smsService.analyzeSms(normalMsg, null);

        assertThat(response.isSuspicious()).isFalse();
        assertThat(response.getRiskScore()).isLessThanOrEqualTo(10);
    }
}
