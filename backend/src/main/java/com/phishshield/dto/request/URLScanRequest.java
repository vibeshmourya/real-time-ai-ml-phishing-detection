package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class URLScanRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String url;

    public URLScanRequest() {}

    public URLScanRequest(String url) {
        this.url = url;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;

        public Builder url(String url) { this.url = url; return this; }

        public URLScanRequest build() {
            return new URLScanRequest(url);
        }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
