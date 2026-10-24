package com.phishshield.dto.response;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ScanType;
import com.phishshield.domain.ThreatCategory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified multi-vector image analysis response consolidating:
 * 1. Image security validation metadata (format, dimensions, bounds).
 * 2. Optical QR code decoding, payload classifications, and destination verification.
 * 3. Optical Character Recognition (OCR) text extraction, engine diagnostics, and explainable heuristic indicators.
 * 4. Extracted URL candidates with source attribution (QR, OCR, or BOTH) and SSRF enforcement.
 * 5. Real Prahar URL scanner results with transparent, un-fabricated risk scores and threat categories.
 * 6. Isolated component failure reports and explicit operational limitations.
 */
public class UnifiedImageAnalysisResponse {

    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED
    private String message;
    private LocalDateTime analyzedAt = LocalDateTime.now();

    private ImageValidationMetadata imageValidation;
    private QRCodeAnalysisResponse qrAnalysis;
    private OCRTextAnalysisResult ocrAnalysis;
    private List<UnifiedUrlCandidateResponse> urlCandidates = new ArrayList<>();
    private List<URLScanResponse> successfulScans = new ArrayList<>();
    private URLScanResponse primaryScan;

    private int overallRiskScore;
    private RiskLevel overallRiskLevel;
    private ThreatCategory threatCategory;

    private Map<String, String> componentErrors = new HashMap<>();
    private List<String> limitations = new ArrayList<>();
    private List<String> recommendedNextSteps = new ArrayList<>();

    public UnifiedImageAnalysisResponse() {}

    public UnifiedImageAnalysisResponse(String status, String message, LocalDateTime analyzedAt,
                                        ImageValidationMetadata imageValidation, QRCodeAnalysisResponse qrAnalysis,
                                        OCRTextAnalysisResult ocrAnalysis, List<UnifiedUrlCandidateResponse> urlCandidates,
                                        List<URLScanResponse> successfulScans, URLScanResponse primaryScan,
                                        int overallRiskScore, RiskLevel overallRiskLevel, ThreatCategory threatCategory,
                                        Map<String, String> componentErrors, List<String> limitations,
                                        List<String> recommendedNextSteps) {
        this.status = status;
        this.message = message;
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
        this.imageValidation = imageValidation;
        this.qrAnalysis = qrAnalysis;
        this.ocrAnalysis = ocrAnalysis;
        this.urlCandidates = urlCandidates != null ? urlCandidates : new ArrayList<>();
        this.successfulScans = successfulScans != null ? successfulScans : new ArrayList<>();
        this.primaryScan = primaryScan;
        this.overallRiskScore = overallRiskScore;
        this.overallRiskLevel = overallRiskLevel;
        this.threatCategory = threatCategory;
        this.componentErrors = componentErrors != null ? componentErrors : new HashMap<>();
        this.limitations = limitations != null ? limitations : new ArrayList<>();
        this.recommendedNextSteps = recommendedNextSteps != null ? recommendedNextSteps : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status = "SUCCESS";
        private String message;
        private LocalDateTime analyzedAt = LocalDateTime.now();
        private ImageValidationMetadata imageValidation;
        private QRCodeAnalysisResponse qrAnalysis;
        private OCRTextAnalysisResult ocrAnalysis;
        private List<UnifiedUrlCandidateResponse> urlCandidates = new ArrayList<>();
        private List<URLScanResponse> successfulScans = new ArrayList<>();
        private URLScanResponse primaryScan;
        private int overallRiskScore;
        private RiskLevel overallRiskLevel = RiskLevel.SAFE;
        private ThreatCategory threatCategory = ThreatCategory.BENIGN;
        private Map<String, String> componentErrors = new HashMap<>();
        private List<String> limitations = new ArrayList<>();
        private List<String> recommendedNextSteps = new ArrayList<>();

        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder analyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; return this; }
        public Builder imageValidation(ImageValidationMetadata imageValidation) { this.imageValidation = imageValidation; return this; }
        public Builder qrAnalysis(QRCodeAnalysisResponse qrAnalysis) { this.qrAnalysis = qrAnalysis; return this; }
        public Builder ocrAnalysis(OCRTextAnalysisResult ocrAnalysis) { this.ocrAnalysis = ocrAnalysis; return this; }
        public Builder urlCandidates(List<UnifiedUrlCandidateResponse> urlCandidates) {
            this.urlCandidates = urlCandidates != null ? urlCandidates : new ArrayList<>();
            return this;
        }
        public Builder successfulScans(List<URLScanResponse> successfulScans) {
            this.successfulScans = successfulScans != null ? successfulScans : new ArrayList<>();
            return this;
        }
        public Builder primaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; return this; }
        public Builder overallRiskScore(int overallRiskScore) { this.overallRiskScore = overallRiskScore; return this; }
        public Builder overallRiskLevel(RiskLevel overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder componentErrors(Map<String, String> componentErrors) {
            this.componentErrors = componentErrors != null ? componentErrors : new HashMap<>();
            return this;
        }
        public Builder limitations(List<String> limitations) {
            this.limitations = limitations != null ? limitations : new ArrayList<>();
            return this;
        }
        public Builder recommendedNextSteps(List<String> steps) {
            this.recommendedNextSteps = steps != null ? steps : new ArrayList<>();
            return this;
        }

        public UnifiedImageAnalysisResponse build() {
            return new UnifiedImageAnalysisResponse(status, message, analyzedAt, imageValidation,
                    qrAnalysis, ocrAnalysis, urlCandidates, successfulScans, primaryScan,
                    overallRiskScore, overallRiskLevel, threatCategory, componentErrors,
                    limitations, recommendedNextSteps);
        }
    }

    // --- Standard Getters & Setters ---

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public ImageValidationMetadata getImageValidation() { return imageValidation; }
    public void setImageValidation(ImageValidationMetadata imageValidation) { this.imageValidation = imageValidation; }

    public QRCodeAnalysisResponse getQrAnalysis() { return qrAnalysis; }
    public void setQrAnalysis(QRCodeAnalysisResponse qrAnalysis) { this.qrAnalysis = qrAnalysis; }

    public OCRTextAnalysisResult getOcrAnalysis() { return ocrAnalysis; }
    public void setOcrAnalysis(OCRTextAnalysisResult ocrAnalysis) { this.ocrAnalysis = ocrAnalysis; }

    public List<UnifiedUrlCandidateResponse> getUrlCandidates() { return urlCandidates; }
    public void setUrlCandidates(List<UnifiedUrlCandidateResponse> urlCandidates) { this.urlCandidates = urlCandidates; }

    public List<URLScanResponse> getSuccessfulScans() { return successfulScans; }
    public void setSuccessfulScans(List<URLScanResponse> successfulScans) { this.successfulScans = successfulScans; }

    public URLScanResponse getPrimaryScan() { return primaryScan; }
    public void setPrimaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; }

    public int getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(int overallRiskScore) { this.overallRiskScore = overallRiskScore; }

    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(RiskLevel overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; }

    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }

    public Map<String, String> getComponentErrors() { return componentErrors; }
    public void setComponentErrors(Map<String, String> componentErrors) { this.componentErrors = componentErrors; }

    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }

    public List<String> getRecommendedNextSteps() { return recommendedNextSteps; }
    public void setRecommendedNextSteps(List<String> recommendedNextSteps) { this.recommendedNextSteps = recommendedNextSteps; }

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
