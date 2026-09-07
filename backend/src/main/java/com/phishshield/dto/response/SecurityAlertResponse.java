package com.phishshield.dto.response;

import com.phishshield.domain.AlertSeverity;
import com.phishshield.domain.AlertType;

import java.time.LocalDateTime;

public class SecurityAlertResponse {

    private Long id;
    private String title;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private String targetAudience;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public SecurityAlertResponse() {}

    public SecurityAlertResponse(Long id, String title, AlertType alertType, AlertSeverity severity,
                                 String message, String targetAudience, Boolean isActive,
                                 LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.title = title;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.targetAudience = targetAudience;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private AlertType alertType;
        private AlertSeverity severity;
        private String message;
        private String targetAudience;
        private Boolean isActive;
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

        public SecurityAlertResponse build() {
            return new SecurityAlertResponse(id, title, alertType, severity, message, targetAudience, isActive, createdAt, expiresAt);
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
