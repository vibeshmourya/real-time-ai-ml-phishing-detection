package com.phishshield.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MLPredictionResult {

    private boolean available;
    private String url;
    private String prediction;       // "SAFE", "PHISHING"
    private String riskLevel;        // "SAFE", "SUSPICIOUS", "HIGH_RISK"
    private Integer riskScore;       // 0 - 100
    private Double mlProbability;    // 0.0 - 1.0
    private Double confidence;       // 0.0 - 1.0
    private String threatCategory;
    private List<Map<String, Object>> indicators = Collections.emptyList();
    private List<String> explanations = Collections.emptyList();
    private String modelName;
    private String modelVersion;
    private Double executionTimeMs;
    private String failureReason;

    public MLPredictionResult() {}

    public static MLPredictionResult failure(String failureReason) {
        MLPredictionResult r = new MLPredictionResult();
        r.setAvailable(false);
        r.setFailureReason(failureReason);
        r.setIndicators(Collections.emptyList());
        r.setExplanations(Collections.emptyList());
        return r;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public Double getMlProbability() { return mlProbability; }
    public void setMlProbability(Double mlProbability) { this.mlProbability = mlProbability; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getThreatCategory() { return threatCategory; }
    public void setThreatCategory(String threatCategory) { this.threatCategory = threatCategory; }

    public List<Map<String, Object>> getIndicators() { return indicators; }
    public void setIndicators(List<Map<String, Object>> indicators) {
        this.indicators = indicators != null ? indicators : Collections.emptyList();
    }

    public List<String> getExplanations() { return explanations; }
    public void setExplanations(List<String> explanations) {
        this.explanations = explanations != null ? explanations : Collections.emptyList();
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Double getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Double executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
