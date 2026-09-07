package com.phishshield.dto.request;

import com.phishshield.domain.ThreatCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserReportRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String url;

    private ThreatCategory threatCategory;

    @NotBlank(message = "Description cannot be empty")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private String reporterEmail;
    private Long scanId;

    public UserReportRequest() {}

    public UserReportRequest(String url, ThreatCategory threatCategory, String description, String reporterEmail, Long scanId) {
        this.url = url;
        this.threatCategory = threatCategory;
        this.description = description;
        this.reporterEmail = reporterEmail;
        this.scanId = scanId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private ThreatCategory threatCategory;
        private String description;
        private String reporterEmail;
        private Long scanId;

        public Builder url(String url) { this.url = url; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder reporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; return this; }
        public Builder scanId(Long scanId) { this.scanId = scanId; return this; }

        public UserReportRequest build() {
            return new UserReportRequest(url, threatCategory, description, reporterEmail, scanId);
        }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
}
