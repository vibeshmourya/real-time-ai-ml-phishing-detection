package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reports")
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "url", columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "normalized_url", length = 1024, nullable = false)
    private String normalizedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_category", length = 60, nullable = false)
    private ThreatCategory threatCategory = ThreatCategory.UNKNOWN;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "reporter_email", length = 100)
    private String reporterEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private URLScan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserReport() {}

    public UserReport(Long id, User user, String url, String normalizedUrl, ThreatCategory threatCategory,
                      String description, String reporterEmail, URLScan scan, ReportStatus status,
                      String adminNotes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.threatCategory = threatCategory != null ? threatCategory : ThreatCategory.UNKNOWN;
        this.description = description;
        this.reporterEmail = reporterEmail;
        this.scan = scan;
        this.status = status != null ? status : ReportStatus.PENDING;
        this.adminNotes = adminNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private String url;
        private String normalizedUrl;
        private ThreatCategory threatCategory = ThreatCategory.UNKNOWN;
        private String description;
        private String reporterEmail;
        private URLScan scan;
        private ReportStatus status = ReportStatus.PENDING;
        private String adminNotes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder normalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder reporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; return this; }
        public Builder scan(URLScan scan) { this.scan = scan; return this; }
        public Builder status(ReportStatus status) { this.status = status; return this; }
        public Builder adminNotes(String adminNotes) { this.adminNotes = adminNotes; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserReport build() {
            return new UserReport(id, user, url, normalizedUrl, threatCategory, description, reporterEmail, scan, status, adminNotes, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
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
    public URLScan getScan() { return scan; }
    public void setScan(URLScan scan) { this.scan = scan; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
