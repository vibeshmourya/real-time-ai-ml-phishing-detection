package com.phishshield.dto.response;

/**
 * Represents a single URL candidate discovered from optical QR payloads or OCR text,
 * recording provenance, security scheme eligibility, scan status, and scanner results.
 */
public class UnifiedUrlCandidateResponse {

    private String url;
    private CandidateUrlSource source;
    private boolean scannable;
    private String scanStatus;
    private String validationError;
    private URLScanResponse scanResult;

    public UnifiedUrlCandidateResponse() {}

    public UnifiedUrlCandidateResponse(String url, CandidateUrlSource source, boolean scannable,
                                       String scanStatus, String validationError, URLScanResponse scanResult) {
        this.url = url;
        this.source = source;
        this.scannable = scannable;
        this.scanStatus = scanStatus;
        this.validationError = validationError;
        this.scanResult = scanResult;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private CandidateUrlSource source;
        private boolean scannable = true;
        private String scanStatus = "PENDING";
        private String validationError;
        private URLScanResponse scanResult;

        public Builder url(String url) { this.url = url; return this; }
        public Builder source(CandidateUrlSource source) { this.source = source; return this; }
        public Builder scannable(boolean scannable) { this.scannable = scannable; return this; }
        public Builder scanStatus(String scanStatus) { this.scanStatus = scanStatus; return this; }
        public Builder validationError(String validationError) { this.validationError = validationError; return this; }
        public Builder scanResult(URLScanResponse scanResult) { this.scanResult = scanResult; return this; }

        public UnifiedUrlCandidateResponse build() {
            return new UnifiedUrlCandidateResponse(url, source, scannable, scanStatus, validationError, scanResult);
        }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public CandidateUrlSource getSource() { return source; }
    public void setSource(CandidateUrlSource source) { this.source = source; }

    public boolean isScannable() { return scannable; }
    public void setScannable(boolean scannable) { this.scannable = scannable; }

    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }

    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }

    public URLScanResponse getScanResult() { return scanResult; }
    public void setScanResult(URLScanResponse scanResult) { this.scanResult = scanResult; }
}
