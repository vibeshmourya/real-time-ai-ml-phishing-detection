package com.phishshield.dto.response;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ScanType;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.threatintel.ThreatIntelReport;

import java.time.LocalDateTime;

public class URLScanResponse {

    private Long scanId;
    private String url;
    private String normalizedUrl;
    private String domain;
    private ScanType scanType;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private ThreatCategory threatCategory;
    private String status;
    private ThreatDetectionResponse detection;
    private PreventionContentResponse preventionGuide;
    private ModelPredictionResponse modelPrediction;
    private ThreatIntelReport threatIntel;
    private ScanExplainabilityDetails explainability;
    private LocalDateTime createdAt;

    public URLScanResponse() {}

    public URLScanResponse(Long scanId, String url, String normalizedUrl, String domain,
                           ScanType scanType, Integer riskScore, RiskLevel riskLevel,
                           ThreatCategory threatCategory, String status, ThreatDetectionResponse detection,
                           PreventionContentResponse preventionGuide, ModelPredictionResponse modelPrediction,
                           ThreatIntelReport threatIntel, ScanExplainabilityDetails explainability, LocalDateTime createdAt) {
        this.scanId = scanId;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.domain = domain;
        this.scanType = scanType;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.threatCategory = threatCategory;
        this.status = status;
        this.detection = detection;
        this.preventionGuide = preventionGuide;
        this.modelPrediction = modelPrediction;
        this.threatIntel = threatIntel;
        this.explainability = explainability;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long scanId;
        private String url;
        private String normalizedUrl;
        private String domain;
        private ScanType scanType;
        private Integer riskScore;
        private RiskLevel riskLevel;
        private ThreatCategory threatCategory;
        private String status;
        private ThreatDetectionResponse detection;
        private PreventionContentResponse preventionGuide;
        private ModelPredictionResponse modelPrediction;
        private ThreatIntelReport threatIntel;
        private ScanExplainabilityDetails explainability;
        private LocalDateTime createdAt;

        public Builder scanId(Long scanId) { this.scanId = scanId; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder normalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder scanType(ScanType scanType) { this.scanType = scanType; return this; }
        public Builder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public Builder riskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder detection(ThreatDetectionResponse detection) { this.detection = detection; return this; }
        public Builder preventionGuide(PreventionContentResponse preventionGuide) { this.preventionGuide = preventionGuide; return this; }
        public Builder modelPrediction(ModelPredictionResponse modelPrediction) { this.modelPrediction = modelPrediction; return this; }
        public Builder threatIntel(ThreatIntelReport threatIntel) { this.threatIntel = threatIntel; return this; }
        public Builder explainability(ScanExplainabilityDetails explainability) { this.explainability = explainability; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public URLScanResponse build() {
            URLScanResponse response = new URLScanResponse();
            response.scanId = this.scanId;
            response.url = this.url;
            response.normalizedUrl = this.normalizedUrl;
            response.domain = this.domain;
            response.scanType = this.scanType;
            response.riskScore = this.riskScore;
            response.riskLevel = this.riskLevel;
            response.threatCategory = this.threatCategory;
            response.status = this.status;
            response.detection = this.detection;
            response.preventionGuide = this.preventionGuide;
            response.modelPrediction = this.modelPrediction;
            response.threatIntel = this.threatIntel;
            response.explainability = this.explainability;
            response.createdAt = this.createdAt;
            return response;
        }
    }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getNormalizedUrl() { return normalizedUrl; }
    public void setNormalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public ScanType getScanType() { return scanType; }
    public void setScanType(ScanType scanType) { this.scanType = scanType; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ThreatDetectionResponse getDetection() { return detection; }
    public void setDetection(ThreatDetectionResponse detection) { this.detection = detection; }

    public PreventionContentResponse getPreventionGuide() { return preventionGuide; }
    public void setPreventionGuide(PreventionContentResponse preventionGuide) { this.preventionGuide = preventionGuide; }

    public ModelPredictionResponse getModelPrediction() { return modelPrediction; }
    public void setModelPrediction(ModelPredictionResponse modelPrediction) { this.modelPrediction = modelPrediction; }

    public ThreatIntelReport getThreatIntel() { return threatIntel; }
    public void setThreatIntel(ThreatIntelReport threatIntel) { this.threatIntel = threatIntel; }

    public ScanExplainabilityDetails getExplainability() { return explainability; }
    public void setExplainability(ScanExplainabilityDetails explainability) { this.explainability = explainability; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
