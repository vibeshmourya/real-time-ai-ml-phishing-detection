package com.phishshield.dto.response;

public class QRCodeItemResponse {

    private int index;
    private String rawPayload;
    private String safeDisplayPayload;
    private QRPayloadType payloadType;
    private String extractedUrl;
    private boolean scannableUrl;
    private String validationError;
    private String scanStatus;
    private URLScanResponse scanResult;

    public QRCodeItemResponse() {}

    public QRCodeItemResponse(int index, String rawPayload, String safeDisplayPayload,
                              QRPayloadType payloadType, String extractedUrl, boolean scannableUrl,
                              String validationError, String scanStatus, URLScanResponse scanResult) {
        this.index = index;
        this.rawPayload = rawPayload;
        this.safeDisplayPayload = safeDisplayPayload;
        this.payloadType = payloadType;
        this.extractedUrl = extractedUrl;
        this.scannableUrl = scannableUrl;
        this.validationError = validationError;
        this.scanStatus = scanStatus;
        this.scanResult = scanResult;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int index;
        private String rawPayload;
        private String safeDisplayPayload;
        private QRPayloadType payloadType;
        private String extractedUrl;
        private boolean scannableUrl;
        private String validationError;
        private String scanStatus;
        private URLScanResponse scanResult;

        public Builder index(int index) { this.index = index; return this; }
        public Builder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
        public Builder safeDisplayPayload(String safeDisplayPayload) { this.safeDisplayPayload = safeDisplayPayload; return this; }
        public Builder payloadType(QRPayloadType payloadType) { this.payloadType = payloadType; return this; }
        public Builder extractedUrl(String extractedUrl) { this.extractedUrl = extractedUrl; return this; }
        public Builder scannableUrl(boolean scannableUrl) { this.scannableUrl = scannableUrl; return this; }
        public Builder validationError(String validationError) { this.validationError = validationError; return this; }
        public Builder scanStatus(String scanStatus) { this.scanStatus = scanStatus; return this; }
        public Builder scanResult(URLScanResponse scanResult) { this.scanResult = scanResult; return this; }

        public QRCodeItemResponse build() {
            return new QRCodeItemResponse(index, rawPayload, safeDisplayPayload, payloadType,
                    extractedUrl, scannableUrl, validationError, scanStatus, scanResult);
        }
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public String getSafeDisplayPayload() { return safeDisplayPayload; }
    public void setSafeDisplayPayload(String safeDisplayPayload) { this.safeDisplayPayload = safeDisplayPayload; }

    public QRPayloadType getPayloadType() { return payloadType; }
    public void setPayloadType(QRPayloadType payloadType) { this.payloadType = payloadType; }

    public String getExtractedUrl() { return extractedUrl; }
    public void setExtractedUrl(String extractedUrl) { this.extractedUrl = extractedUrl; }

    public boolean isScannableUrl() { return scannableUrl; }
    public void setScannableUrl(boolean scannableUrl) { this.scannableUrl = scannableUrl; }

    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }

    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }

    public URLScanResponse getScanResult() { return scanResult; }
    public void setScanResult(URLScanResponse scanResult) { this.scanResult = scanResult; }
}
