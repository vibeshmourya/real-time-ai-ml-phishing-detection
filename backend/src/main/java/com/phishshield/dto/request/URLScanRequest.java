package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class URLScanRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String url;

    private boolean includeThreatIntelConsent = false;

    public URLScanRequest() {}

    public URLScanRequest(String url) {
        this.url = url;
    }

    public URLScanRequest(String url, boolean includeThreatIntelConsent) {
        this.url = url;
        this.includeThreatIntelConsent = includeThreatIntelConsent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private boolean includeThreatIntelConsent;

        public Builder url(String url) { this.url = url; return this; }
        public Builder includeThreatIntelConsent(boolean consent) { this.includeThreatIntelConsent = consent; return this; }

        public URLScanRequest build() {
            return new URLScanRequest(url, includeThreatIntelConsent);
        }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isIncludeThreatIntelConsent() { return includeThreatIntelConsent; }
    public void setIncludeThreatIntelConsent(boolean includeThreatIntelConsent) { this.includeThreatIntelConsent = includeThreatIntelConsent; }
}
