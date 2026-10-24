-- =============================================================================
-- Prahar 2.0 Database Migration Script
-- Adds: Financial Transactions (Phase 3) and Fraud Incidents (Phase 4)
-- =============================================================================

USE phishshield_db;

-- 1. Financial Transactions Table (Personal Finance & Monitoring)
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

-- 2. Fraud Incidents Table (Fraud & Safety Centre)
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

-- 3. Persistent Chat History (Phase 17)
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

