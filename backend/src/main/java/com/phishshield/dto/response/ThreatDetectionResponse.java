package com.phishshield.dto.response;

import com.phishshield.domain.ThreatCategory;

import java.util.ArrayList;
import java.util.List;

public class ThreatDetectionResponse {

    private Long id;
    private ThreatCategory threatCategory;
    private Double confidenceScore;
    private String explanationSummary;
    private String recommendedActions;
    private List<ThreatIndicatorResponse> indicators = new ArrayList<>();

    public ThreatDetectionResponse() {}

    public ThreatDetectionResponse(Long id, ThreatCategory threatCategory, Double confidenceScore,
                                   String explanationSummary, String recommendedActions,
                                   List<ThreatIndicatorResponse> indicators) {
        this.id = id;
        this.threatCategory = threatCategory;
        this.confidenceScore = confidenceScore;
        this.explanationSummary = explanationSummary;
        this.recommendedActions = recommendedActions;
        this.indicators = indicators != null ? indicators : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private ThreatCategory threatCategory;
        private Double confidenceScore;
        private String explanationSummary;
        private String recommendedActions;
        private List<ThreatIndicatorResponse> indicators = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder confidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder explanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; return this; }
        public Builder recommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; return this; }
        public Builder indicators(List<ThreatIndicatorResponse> indicators) { this.indicators = indicators; return this; }

        public ThreatDetectionResponse build() {
            return new ThreatDetectionResponse(id, threatCategory, confidenceScore, explanationSummary, recommendedActions, indicators);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getExplanationSummary() { return explanationSummary; }
    public void setExplanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; }
    public String getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; }
    public List<ThreatIndicatorResponse> getIndicators() { return indicators; }
    public void setIndicators(List<ThreatIndicatorResponse> indicators) { this.indicators = indicators; }
}
