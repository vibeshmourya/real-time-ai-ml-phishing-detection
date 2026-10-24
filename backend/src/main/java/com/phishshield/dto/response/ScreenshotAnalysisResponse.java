package com.phishshield.dto.response;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ScanType;
import com.phishshield.domain.ThreatCategory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotAnalysisResponse {

    private QRCodeAnalysisResponse qrAnalysis;
    private OCRTextAnalysisResult ocrAnalysis;
    private List<URLScanResponse> extractedUrlScans = new ArrayList<>();
    private URLScanResponse primaryScan;
    private int overallRiskScore;
    private RiskLevel overallRiskLevel;
    private ThreatCategory threatCategory;
    private List<String> limitations = new ArrayList<>();
    private String status;
    private String message;
    private LocalDateTime analyzedAt = LocalDateTime.now();

    public ScreenshotAnalysisResponse() {}

    public ScreenshotAnalysisResponse(QRCodeAnalysisResponse qrAnalysis, OCRTextAnalysisResult ocrAnalysis,
                                      List<URLScanResponse> extractedUrlScans, URLScanResponse primaryScan,
                                      int overallRiskScore, RiskLevel overallRiskLevel, ThreatCategory threatCategory,
                                      List<String> limitations, String status, String message) {
        this.qrAnalysis = qrAnalysis;
        this.ocrAnalysis = ocrAnalysis;
        this.extractedUrlScans = extractedUrlScans != null ? extractedUrlScans : new ArrayList<>();
        this.primaryScan = primaryScan;
        this.overallRiskScore = overallRiskScore;
        this.overallRiskLevel = overallRiskLevel;
        this.threatCategory = threatCategory;
        this.limitations = limitations != null ? limitations : new ArrayList<>();
        this.status = status;
        this.message = message;
        this.analyzedAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QRCodeAnalysisResponse qrAnalysis;
        private OCRTextAnalysisResult ocrAnalysis;
        private List<URLScanResponse> extractedUrlScans = new ArrayList<>();
        private URLScanResponse primaryScan;
        private int overallRiskScore;
        private RiskLevel overallRiskLevel;
        private ThreatCategory threatCategory;
        private List<String> limitations = new ArrayList<>();
        private String status;
        private String message;

        public Builder qrAnalysis(QRCodeAnalysisResponse qrAnalysis) { this.qrAnalysis = qrAnalysis; return this; }
        public Builder ocrAnalysis(OCRTextAnalysisResult ocrAnalysis) { this.ocrAnalysis = ocrAnalysis; return this; }
        public Builder extractedUrlScans(List<URLScanResponse> scans) {
            this.extractedUrlScans = scans != null ? scans : new ArrayList<>();
            return this;
        }
        public Builder primaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; return this; }
        public Builder overallRiskScore(int score) { this.overallRiskScore = score; return this; }
        public Builder overallRiskLevel(RiskLevel level) { this.overallRiskLevel = level; return this; }
        public Builder threatCategory(ThreatCategory category) { this.threatCategory = category; return this; }
        public Builder limitations(List<String> limitations) {
            this.limitations = limitations != null ? limitations : new ArrayList<>();
            return this;
        }
        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public ScreenshotAnalysisResponse build() {
            return new ScreenshotAnalysisResponse(qrAnalysis, ocrAnalysis, extractedUrlScans, primaryScan,
                    overallRiskScore, overallRiskLevel, threatCategory, limitations, status, message);
        }
    }

    public QRCodeAnalysisResponse getQrAnalysis() { return qrAnalysis; }
    public void setQrAnalysis(QRCodeAnalysisResponse qrAnalysis) { this.qrAnalysis = qrAnalysis; }

    public OCRTextAnalysisResult getOcrAnalysis() { return ocrAnalysis; }
    public void setOcrAnalysis(OCRTextAnalysisResult ocrAnalysis) { this.ocrAnalysis = ocrAnalysis; }

    public List<URLScanResponse> getExtractedUrlScans() { return extractedUrlScans; }
    public void setExtractedUrlScans(List<URLScanResponse> extractedUrlScans) { this.extractedUrlScans = extractedUrlScans; }

    public URLScanResponse getPrimaryScan() { return primaryScan; }
    public void setPrimaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; }

    public int getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(int overallRiskScore) { this.overallRiskScore = overallRiskScore; }

    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(RiskLevel overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; }

    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }

    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    // --- Dual-Compatibility Delegation Getters for existing UI & Scan History ---
    public Long getScanId() {
        return primaryScan != null ? primaryScan.getScanId() : null;
    }

    public ScanType getScanType() {
        return primaryScan != null && primaryScan.getScanType() != null ? primaryScan.getScanType() : ScanType.SCREENSHOT;
    }

    public String getUrl() {
        return primaryScan != null ? primaryScan.getUrl() : null;
    }

    public String getNormalizedUrl() {
        return primaryScan != null ? primaryScan.getNormalizedUrl() : null;
    }

    public Integer getRiskScore() {
        return primaryScan != null ? primaryScan.getRiskScore() : overallRiskScore;
    }

    public RiskLevel getRiskLevel() {
        return primaryScan != null ? primaryScan.getRiskLevel() : overallRiskLevel;
    }

    public ThreatDetectionResponse getDetection() {
        return primaryScan != null ? primaryScan.getDetection() : null;
    }

    public PreventionContentResponse getPreventionGuide() {
        return primaryScan != null ? primaryScan.getPreventionGuide() : null;
    }

    public ModelPredictionResponse getModelPrediction() {
        return primaryScan != null ? primaryScan.getModelPrediction() : null;
    }
}
