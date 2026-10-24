package com.phishshield.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TransactionSummaryDto {

    private BigDecimal totalInflows;
    private BigDecimal totalOutflows;
    private BigDecimal netCashFlow;
    private long flaggedCount;
    private long needsReviewCount;
    private Map<String, BigDecimal> categoryExpenses;
    private List<TransactionDto> recentTransactions;
    private String coverageNote;

    public TransactionSummaryDto() {}

    public BigDecimal getTotalInflows() { return totalInflows; }
    public void setTotalInflows(BigDecimal totalInflows) { this.totalInflows = totalInflows; }
    public BigDecimal getTotalOutflows() { return totalOutflows; }
    public void setTotalOutflows(BigDecimal totalOutflows) { this.totalOutflows = totalOutflows; }
    public BigDecimal getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(BigDecimal netCashFlow) { this.netCashFlow = netCashFlow; }
    public long getFlaggedCount() { return flaggedCount; }
    public void setFlaggedCount(long flaggedCount) { this.flaggedCount = flaggedCount; }
    public long getNeedsReviewCount() { return needsReviewCount; }
    public void setNeedsReviewCount(long needsReviewCount) { this.needsReviewCount = needsReviewCount; }
    public Map<String, BigDecimal> getCategoryExpenses() { return categoryExpenses; }
    public void setCategoryExpenses(Map<String, BigDecimal> categoryExpenses) { this.categoryExpenses = categoryExpenses; }
    public List<TransactionDto> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<TransactionDto> recentTransactions) { this.recentTransactions = recentTransactions; }
    public String getCoverageNote() { return coverageNote; }
    public void setCoverageNote(String coverageNote) { this.coverageNote = coverageNote; }
}
