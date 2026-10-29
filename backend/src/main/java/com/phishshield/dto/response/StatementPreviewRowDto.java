package com.phishshield.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class StatementPreviewRowDto {
    private int rowNumber;
    private LocalDateTime date;
    private String dateStr;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal amount;
    private String direction;
    private String reference;
    private String account;
    private String category;
    private String transactionType;
    private boolean isDuplicate;
    private boolean isValid;
    private String validationError;

    public StatementPreviewRowDto() {}

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public boolean isDuplicate() { return isDuplicate; }
    public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }
}
