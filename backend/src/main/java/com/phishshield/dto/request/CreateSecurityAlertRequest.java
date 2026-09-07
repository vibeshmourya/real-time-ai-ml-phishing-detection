package com.phishshield.dto.request;

import com.phishshield.domain.AlertSeverity;
import com.phishshield.domain.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateSecurityAlertRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotNull(message = "Alert type is required")
    private AlertType alertType;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotBlank(message = "Message is required")
    private String message;

    private String targetAudience = "ALL";
    private LocalDateTime expiresAt;

    public CreateSecurityAlertRequest() {}

    public CreateSecurityAlertRequest(String title, AlertType alertType, AlertSeverity severity,
                                      String message, String targetAudience, LocalDateTime expiresAt) {
        this.title = title;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.targetAudience = targetAudience != null ? targetAudience : "ALL";
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private AlertType alertType;
        private AlertSeverity severity;
        private String message;
        private String targetAudience = "ALL";
        private LocalDateTime expiresAt;

        public Builder title(String title) { this.title = title; return this; }
        public Builder alertType(AlertType alertType) { this.alertType = alertType; return this; }
        public Builder severity(AlertSeverity severity) { this.severity = severity; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder targetAudience(String targetAudience) { this.targetAudience = targetAudience; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }

        public CreateSecurityAlertRequest build() {
            return new CreateSecurityAlertRequest(title, alertType, severity, message, targetAudience, expiresAt);
        }
    }

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
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
