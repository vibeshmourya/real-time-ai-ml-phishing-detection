package com.phishshield.dto.response;

import com.phishshield.domain.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

public class PersonalizedRiskAssessment {

    private int riskScore; // 0 - 100
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private TransactionStatus suggestedStatus; // CONFIRMED, NEEDS_REVIEW, FLAGGED_SUSPICIOUS
    private List<String> reasonCodes = new ArrayList<>();
    private String explanation;
    private String recommendation;
    private boolean personalizationEstablished;
    private boolean isHistorical;
    private boolean isRecurringMatch;
    private double zScore;
    private String recipientFamiliarity; // KNOWN, NEW, UNKNOWN

    public PersonalizedRiskAssessment() {}

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public TransactionStatus getSuggestedStatus() { return suggestedStatus; }
    public void setSuggestedStatus(TransactionStatus suggestedStatus) { this.suggestedStatus = suggestedStatus; }
    public List<String> getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public boolean isPersonalizationEstablished() { return personalizationEstablished; }
    public void setPersonalizationEstablished(boolean personalizationEstablished) { this.personalizationEstablished = personalizationEstablished; }
    public boolean isHistorical() { return isHistorical; }
    public void setHistorical(boolean historical) { isHistorical = historical; }
    public boolean isRecurringMatch() { return isRecurringMatch; }
    public void setRecurringMatch(boolean recurringMatch) { isRecurringMatch = recurringMatch; }
    public double getzScore() { return zScore; }
    public void setzScore(double zScore) { this.zScore = zScore; }
    public String getRecipientFamiliarity() { return recipientFamiliarity; }
    public void setRecipientFamiliarity(String recipientFamiliarity) { this.recipientFamiliarity = recipientFamiliarity; }
}
