package com.phishshield.dto.response;

import com.phishshield.domain.Notification;
import com.phishshield.domain.NotificationSeverity;
import com.phishshield.domain.NotificationType;

import java.time.LocalDateTime;

/**
 * Clean response DTO representing an authenticated user's notification.
 */
public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private NotificationSeverity severity;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Long relatedScanId;
    private Long relatedIncidentId;
    private LocalDateTime createdAt;

    public NotificationDto() {}

    public NotificationDto(Long id, String title, String message, NotificationType notificationType,
                           NotificationSeverity severity, Boolean isRead, LocalDateTime readAt,
                           Long relatedScanId, Long relatedIncidentId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.severity = severity;
        this.isRead = isRead;
        this.readAt = readAt;
        this.relatedScanId = relatedScanId;
        this.relatedIncidentId = relatedIncidentId;
        this.createdAt = createdAt;
    }

    public static NotificationDto fromEntity(Notification entity) {
        if (entity == null) return null;
        return new NotificationDto(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getNotificationType(),
                entity.getSeverity(),
                entity.getIsRead(),
                entity.getReadAt(),
                entity.getRelatedScanId(),
                entity.getRelatedIncidentId(),
                entity.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
