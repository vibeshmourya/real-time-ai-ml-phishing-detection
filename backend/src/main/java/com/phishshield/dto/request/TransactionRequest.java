package com.phishshield.dto.request;

import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRequest {

    private LocalDateTime transactionDateTime;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency = "INR";

    @NotNull(message = "Direction (INCOME/EXPENSE/TRANSFER) is required")
    private TransactionDirection direction;

    private TransactionType transactionType = TransactionType.UPI;
    private TransactionCategory category = TransactionCategory.OTHER;
    private String sourceInstitution;
    private String maskedAccount;
    private String merchant;
    private String transactionReference;
    private String notes;

    public TransactionRequest() {}

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
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
