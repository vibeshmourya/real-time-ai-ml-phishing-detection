-- =============================================================================
-- PhishShield Relational Database Schema (MySQL 8.0+)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS phishshield_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE phishshield_db;

-- 1. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- 3. URL Scans Table
CREATE TABLE IF NOT EXISTS url_scans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    url TEXT NOT NULL,
    normalized_url VARCHAR(1024) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    scan_type ENUM('URL', 'EMAIL', 'MESSAGE', 'SCREENSHOT', 'QR') NOT NULL DEFAULT 'URL',
    risk_score INT NOT NULL DEFAULT 0,
    risk_level ENUM('SAFE', 'SUSPICIOUS', 'HIGH_RISK') NOT NULL DEFAULT 'SAFE',
    threat_category VARCHAR(60) NOT NULL DEFAULT 'BENIGN',
    status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'COMPLETED',
    client_ip VARCHAR(64),
    user_agent VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scans_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_url_scans_domain ON url_scans(domain);
CREATE INDEX idx_url_scans_risk_level ON url_scans(risk_level);
CREATE INDEX idx_url_scans_threat_cat ON url_scans(threat_category);
CREATE INDEX idx_url_scans_created_at ON url_scans(created_at);
CREATE INDEX idx_url_scans_user_id ON url_scans(user_id);

-- 4. Threat Detections Table
CREATE TABLE IF NOT EXISTS threat_detections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    threat_category VARCHAR(60) NOT NULL,
    confidence_score DOUBLE NOT NULL DEFAULT 0.0,
    explanation_summary TEXT,
    recommended_actions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_detections_scan FOREIGN KEY (scan_id) REFERENCES url_scans (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_detections_scan_id ON threat_detections(scan_id);

-- 5. Threat Indicators Table
CREATE TABLE IF NOT EXISTS threat_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    detection_id BIGINT NOT NULL,
    indicator_type ENUM('LEXICAL', 'DOMAIN', 'SECURITY', 'CONTENT', 'REPUTATION') NOT NULL,
    indicator_name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    rule_triggered VARCHAR(100),
    weight DOUBLE NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_indicators_detection FOREIGN KEY (detection_id) REFERENCES threat_detections (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_indicators_detection_id ON threat_indicators(detection_id);

-- 6. Model Predictions Table
CREATE TABLE IF NOT EXISTS model_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    phishing_probability DOUBLE NOT NULL DEFAULT 0.0,
    lexical_score DOUBLE NOT NULL DEFAULT 0.0,
    domain_score DOUBLE NOT NULL DEFAULT 0.0,
    rule_score DOUBLE NOT NULL DEFAULT 0.0,
    explanation_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_predictions_scan FOREIGN KEY (scan_id) REFERENCES url_scans (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_predictions_scan_id ON model_predictions(scan_id);

-- 7. User Reports Table
CREATE TABLE IF NOT EXISTS user_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    url TEXT NOT NULL,
    normalized_url VARCHAR(1024) NOT NULL,
    threat_category VARCHAR(60) NOT NULL DEFAULT 'UNKNOWN',
    description TEXT NOT NULL,
    reporter_email VARCHAR(100),
    scan_id BIGINT NULL,
    status ENUM('PENDING', 'VERIFIED', 'REJECTED', 'RESOLVED') NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_scan FOREIGN KEY (scan_id) REFERENCES url_scans (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reports_status ON user_reports(status);
CREATE INDEX idx_reports_created_at ON user_reports(created_at);

-- 8. Prevention Content Table
CREATE TABLE IF NOT EXISTS prevention_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    threat_category VARCHAR(60) NOT NULL,
    title VARCHAR(150) NOT NULL,
    short_explanation TEXT NOT NULL,
    attack_mechanism TEXT NOT NULL,
    why_it_matters TEXT NOT NULL,
    immediate_actions TEXT NOT NULL,
    checklist_json TEXT,
    video_url VARCHAR(500),
    audio_url VARCHAR(500),
    language_code VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_cat_lang UNIQUE (threat_category, language_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prevention_cat_lang ON prevention_content(threat_category, language_code);

-- 9. Security Alerts Table
CREATE TABLE IF NOT EXISTS security_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    alert_type ENUM('EMERGING_THREAT', 'ADVISORY', 'SYSTEM_NOTICE') NOT NULL DEFAULT 'ADVISORY',
    severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL DEFAULT 'WARNING',
    message TEXT NOT NULL,
    target_audience VARCHAR(50) NOT NULL DEFAULT 'ALL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alerts_active ON security_alerts(is_active);

-- 10. Feedback Table
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    feedback_type ENUM('ACCURATE', 'FALSE_POSITIVE', 'FALSE_NEGATIVE') NOT NULL,
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_scan FOREIGN KEY (scan_id) REFERENCES url_scans (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_action ON audit_logs(action);

-- 12. Financial Transactions Table (Phase 3: Personal Finance & Monitoring)
CREATE TABLE IF NOT EXISTS financial_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_date_time DATETIME NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    direction ENUM('INCOME', 'EXPENSE', 'TRANSFER') NOT NULL,
    transaction_type ENUM('UPI', 'CARD', 'NETBANKING', 'ATM', 'CASH', 'OTHER') NOT NULL DEFAULT 'UPI',
    category ENUM('SALARY', 'SHOPPING', 'FOOD', 'BILLS', 'TRAVEL', 'CASH_WITHDRAWAL', 'TRANSFERS', 'REFUNDS', 'INVESTMENTS', 'OTHER') NOT NULL DEFAULT 'OTHER',
    source_institution VARCHAR(100),
    masked_account VARCHAR(50),
    merchant VARCHAR(150),
    transaction_reference VARCHAR(100),
    import_source ENUM('MANUAL', 'CSV', 'NOTIFICATION_TEXT', 'STATEMENT') NOT NULL DEFAULT 'MANUAL',
    status ENUM('CONFIRMED', 'NEEDS_REVIEW', 'FLAGGED_SUSPICIOUS') NOT NULL DEFAULT 'CONFIRMED',
    flag_reason VARCHAR(255),
    notes TEXT,
    duplicate_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_fingerprint UNIQUE (user_id, duplicate_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_transactions_user ON financial_transactions(user_id);
CREATE INDEX idx_transactions_date ON financial_transactions(transaction_date_time);
CREATE INDEX idx_transactions_category ON financial_transactions(category);
CREATE INDEX idx_transactions_status ON financial_transactions(status);
CREATE INDEX idx_transactions_direction ON financial_transactions(direction);

-- 13. Fraud Incidents Table (Phase 4: Fraud & Safety Centre)
CREATE TABLE IF NOT EXISTS fraud_incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    incident_type ENUM('UNAUTHORIZED_DEBIT', 'SUSPICIOUS_CREDIT', 'UPI_FRAUD', 'ATM_CARD_FRAUD', 'OTP_DISCLOSURE', 'PHISHING_CLICKED', 'CREDENTIALS_LEAKED', 'PHONE_THEFT', 'SIM_SWAP', 'MALICIOUS_APP', 'IDENTITY_THEFT', 'OTHER') NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    incident_date_time DATETIME NOT NULL,
    amount_involved DECIMAL(12, 2),
    currency VARCHAR(10) DEFAULT 'INR',
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    source_scan_id BIGINT,
    related_transaction_ids VARCHAR(255),
    bank_or_provider_contacted VARCHAR(100),
    complaint_reference VARCHAR(100),
    bank_ticket_reference VARCHAR(100),
    status ENUM('DRAFT', 'ACTION_REQUIRED', 'SUBMITTED_EXTERNALLY', 'ACKNOWLEDGED', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'ACTION_REQUIRED',
    actions_checklist_json TEXT,
    timeline_json TEXT,
    evidence_notes TEXT,
    next_follow_up_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_incidents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_incidents_scan FOREIGN KEY (source_scan_id) REFERENCES url_scans (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_incidents_user ON fraud_incidents(user_id);
CREATE INDEX idx_incidents_type ON fraud_incidents(incident_type);
CREATE INDEX idx_incidents_status ON fraud_incidents(status);
CREATE INDEX idx_incidents_severity ON fraud_incidents(severity);
CREATE INDEX idx_incidents_date ON fraud_incidents(incident_date_time);

-- 14. Persistent Chat History (Phase 17)
CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL DEFAULT 'New Conversation',
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conversations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_conversations_user ON chat_conversations(user_id);
CREATE INDEX idx_chat_conversations_updated ON chat_conversations(updated_at);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    provider VARCHAR(50),
    is_live_ai BOOLEAN DEFAULT FALSE,
    intent VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_messages_conv ON chat_messages(conversation_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at);

-- 15. User Notifications (Phase 21)
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

-- 16. Notification Preferences (Phase 21)
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



