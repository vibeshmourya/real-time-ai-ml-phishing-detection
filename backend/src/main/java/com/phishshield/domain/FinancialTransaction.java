package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "financial_transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_fingerprint", columnNames = {"user_id", "duplicate_fingerprint"})
    },
    indexes = {
        @Index(name = "idx_transactions_user", columnList = "user_id"),
        @Index(name = "idx_transactions_date", columnList = "transaction_date_time"),
        @Index(name = "idx_transactions_category", columnList = "category"),
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_direction", columnList = "direction")
    }
)
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 20, nullable = false)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30, nullable = false)
    private TransactionType transactionType = TransactionType.UPI;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40, nullable = false)
    private TransactionCategory category = TransactionCategory.OTHER;

    @Column(name = "source_institution", length = 100)
    private String sourceInstitution;

    @Column(name = "masked_account", length = 50)
    private String maskedAccount;

    @Column(name = "merchant", length = 150)
    private String merchant;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_source", length = 30, nullable = false)
    private TransactionImportSource importSource = TransactionImportSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private TransactionStatus status = TransactionStatus.CONFIRMED;

    @Column(name = "flag_reason", length = 255)
    private String flagReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "duplicate_fingerprint", length = 64, nullable = false)
    private String duplicateFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FinancialTransaction() {}

    public FinancialTransaction(Long id, User user, LocalDateTime transactionDateTime, BigDecimal amount,
                                String currency, TransactionDirection direction, TransactionType transactionType,
                                TransactionCategory category, String sourceInstitution, String maskedAccount,
                                String merchant, String transactionReference, TransactionImportSource importSource,
                                TransactionStatus status, String flagReason, String notes,
                                String duplicateFingerprint, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.transactionDateTime = transactionDateTime;
        this.amount = amount;
        this.currency = currency != null ? currency : "INR";
        this.direction = direction;
        this.transactionType = transactionType != null ? transactionType : TransactionType.UPI;
        this.category = category != null ? category : TransactionCategory.OTHER;
        this.sourceInstitution = sourceInstitution;
        this.maskedAccount = maskedAccount;
        this.merchant = merchant;
        this.transactionReference = transactionReference;
        this.importSource = importSource != null ? importSource : TransactionImportSource.MANUAL;
        this.status = status != null ? status : TransactionStatus.CONFIRMED;
        this.flagReason = flagReason;
        this.notes = notes;
        this.duplicateFingerprint = duplicateFingerprint;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private LocalDateTime transactionDateTime;
        private BigDecimal amount;
        private String currency = "INR";
        private TransactionDirection direction;
        private TransactionType transactionType = TransactionType.UPI;
        private TransactionCategory category = TransactionCategory.OTHER;
        private String sourceInstitution;
        private String maskedAccount;
        private String merchant;
        private String transactionReference;
        private TransactionImportSource importSource = TransactionImportSource.MANUAL;
        private TransactionStatus status = TransactionStatus.CONFIRMED;
        private String flagReason;
        private String notes;
        private String duplicateFingerprint;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder transactionDateTime(LocalDateTime dt) { this.transactionDateTime = dt; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder direction(TransactionDirection direction) { this.direction = direction; return this; }
        public Builder transactionType(TransactionType type) { this.transactionType = type; return this; }
        public Builder category(TransactionCategory category) { this.category = category; return this; }
        public Builder sourceInstitution(String inst) { this.sourceInstitution = inst; return this; }
        public Builder maskedAccount(String acct) { this.maskedAccount = acct; return this; }
        public Builder merchant(String merchant) { this.merchant = merchant; return this; }
        public Builder transactionReference(String ref) { this.transactionReference = ref; return this; }
        public Builder importSource(TransactionImportSource source) { this.importSource = source; return this; }
        public Builder status(TransactionStatus status) { this.status = status; return this; }
        public Builder flagReason(String flagReason) { this.flagReason = flagReason; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder duplicateFingerprint(String fingerprint) { this.duplicateFingerprint = fingerprint; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public FinancialTransaction build() {
            return new FinancialTransaction(id, user, transactionDateTime, amount, currency, direction,
                    transactionType, category, sourceInstitution, maskedAccount, merchant, transactionReference,
                    importSource, status, flagReason, notes, duplicateFingerprint, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getTransactionDateTime() { return transactionDateTime; }
    public void setTransactionDateTime(LocalDateTime transactionDateTime) { this.transactionDateTime = transactionDateTime; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionDirection getDirection() { return direction; }
    public void setDirection(TransactionDirection direction) { this.direction = direction; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
    public String getSourceInstitution() { return sourceInstitution; }
    public void setSourceInstitution(String sourceInstitution) { this.sourceInstitution = sourceInstitution; }
    public String getMaskedAccount() { return maskedAccount; }
    public void setMaskedAccount(String maskedAccount) { this.maskedAccount = maskedAccount; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public TransactionImportSource getImportSource() { return importSource; }
    public void setImportSource(TransactionImportSource importSource) { this.importSource = importSource; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getDuplicateFingerprint() { return duplicateFingerprint; }
    public void setDuplicateFingerprint(String duplicateFingerprint) { this.duplicateFingerprint = duplicateFingerprint; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
