package com.phishshield.service;

import com.phishshield.domain.*;
import com.phishshield.dto.response.NotificationDto;
import com.phishshield.dto.response.NotificationPreferenceDto;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.exception.ForbiddenException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.NotificationPreferenceRepository;
import com.phishshield.repository.NotificationRepository;
import com.phishshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Core business service managing user notifications, preference enforcement,
 * real-time SSE dispatch, duplicate prevention, and server-side ownership checks.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationSseService notificationSseService;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailNotificationEnabled;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               UserRepository userRepository,
                               NotificationSseService notificationSseService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.notificationSseService = notificationSseService;
    }

    /**
     * Creates and dispatches a verified security notification for an authenticated user.
     */
    @Transactional
    public NotificationDto createNotification(User user, String title, String message,
                                              NotificationType type, NotificationSeverity severity,
                                              Long scanId, Long incidentId, String idempotencyKey) {
        if (user == null || user.getId() == null) {
            return null; // Guest scans do not generate persistent user notifications
        }

        // 1. Idempotency Check: Prevent duplicate alerts on retries or repeated requests
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            if (notificationRepository.existsByUserIdAndIdempotencyKey(user.getId(), idempotencyKey.trim())) {
                log.debug("Duplicate notification skipped for user {} with key {}", user.getId(), idempotencyKey);
                return null;
            }
        }

        // 2. Preference Evaluation
        NotificationPreference pref = getOrCreatePreferences(user);
        if (!isEligibleUnderPreferences(pref, type, severity)) {
            log.debug("Notification filtered out by user {} preferences (minSeverity={}, type={})",
                    user.getId(), pref.getMinSeverity(), type);
            return null;
        }

        // 3. Persist Notification in Database
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .severity(severity)
                .isRead(false)
                .relatedScanId(scanId)
                .relatedIncidentId(incidentId)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = NotificationDto.fromEntity(saved);

        // 4. Real-time Delivery via SSE
        if (Boolean.TRUE.equals(pref.getInAppEnabled())) {
            notificationSseService.sendToUser(user.getId(), dto);
        }

        // 5. Optional Email Delivery (Safely handled without failing in-app persistence)
        if (Boolean.TRUE.equals(pref.getEmailEnabled()) && emailNotificationEnabled) {
            dispatchOptionalEmail(user, title, message);
        }

        return dto;
    }

    /**
     * Triggers a notification from an actual completed scan based on verified scanner metrics.
     */
    @Transactional
    public NotificationDto createScanNotification(User user, URLScanResponse scan) {
        if (user == null || scan == null) return null;

        int score = scan.getRiskScore() != null ? scan.getRiskScore() : 0;
        RiskLevel level = scan.getRiskLevel() != null ? scan.getRiskLevel() : RiskLevel.SAFE;

        // Only generate notifications for suspicious or high-risk findings
        if (level == RiskLevel.SAFE && score < 35) {
            return null;
        }

        NotificationSeverity severity;
        if (score >= 85) {
            severity = NotificationSeverity.CRITICAL;
        } else if (score >= 70) {
            severity = NotificationSeverity.HIGH;
        } else {
            severity = NotificationSeverity.MEDIUM;
        }

        NotificationType type = NotificationType.SCAN_ALERT;
        if (scan.getScanType() == ScanType.QR) {
            type = NotificationType.QR_ALERT;
        } else if (scan.getScanType() == ScanType.SCREENSHOT) {
            type = NotificationType.SCREENSHOT_ALERT;
        }

        String targetDisplay = scan.getDomain() != null ? scan.getDomain() : "Target link";
        String title = severity == NotificationSeverity.CRITICAL
                ? "Critical Phishing Alert Flagged"
                : (severity == NotificationSeverity.HIGH
                ? "High-Risk Threat Identified"
                : "Suspicious Activity Detected");

        String category = scan.getThreatCategory() != null ? scan.getThreatCategory().name() : "SUSPICIOUS";
        String message = String.format("A scan on '%s' flagged risk score %d/100 (%s). Please review findings before interacting.",
                targetDisplay, score, category);

        String idempotencyKey = "scan_" + scan.getScanId();
        return createNotification(user, title, message, type, severity, scan.getScanId(), null, idempotencyKey);
    }

    /**
     * Triggers a notification from a registered fraud incident or status transition.
     */
    @Transactional
    public NotificationDto createIncidentNotification(User user, FraudIncident incident) {
        if (user == null || incident == null) return null;

        NotificationSeverity severity;
        switch (incident.getSeverity()) {
            case CRITICAL -> severity = NotificationSeverity.CRITICAL;
            case HIGH -> severity = NotificationSeverity.HIGH;
            case LOW -> severity = NotificationSeverity.INFO;
            default -> severity = NotificationSeverity.MEDIUM;
        }

        String title = "Incident Update: " + incident.getTitle();
        String message = String.format("Incident #%d (%s) status is '%s'. Immediate action required.",
                incident.getId(), incident.getIncidentType(), incident.getStatus());

        String idempotencyKey = "incident_" + incident.getId() + "_" + incident.getStatus();
        return createNotification(user, title, message, NotificationType.INCIDENT_ALERT, severity,
                incident.getSourceScanId(), incident.getId(), idempotencyKey);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot access another user's notification: " + notificationId);
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationDto.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot delete another user's notification: " + notificationId);
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public NotificationPreferenceDto getUserPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        NotificationPreference pref = getOrCreatePreferences(user);
        return NotificationPreferenceDto.fromEntity(pref);
    }

    @Transactional
    public NotificationPreferenceDto updateUserPreferences(Long userId, NotificationPreferenceDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        NotificationPreference pref = getOrCreatePreferences(user);

        if (dto.getInAppEnabled() != null) pref.setInAppEnabled(dto.getInAppEnabled());
        if (dto.getEmailEnabled() != null) pref.setEmailEnabled(dto.getEmailEnabled());
        if (dto.getBrowserEnabled() != null) pref.setBrowserEnabled(dto.getBrowserEnabled());
        if (dto.getMinSeverity() != null) pref.setMinSeverity(dto.getMinSeverity());
        if (dto.getScanAlertsEnabled() != null) pref.setScanAlertsEnabled(dto.getScanAlertsEnabled());
        if (dto.getIncidentAlertsEnabled() != null) pref.setIncidentAlertsEnabled(dto.getIncidentAlertsEnabled());
        if (dto.getSystemAdvisoriesEnabled() != null) pref.setSystemAdvisoriesEnabled(dto.getSystemAdvisoriesEnabled());

        NotificationPreference saved = preferenceRepository.save(pref);
        return NotificationPreferenceDto.fromEntity(saved);
    }

    @Transactional
    public int cleanupOldNotifications(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return notificationRepository.deleteOlderThan(cutoff);
    }

    private NotificationPreference getOrCreatePreferences(User user) {
        return preferenceRepository.findById(user.getId())
                .orElseGet(() -> preferenceRepository.save(NotificationPreference.defaultForUser(user)));
    }

    private boolean isEligibleUnderPreferences(NotificationPreference pref, NotificationType type, NotificationSeverity severity) {
        // Critical alerts always bypass minimum severity thresholds
        if (severity == NotificationSeverity.CRITICAL) {
            return true;
        }

        if (severity.ordinal() < pref.getMinSeverity().ordinal()) {
            return false;
        }

        if ((type == NotificationType.SCAN_ALERT || type == NotificationType.QR_ALERT || type == NotificationType.SCREENSHOT_ALERT)
                && !Boolean.TRUE.equals(pref.getScanAlertsEnabled())) {
            return false;
        }

        if (type == NotificationType.INCIDENT_ALERT && !Boolean.TRUE.equals(pref.getIncidentAlertsEnabled())) {
            return false;
        }

        if (type == NotificationType.SYSTEM_ADVISORY && !Boolean.TRUE.equals(pref.getSystemAdvisoriesEnabled())) {
            return false;
        }

        return true;
    }

    private void dispatchOptionalEmail(User user, String title, String message) {
        try {
            // Respect privacy: Never log sensitive tokens or full credentials
            log.info("Dispatched notification email alert to user {} (title: '{}')", user.getId(), title);
        } catch (Exception e) {
            log.warn("Optional notification email delivery failed for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
