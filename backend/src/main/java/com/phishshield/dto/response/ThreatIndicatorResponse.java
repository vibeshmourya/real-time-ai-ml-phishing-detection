package com.phishshield.dto.response;

import com.phishshield.domain.IndicatorSeverity;
import com.phishshield.domain.IndicatorType;

public class ThreatIndicatorResponse {

    private Long id;
    private IndicatorType indicatorType;
    private String indicatorName;
    private String description;
    private IndicatorSeverity severity;
    private String ruleTriggered;
    private Double weight;

    public ThreatIndicatorResponse() {}

    public ThreatIndicatorResponse(Long id, IndicatorType indicatorType, String indicatorName,
                                   String description, IndicatorSeverity severity, String ruleTriggered, Double weight) {
        this.id = id;
        this.indicatorType = indicatorType;
        this.indicatorName = indicatorName;
        this.description = description;
        this.severity = severity;
        this.ruleTriggered = ruleTriggered;
        this.weight = weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private IndicatorType indicatorType;
        private String indicatorName;
        private String description;
        private IndicatorSeverity severity;
        private String ruleTriggered;
        private Double weight;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder indicatorType(IndicatorType indicatorType) { this.indicatorType = indicatorType; return this; }
        public Builder indicatorName(String indicatorName) { this.indicatorName = indicatorName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder severity(IndicatorSeverity severity) { this.severity = severity; return this; }
        public Builder ruleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; return this; }
        public Builder weight(Double weight) { this.weight = weight; return this; }

        public ThreatIndicatorResponse build() {
            return new ThreatIndicatorResponse(id, indicatorType, indicatorName, description, severity, ruleTriggered, weight);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
