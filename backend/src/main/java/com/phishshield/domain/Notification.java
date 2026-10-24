package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persistent user notification entity storing security alerts, incident updates, and threat events.
 */
@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_notifications_idempotency", columnList = "user_id, idempotency_key")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 40, nullable = false)
    private NotificationType notificationType = NotificationType.SCAN_ALERT;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private NotificationSeverity severity = NotificationSeverity.MEDIUM;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "related_scan_id")
    private Long relatedScanId;

    @Column(name = "related_incident_id")
    private Long relatedIncidentId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(Long id, User user, String title, String message, NotificationType notificationType,
                        NotificationSeverity severity, Boolean isRead, LocalDateTime readAt,
                        Long relatedScanId, Long relatedIncidentId, String idempotencyKey, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType != null ? notificationType : NotificationType.SCAN_ALERT;
        this.severity = severity != null ? severity : NotificationSeverity.MEDIUM;
        this.isRead = isRead != null ? isRead : false;
        this.readAt = readAt;
        this.relatedScanId = relatedScanId;
        this.relatedIncidentId = relatedIncidentId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private String title;
        private String message;
        private NotificationType notificationType = NotificationType.SCAN_ALERT;
        private NotificationSeverity severity = NotificationSeverity.MEDIUM;
        private Boolean isRead = false;
        private LocalDateTime readAt;
        private Long relatedScanId;
        private Long relatedIncidentId;
        private String idempotencyKey;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder notificationType(NotificationType type) { this.notificationType = type; return this; }
        public Builder severity(NotificationSeverity severity) { this.severity = severity; return this; }
        public Builder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public Builder readAt(LocalDateTime readAt) { this.readAt = readAt; return this; }
        public Builder relatedScanId(Long scanId) { this.relatedScanId = scanId; return this; }
        public Builder relatedIncidentId(Long incidentId) { this.relatedIncidentId = incidentId; return this; }
        public Builder idempotencyKey(String key) { this.idempotencyKey = key; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Notification build() {
            return new Notification(id, user, title, message, notificationType, severity,
                    isRead, readAt, relatedScanId, relatedIncidentId, idempotencyKey, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public NotificationSeverity getSeverity() { return severity; }
    public void setSeverity(NotificationSeverity severity) { this.severity = severity; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public Long getRelatedScanId() { return relatedScanId; }
    public void setRelatedScanId(Long relatedScanId) { this.relatedScanId = relatedScanId; }

    public Long getRelatedIncidentId() { return relatedIncidentId; }
    public void setRelatedIncidentId(Long relatedIncidentId) { this.relatedIncidentId = relatedIncidentId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
