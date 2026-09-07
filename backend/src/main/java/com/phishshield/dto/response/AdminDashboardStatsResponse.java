package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardStatsResponse {

    private long totalUsers;
    private long totalScans;
    private long highRiskDetections;
    private long suspiciousDetections;
    private long safeDetections;
    private long pendingReports;
    private long totalReports;
    private long activeAlerts;
    private Map<String, Long> threatCategoryDistribution = new HashMap<>();
    private Map<String, Long> riskLevelDistribution = new HashMap<>();
    private List<URLScanResponse> recentScans = new ArrayList<>();
    private List<UserReportResponse> recentReports = new ArrayList<>();

    public AdminDashboardStatsResponse() {}

    public AdminDashboardStatsResponse(long totalUsers, long totalScans, long highRiskDetections,
                                       long suspiciousDetections, long safeDetections, long pendingReports,
                                       long totalReports, long activeAlerts,
                                       Map<String, Long> threatCategoryDistribution,
                                       Map<String, Long> riskLevelDistribution,
                                       List<URLScanResponse> recentScans,
                                       List<UserReportResponse> recentReports) {
        this.totalUsers = totalUsers;
        this.totalScans = totalScans;
        this.highRiskDetections = highRiskDetections;
        this.suspiciousDetections = suspiciousDetections;
        this.safeDetections = safeDetections;
        this.pendingReports = pendingReports;
        this.totalReports = totalReports;
        this.activeAlerts = activeAlerts;
        this.threatCategoryDistribution = threatCategoryDistribution != null ? threatCategoryDistribution : new HashMap<>();
        this.riskLevelDistribution = riskLevelDistribution != null ? riskLevelDistribution : new HashMap<>();
        this.recentScans = recentScans != null ? recentScans : new ArrayList<>();
        this.recentReports = recentReports != null ? recentReports : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalUsers;
        private long totalScans;
        private long highRiskDetections;
        private long suspiciousDetections;
        private long safeDetections;
        private long pendingReports;
        private long totalReports;
        private long activeAlerts;
        private Map<String, Long> threatCategoryDistribution = new HashMap<>();
        private Map<String, Long> riskLevelDistribution = new HashMap<>();
        private List<URLScanResponse> recentScans = new ArrayList<>();
        private List<UserReportResponse> recentReports = new ArrayList<>();

        public Builder totalUsers(long totalUsers) { this.totalUsers = totalUsers; return this; }
        public Builder totalScans(long totalScans) { this.totalScans = totalScans; return this; }
        public Builder highRiskDetections(long highRiskDetections) { this.highRiskDetections = highRiskDetections; return this; }
        public Builder suspiciousDetections(long suspiciousDetections) { this.suspiciousDetections = suspiciousDetections; return this; }
        public Builder safeDetections(long safeDetections) { this.safeDetections = safeDetections; return this; }
        public Builder pendingReports(long pendingReports) { this.pendingReports = pendingReports; return this; }
        public Builder totalReports(long totalReports) { this.totalReports = totalReports; return this; }
        public Builder activeAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; return this; }
        public Builder threatCategoryDistribution(Map<String, Long> threatCategoryDistribution) { this.threatCategoryDistribution = threatCategoryDistribution; return this; }
        public Builder riskLevelDistribution(Map<String, Long> riskLevelDistribution) { this.riskLevelDistribution = riskLevelDistribution; return this; }
        public Builder recentScans(List<URLScanResponse> recentScans) { this.recentScans = recentScans; return this; }
        public Builder recentReports(List<UserReportResponse> recentReports) { this.recentReports = recentReports; return this; }

        public AdminDashboardStatsResponse build() {
            return new AdminDashboardStatsResponse(totalUsers, totalScans, highRiskDetections,
                    suspiciousDetections, safeDetections, pendingReports, totalReports, activeAlerts,
                    threatCategoryDistribution, riskLevelDistribution, recentScans, recentReports);
        }
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalScans() { return totalScans; }
    public void setTotalScans(long totalScans) { this.totalScans = totalScans; }
    public long getHighRiskDetections() { return highRiskDetections; }
    public void setHighRiskDetections(long highRiskDetections) { this.highRiskDetections = highRiskDetections; }
    public long getSuspiciousDetections() { return suspiciousDetections; }
    public void setSuspiciousDetections(long suspiciousDetections) { this.suspiciousDetections = suspiciousDetections; }
    public long getSafeDetections() { return safeDetections; }
    public void setSafeDetections(long safeDetections) { this.safeDetections = safeDetections; }
    public long getPendingReports() { return pendingReports; }
    public void setPendingReports(long pendingReports) { this.pendingReports = pendingReports; }
    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }
    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }
    public Map<String, Long> getThreatCategoryDistribution() { return threatCategoryDistribution; }
    public void setThreatCategoryDistribution(Map<String, Long> threatCategoryDistribution) { this.threatCategoryDistribution = threatCategoryDistribution; }
    public Map<String, Long> getRiskLevelDistribution() { return riskLevelDistribution; }
    public void setRiskLevelDistribution(Map<String, Long> riskLevelDistribution) { this.riskLevelDistribution = riskLevelDistribution; }
    public List<URLScanResponse> getRecentScans() { return recentScans; }
    public void setRecentScans(List<URLScanResponse> recentScans) { this.recentScans = recentScans; }
    public List<UserReportResponse> getRecentReports() { return recentReports; }
    public void setRecentReports(List<UserReportResponse> recentReports) { this.recentReports = recentReports; }
}
