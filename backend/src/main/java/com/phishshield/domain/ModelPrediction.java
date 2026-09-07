package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_predictions")
public class ModelPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private URLScan scan;

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    @Column(name = "model_version", length = 50, nullable = false)
    private String modelVersion;

    @Column(name = "phishing_probability", nullable = false)
    private Double phishingProbability = 0.0;

    @Column(name = "lexical_score", nullable = false)
    private Double lexicalScore = 0.0;

    @Column(name = "domain_score", nullable = false)
    private Double domainScore = 0.0;

    @Column(name = "rule_score", nullable = false)
    private Double ruleScore = 0.0;

    @Column(name = "explanation_json", columnDefinition = "TEXT")
    private String explanationJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ModelPrediction() {}

    public ModelPrediction(Long id, URLScan scan, String modelName, String modelVersion,
                           Double phishingProbability, Double lexicalScore, Double domainScore,
                           Double ruleScore, String explanationJson, LocalDateTime createdAt) {
        this.id = id;
        this.scan = scan;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.phishingProbability = phishingProbability != null ? phishingProbability : 0.0;
        this.lexicalScore = lexicalScore != null ? lexicalScore : 0.0;
        this.domainScore = domainScore != null ? domainScore : 0.0;
        this.ruleScore = ruleScore != null ? ruleScore : 0.0;
        this.explanationJson = explanationJson;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private URLScan scan;
        private String modelName;
        private String modelVersion;
        private Double phishingProbability = 0.0;
        private Double lexicalScore = 0.0;
        private Double domainScore = 0.0;
        private Double ruleScore = 0.0;
        private String explanationJson;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder scan(URLScan scan) { this.scan = scan; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder modelVersion(String modelVersion) { this.modelVersion = modelVersion; return this; }
        public Builder phishingProbability(Double phishingProbability) { this.phishingProbability = phishingProbability; return this; }
        public Builder lexicalScore(Double lexicalScore) { this.lexicalScore = lexicalScore; return this; }
        public Builder domainScore(Double domainScore) { this.domainScore = domainScore; return this; }
        public Builder ruleScore(Double ruleScore) { this.ruleScore = ruleScore; return this; }
        public Builder explanationJson(String explanationJson) { this.explanationJson = explanationJson; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ModelPrediction build() {
            return new ModelPrediction(id, scan, modelName, modelVersion, phishingProbability, lexicalScore, domainScore, ruleScore, explanationJson, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public URLScan getScan() { return scan; }
    public void setScan(URLScan scan) { this.scan = scan; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Double getPhishingProbability() { return phishingProbability; }
    public void setPhishingProbability(Double phishingProbability) { this.phishingProbability = phishingProbability; }
    public Double getLexicalScore() { return lexicalScore; }
    public void setLexicalScore(Double lexicalScore) { this.lexicalScore = lexicalScore; }
    public Double getDomainScore() { return domainScore; }
    public void setDomainScore(Double domainScore) { this.domainScore = domainScore; }
    public Double getRuleScore() { return ruleScore; }
    public void setRuleScore(Double ruleScore) { this.ruleScore = ruleScore; }
    public String getExplanationJson() { return explanationJson; }
    public void setExplanationJson(String explanationJson) { this.explanationJson = explanationJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
