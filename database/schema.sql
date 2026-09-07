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
