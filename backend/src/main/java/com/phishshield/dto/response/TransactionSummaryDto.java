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

    // Personalized Baseline Metrics
    private boolean personalizationEstablished;
    private BigDecimal baselineMedian;
    private BigDecimal baselineMad;
    private long historyCount;
    private int recurringExpensesCount;
    private String baselineStatusMessage;

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

    public boolean isPersonalizationEstablished() { return personalizationEstablished; }
    public void setPersonalizationEstablished(boolean personalizationEstablished) { this.personalizationEstablished = personalizationEstablished; }
    public BigDecimal getBaselineMedian() { return baselineMedian; }
    public void setBaselineMedian(BigDecimal baselineMedian) { this.baselineMedian = baselineMedian; }
    public BigDecimal getBaselineMad() { return baselineMad; }
    public void setBaselineMad(BigDecimal baselineMad) { this.baselineMad = baselineMad; }
    public long getHistoryCount() { return historyCount; }
    public void setHistoryCount(long historyCount) { this.historyCount = historyCount; }
    public int getRecurringExpensesCount() { return recurringExpensesCount; }
    public void setRecurringExpensesCount(int recurringExpensesCount) { this.recurringExpensesCount = recurringExpensesCount; }
    public String getBaselineStatusMessage() { return baselineStatusMessage; }
    public void setBaselineStatusMessage(String baselineStatusMessage) { this.baselineStatusMessage = baselineStatusMessage; }
}
