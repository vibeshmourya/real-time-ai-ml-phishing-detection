package com.phishshield.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelPredictionResponse {

    private String modelName;
    private String modelVersion;
    private Double phishingProbability;
    private Boolean isMlAvailable;
    private String mlFailureReason;
    private Double lexicalScore;
    private Double domainScore;
    private Double ruleScore;
    private List<String> explanations = Collections.emptyList();
    private List<Map<String, Object>> mlIndicators = Collections.emptyList();
    private Double executionTimeMs;
    private LocalDateTime timestamp;

    public ModelPredictionResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelName;
        private String modelVersion;
        private Double phishingProbability;
        private Boolean isMlAvailable = true;
        private String mlFailureReason;
        private Double lexicalScore;
        private Double domainScore;
        private Double ruleScore;
        private List<String> explanations = Collections.emptyList();
        private List<Map<String, Object>> mlIndicators = Collections.emptyList();
        private Double executionTimeMs;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder modelVersion(String modelVersion) { this.modelVersion = modelVersion; return this; }
        public Builder phishingProbability(Double phishingProbability) { this.phishingProbability = phishingProbability; return this; }
        public Builder isMlAvailable(Boolean isMlAvailable) { this.isMlAvailable = isMlAvailable; return this; }
        public Builder mlFailureReason(String mlFailureReason) { this.mlFailureReason = mlFailureReason; return this; }
        public Builder lexicalScore(Double lexicalScore) { this.lexicalScore = lexicalScore; return this; }
        public Builder domainScore(Double domainScore) { this.domainScore = domainScore; return this; }
        public Builder ruleScore(Double ruleScore) { this.ruleScore = ruleScore; return this; }
        public Builder explanations(List<String> explanations) {
            this.explanations = explanations != null ? explanations : Collections.emptyList();
            return this;
        }
        public Builder mlIndicators(List<Map<String, Object>> mlIndicators) {
            this.mlIndicators = mlIndicators != null ? mlIndicators : Collections.emptyList();
            return this;
        }
        public Builder executionTimeMs(Double executionTimeMs) { this.executionTimeMs = executionTimeMs; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ModelPredictionResponse build() {
            ModelPredictionResponse r = new ModelPredictionResponse();
            r.modelName = this.modelName;
            r.modelVersion = this.modelVersion;
            r.phishingProbability = this.phishingProbability;
            r.isMlAvailable = this.isMlAvailable;
            r.mlFailureReason = this.mlFailureReason;
            r.lexicalScore = this.lexicalScore;
            r.domainScore = this.domainScore;
            r.ruleScore = this.ruleScore;
            r.explanations = this.explanations;
            r.mlIndicators = this.mlIndicators;
            r.executionTimeMs = this.executionTimeMs;
            r.timestamp = this.timestamp;
            return r;
        }
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Double getPhishingProbability() { return phishingProbability; }
    public void setPhishingProbability(Double phishingProbability) { this.phishingProbability = phishingProbability; }

    public Boolean getIsMlAvailable() { return isMlAvailable; }
    public void setIsMlAvailable(Boolean isMlAvailable) { this.isMlAvailable = isMlAvailable; }

    public String getMlFailureReason() { return mlFailureReason; }
    public void setMlFailureReason(String mlFailureReason) { this.mlFailureReason = mlFailureReason; }

    public Double getLexicalScore() { return lexicalScore; }
    public void setLexicalScore(Double lexicalScore) { this.lexicalScore = lexicalScore; }

    public Double getDomainScore() { return domainScore; }
    public void setDomainScore(Double domainScore) { this.domainScore = domainScore; }

    public Double getRuleScore() { return ruleScore; }
    public void setRuleScore(Double ruleScore) { this.ruleScore = ruleScore; }

    public List<String> getExplanations() { return explanations; }
    public void setExplanations(List<String> explanations) { this.explanations = explanations; }

    public List<Map<String, Object>> getMlIndicators() { return mlIndicators; }
    public void setMlIndicators(List<Map<String, Object>> mlIndicators) { this.mlIndicators = mlIndicators; }

    public Double getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Double executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
