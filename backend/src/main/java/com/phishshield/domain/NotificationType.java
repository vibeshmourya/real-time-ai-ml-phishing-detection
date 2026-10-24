package com.phishshield.domain;

/**
 * Categorizes the origin of a security alert or notification.
 */
public enum NotificationType {
    SCAN_ALERT,
    QR_ALERT,
    SCREENSHOT_ALERT,
    INCIDENT_ALERT,
    SYSTEM_ADVISORY
}
