package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_scans")
public class URLScan {

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

    @Column(name = "domain", length = 255, nullable = false)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", length = 30, nullable = false)
    private ScanType scanType = ScanType.URL;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 30, nullable = false)
    private RiskLevel riskLevel = RiskLevel.SAFE;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_category", length = 60, nullable = false)
    private ThreatCategory threatCategory = ThreatCategory.BENIGN;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "COMPLETED";

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @OneToOne(mappedBy = "scan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ThreatDetection detection;

    @OneToOne(mappedBy = "scan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ModelPrediction prediction;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public URLScan() {}

    public URLScan(Long id, User user, String url, String normalizedUrl, String domain,
                   ScanType scanType, Integer riskScore, RiskLevel riskLevel, ThreatCategory threatCategory,
                   String status, String clientIp, String userAgent, ThreatDetection detection,
                   ModelPrediction prediction, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.domain = domain;
        this.scanType = scanType != null ? scanType : ScanType.URL;
        this.riskScore = riskScore != null ? riskScore : 0;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.SAFE;
        this.threatCategory = threatCategory != null ? threatCategory : ThreatCategory.BENIGN;
        this.status = status != null ? status : "COMPLETED";
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.detection = detection;
        this.prediction = prediction;
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
        private String domain;
        private ScanType scanType = ScanType.URL;
        private Integer riskScore = 0;
        private RiskLevel riskLevel = RiskLevel.SAFE;
        private ThreatCategory threatCategory = ThreatCategory.BENIGN;
        private String status = "COMPLETED";
        private String clientIp;
        private String userAgent;
        private ThreatDetection detection;
        private ModelPrediction prediction;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder normalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder scanType(ScanType scanType) { this.scanType = scanType; return this; }
        public Builder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public Builder riskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder detection(ThreatDetection detection) { this.detection = detection; return this; }
        public Builder prediction(ModelPrediction prediction) { this.prediction = prediction; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public URLScan build() {
            return new URLScan(id, user, url, normalizedUrl, domain, scanType, riskScore, riskLevel,
                    threatCategory, status, clientIp, userAgent, detection, prediction, createdAt, updatedAt);
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
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public ScanType getScanType() { return scanType; }
    public void setScanType(ScanType scanType) { this.scanType = scanType; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public ThreatDetection getDetection() { return detection; }
    public void setDetection(ThreatDetection detection) { this.detection = detection; }
    public ModelPrediction getPrediction() { return prediction; }
    public void setPrediction(ModelPrediction prediction) { this.prediction = prediction; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
