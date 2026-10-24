package com.phishshield.dto.response;

import com.phishshield.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDto {
    private Long id;
    private LocalDateTime transactionDateTime;
    private BigDecimal amount;
    private String currency;
    private TransactionDirection direction;
    private TransactionType transactionType;
    private TransactionCategory category;
    private String sourceInstitution;
    private String maskedAccount;
    private String merchant;
    private String transactionReference;
    private TransactionImportSource importSource;
    private TransactionStatus status;
    private String flagReason;
    private String notes;
    private LocalDateTime createdAt;

    public TransactionDto() {}

    public static TransactionDto fromEntity(FinancialTransaction t) {
        TransactionDto dto = new TransactionDto();
        dto.setId(t.getId());
        dto.setTransactionDateTime(t.getTransactionDateTime());
        dto.setAmount(t.getAmount());
        dto.setCurrency(t.getCurrency());
        dto.setDirection(t.getDirection());
        dto.setTransactionType(t.getTransactionType());
        dto.setCategory(t.getCategory());
        dto.setSourceInstitution(t.getSourceInstitution());
        dto.setMaskedAccount(t.getMaskedAccount());
        dto.setMerchant(t.getMerchant());
        dto.setTransactionReference(t.getTransactionReference());
        dto.setImportSource(t.getImportSource());
        dto.setStatus(t.getStatus());
        dto.setFlagReason(t.getFlagReason());
        dto.setNotes(t.getNotes());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
