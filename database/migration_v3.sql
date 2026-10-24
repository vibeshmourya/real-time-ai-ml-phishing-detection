-- =============================================================================
-- Prahar Database Migration Script v3
-- Adds: Real-Time Security Alerts, Persistent Notifications, and User Preferences (Phase 21)
-- =============================================================================

USE phishshield_db;

-- 1. User Notifications Table
CREATE TABLE IF NOT EXISTS user_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    link_url VARCHAR(255),
    source_scan_id BIGINT,
    source_incident_id BIGINT,
    idempotency_key VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_scan FOREIGN KEY (source_scan_id) REFERENCES url_scans (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_incident FOREIGN KEY (source_incident_id) REFERENCES fraud_incidents (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user ON user_notifications(user_id);
CREATE INDEX idx_notifications_user_read ON user_notifications(user_id, is_read);
CREATE INDEX idx_notifications_user_created ON user_notifications(user_id, created_at);
CREATE INDEX idx_notifications_idempotency ON user_notifications(user_id, idempotency_key);

-- 2. Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    browser_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    min_severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    scan_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    incident_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    system_advisories_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pref_user ON notification_preferences(user_id);
