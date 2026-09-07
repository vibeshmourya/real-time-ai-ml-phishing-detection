package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_alerts")
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 30, nullable = false)
    private AlertType alertType = AlertType.ADVISORY;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "target_audience", length = 50, nullable = false)
    private String targetAudience = "ALL";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public SecurityAlert() {}

    public SecurityAlert(Long id, String title, AlertType alertType, AlertSeverity severity,
                         String message, String targetAudience, Boolean isActive,
                         LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.title = title;
        this.alertType = alertType != null ? alertType : AlertType.ADVISORY;
        this.severity = severity != null ? severity : AlertSeverity.WARNING;
        this.message = message;
        this.targetAudience = targetAudience != null ? targetAudience : "ALL";
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private AlertType alertType = AlertType.ADVISORY;
        private AlertSeverity severity = AlertSeverity.WARNING;
        private String message;
        private String targetAudience = "ALL";
        private Boolean isActive = true;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder alertType(AlertType alertType) { this.alertType = alertType; return this; }
        public Builder severity(AlertSeverity severity) { this.severity = severity; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder targetAudience(String targetAudience) { this.targetAudience = targetAudience; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }

        public SecurityAlert build() {
            return new SecurityAlert(id, title, alertType, severity, message, targetAudience, isActive, createdAt, expiresAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
