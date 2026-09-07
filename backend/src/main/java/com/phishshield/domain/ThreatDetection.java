package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "threat_detections")
public class ThreatDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private URLScan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_category", length = 60, nullable = false)
    private ThreatCategory threatCategory;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 0.0;

    @Column(name = "explanation_summary", columnDefinition = "TEXT")
    private String explanationSummary;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    @OneToMany(mappedBy = "detection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ThreatIndicator> indicators = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ThreatDetection() {}

    public ThreatDetection(Long id, URLScan scan, ThreatCategory threatCategory, Double confidenceScore,
                           String explanationSummary, String recommendedActions,
                           List<ThreatIndicator> indicators, LocalDateTime createdAt) {
        this.id = id;
        this.scan = scan;
        this.threatCategory = threatCategory;
        this.confidenceScore = confidenceScore != null ? confidenceScore : 0.0;
        this.explanationSummary = explanationSummary;
        this.recommendedActions = recommendedActions;
        this.indicators = indicators != null ? indicators : new ArrayList<>();
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private URLScan scan;
        private ThreatCategory threatCategory;
        private Double confidenceScore = 0.0;
        private String explanationSummary;
        private String recommendedActions;
        private List<ThreatIndicator> indicators = new ArrayList<>();
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder scan(URLScan scan) { this.scan = scan; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder confidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder explanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; return this; }
        public Builder recommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; return this; }
        public Builder indicators(List<ThreatIndicator> indicators) { this.indicators = indicators; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ThreatDetection build() {
            return new ThreatDetection(id, scan, threatCategory, confidenceScore, explanationSummary, recommendedActions, indicators, createdAt);
        }
    }

    public void addIndicator(ThreatIndicator indicator) {
        indicators.add(indicator);
        indicator.setDetection(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public URLScan getScan() { return scan; }
    public void setScan(URLScan scan) { this.scan = scan; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getExplanationSummary() { return explanationSummary; }
    public void setExplanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; }
    public String getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; }
    public List<ThreatIndicator> getIndicators() { return indicators; }
    public void setIndicators(List<ThreatIndicator> indicators) { this.indicators = indicators; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
