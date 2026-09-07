package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "threat_indicators")
public class ThreatIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detection_id", nullable = false)
    private ThreatDetection detection;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", length = 30, nullable = false)
    private IndicatorType indicatorType;

    @Column(name = "indicator_name", length = 100, nullable = false)
    private String indicatorName;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private IndicatorSeverity severity = IndicatorSeverity.MEDIUM;

    @Column(name = "rule_triggered", length = 100)
    private String ruleTriggered;

    @Column(name = "weight", nullable = false)
    private Double weight = 1.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ThreatIndicator() {}

    public ThreatIndicator(Long id, ThreatDetection detection, IndicatorType indicatorType,
                           String indicatorName, String description, IndicatorSeverity severity,
                           String ruleTriggered, Double weight, LocalDateTime createdAt) {
        this.id = id;
        this.detection = detection;
        this.indicatorType = indicatorType;
        this.indicatorName = indicatorName;
        this.description = description;
        this.severity = severity != null ? severity : IndicatorSeverity.MEDIUM;
        this.ruleTriggered = ruleTriggered;
        this.weight = weight != null ? weight : 1.0;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private ThreatDetection detection;
        private IndicatorType indicatorType;
        private String indicatorName;
        private String description;
        private IndicatorSeverity severity = IndicatorSeverity.MEDIUM;
        private String ruleTriggered;
        private Double weight = 1.0;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder detection(ThreatDetection detection) { this.detection = detection; return this; }
        public Builder indicatorType(IndicatorType indicatorType) { this.indicatorType = indicatorType; return this; }
        public Builder indicatorName(String indicatorName) { this.indicatorName = indicatorName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder severity(IndicatorSeverity severity) { this.severity = severity; return this; }
        public Builder ruleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; return this; }
        public Builder weight(Double weight) { this.weight = weight; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ThreatIndicator build() {
            return new ThreatIndicator(id, detection, indicatorType, indicatorName, description, severity, ruleTriggered, weight, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ThreatDetection getDetection() { return detection; }
    public void setDetection(ThreatDetection detection) { this.detection = detection; }
    public IndicatorType getIndicatorType() { return indicatorType; }
    public void setIndicatorType(IndicatorType indicatorType) { this.indicatorType = indicatorType; }
    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IndicatorSeverity getSeverity() { return severity; }
    public void setSeverity(IndicatorSeverity severity) { this.severity = severity; }
    public String getRuleTriggered() { return ruleTriggered; }
    public void setRuleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
