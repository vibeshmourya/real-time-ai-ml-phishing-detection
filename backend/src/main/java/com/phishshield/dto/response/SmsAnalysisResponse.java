package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.List;

public class SmsAnalysisResponse {

    private String redactedText;
    private boolean isSuspicious;
    private String threatCategory; // NONE, PHISHING_LINK, BANK_IMPERSONATION, CREDENTIAL_HARVESTING, TRANSACTION_ANOMALY, LOTTERY_BAIT
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private int riskScore; // 0 - 100
    private TransactionDto extractedTransaction;
    private List<String> extractedUrls = new ArrayList<>();
    private List<String> indicators = new ArrayList<>();
    private String explanation;
    private String recommendedAction;
    private String privacyNotice;
    private String androidPlatformNotice;

    public SmsAnalysisResponse() {}

    public String getRedactedText() { return redactedText; }
    public void setRedactedText(String redactedText) { this.redactedText = redactedText; }
    public boolean isSuspicious() { return isSuspicious; }
    public void setSuspicious(boolean suspicious) { isSuspicious = suspicious; }
    public String getThreatCategory() { return threatCategory; }
    public void setThreatCategory(String threatCategory) { this.threatCategory = threatCategory; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public TransactionDto getExtractedTransaction() { return extractedTransaction; }
    public void setExtractedTransaction(TransactionDto extractedTransaction) { this.extractedTransaction = extractedTransaction; }
    public List<String> getExtractedUrls() { return extractedUrls; }
    public void setExtractedUrls(List<String> extractedUrls) { this.extractedUrls = extractedUrls; }
    public List<String> getIndicators() { return indicators; }
    public void setIndicators(List<String> indicators) { this.indicators = indicators; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getPrivacyNotice() { return privacyNotice; }
    public void setPrivacyNotice(String privacyNotice) { this.privacyNotice = privacyNotice; }
    public String getAndroidPlatformNotice() { return androidPlatformNotice; }
    public void setAndroidPlatformNotice(String androidPlatformNotice) { this.androidPlatformNotice = androidPlatformNotice; }
}
