package com.phishshield.dto.response;

import com.phishshield.domain.ReportStatus;
import com.phishshield.domain.ThreatCategory;

import java.time.LocalDateTime;

public class UserReportResponse {

    private Long id;
    private String url;
    private String normalizedUrl;
    private ThreatCategory threatCategory;
    private String description;
    private String reporterEmail;
    private ReportStatus status;
    private String adminNotes;
    private Long scanId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserReportResponse() {}

    public UserReportResponse(Long id, String url, String normalizedUrl, ThreatCategory threatCategory,
                              String description, String reporterEmail, ReportStatus status,
                              String adminNotes, Long scanId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.threatCategory = threatCategory;
        this.description = description;
        this.reporterEmail = reporterEmail;
        this.status = status;
        this.adminNotes = adminNotes;
        this.scanId = scanId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String url;
        private String normalizedUrl;
        private ThreatCategory threatCategory;
        private String description;
        private String reporterEmail;
        private ReportStatus status;
        private String adminNotes;
        private Long scanId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder normalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder reporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; return this; }
        public Builder status(ReportStatus status) { this.status = status; return this; }
        public Builder adminNotes(String adminNotes) { this.adminNotes = adminNotes; return this; }
        public Builder scanId(Long scanId) { this.scanId = scanId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserReportResponse build() {
            return new UserReportResponse(id, url, normalizedUrl, threatCategory, description, reporterEmail, status, adminNotes, scanId, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getNormalizedUrl() { return normalizedUrl; }
    public void setNormalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
