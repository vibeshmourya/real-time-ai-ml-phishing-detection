package com.phishshield.dto.response;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QRCodeAnalysisResponse {

    private int totalQrCodesFound;
    private List<QRCodeItemResponse> qrCodes = new ArrayList<>();
    private URLScanResponse primaryScan;
    private String status;
    private String message;
    private LocalDateTime analyzedAt = LocalDateTime.now();

    public QRCodeAnalysisResponse() {}

    public QRCodeAnalysisResponse(int totalQrCodesFound, List<QRCodeItemResponse> qrCodes,
                                  URLScanResponse primaryScan, String status, String message) {
        this.totalQrCodesFound = totalQrCodesFound;
        this.qrCodes = qrCodes != null ? qrCodes : new ArrayList<>();
        this.primaryScan = primaryScan;
        this.status = status;
        this.message = message;
        this.analyzedAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalQrCodesFound;
        private List<QRCodeItemResponse> qrCodes = new ArrayList<>();
        private URLScanResponse primaryScan;
        private String status;
        private String message;

        public Builder totalQrCodesFound(int total) { this.totalQrCodesFound = total; return this; }
        public Builder qrCodes(List<QRCodeItemResponse> qrCodes) { this.qrCodes = qrCodes; return this; }
        public Builder primaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public QRCodeAnalysisResponse build() {
            return new QRCodeAnalysisResponse(totalQrCodesFound, qrCodes, primaryScan, status, message);
        }
    }

    public int getTotalQrCodesFound() { return totalQrCodesFound; }
    public void setTotalQrCodesFound(int totalQrCodesFound) { this.totalQrCodesFound = totalQrCodesFound; }

    public List<QRCodeItemResponse> getQrCodes() { return qrCodes; }
    public void setQrCodes(List<QRCodeItemResponse> qrCodes) { this.qrCodes = qrCodes; }

    public URLScanResponse getPrimaryScan() { return primaryScan; }
    public void setPrimaryScan(URLScanResponse primaryScan) { this.primaryScan = primaryScan; }

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

    public String getUrl() {
        return primaryScan != null ? primaryScan.getUrl() : null;
    }

    public Integer getRiskScore() {
        return primaryScan != null ? primaryScan.getRiskScore() : 0;
    }

    public RiskLevel getRiskLevel() {
        return primaryScan != null ? primaryScan.getRiskLevel() : RiskLevel.SAFE;
    }

    public ThreatCategory getThreatCategory() {
        return primaryScan != null ? primaryScan.getThreatCategory() : ThreatCategory.BENIGN;
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
