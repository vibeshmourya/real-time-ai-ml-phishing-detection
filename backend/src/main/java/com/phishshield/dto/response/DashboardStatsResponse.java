package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsResponse {

    private long totalScans;
    private long safeScans;
    private long suspiciousScans;
    private long highRiskScans;
    private long reportsSubmitted;
    private Map<String, Long> scansByRiskLevel = new HashMap<>();
    private List<URLScanResponse> recentScans = new ArrayList<>();

    public DashboardStatsResponse() {}

    public DashboardStatsResponse(long totalScans, long safeScans, long suspiciousScans,
                                  long highRiskScans, long reportsSubmitted,
                                  Map<String, Long> scansByRiskLevel, List<URLScanResponse> recentScans) {
        this.totalScans = totalScans;
        this.safeScans = safeScans;
        this.suspiciousScans = suspiciousScans;
        this.highRiskScans = highRiskScans;
        this.reportsSubmitted = reportsSubmitted;
        this.scansByRiskLevel = scansByRiskLevel != null ? scansByRiskLevel : new HashMap<>();
        this.recentScans = recentScans != null ? recentScans : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalScans;
        private long safeScans;
        private long suspiciousScans;
        private long highRiskScans;
        private long reportsSubmitted;
        private Map<String, Long> scansByRiskLevel = new HashMap<>();
        private List<URLScanResponse> recentScans = new ArrayList<>();

        public Builder totalScans(long totalScans) { this.totalScans = totalScans; return this; }
        public Builder safeScans(long safeScans) { this.safeScans = safeScans; return this; }
        public Builder suspiciousScans(long suspiciousScans) { this.suspiciousScans = suspiciousScans; return this; }
        public Builder highRiskScans(long highRiskScans) { this.highRiskScans = highRiskScans; return this; }
        public Builder reportsSubmitted(long reportsSubmitted) { this.reportsSubmitted = reportsSubmitted; return this; }
        public Builder scansByRiskLevel(Map<String, Long> scansByRiskLevel) { this.scansByRiskLevel = scansByRiskLevel; return this; }
        public Builder recentScans(List<URLScanResponse> recentScans) { this.recentScans = recentScans; return this; }

        public DashboardStatsResponse build() {
            return new DashboardStatsResponse(totalScans, safeScans, suspiciousScans, highRiskScans, reportsSubmitted, scansByRiskLevel, recentScans);
        }
    }

    public long getTotalScans() { return totalScans; }
    public void setTotalScans(long totalScans) { this.totalScans = totalScans; }
    public long getSafeScans() { return safeScans; }
    public void setSafeScans(long safeScans) { this.safeScans = safeScans; }
    public long getSuspiciousScans() { return suspiciousScans; }
    public void setSuspiciousScans(long suspiciousScans) { this.suspiciousScans = suspiciousScans; }
    public long getHighRiskScans() { return highRiskScans; }
    public void setHighRiskScans(long highRiskScans) { this.highRiskScans = highRiskScans; }
    public long getReportsSubmitted() { return reportsSubmitted; }
    public void setReportsSubmitted(long reportsSubmitted) { this.reportsSubmitted = reportsSubmitted; }
    public Map<String, Long> getScansByRiskLevel() { return scansByRiskLevel; }
    public void setScansByRiskLevel(Map<String, Long> scansByRiskLevel) { this.scansByRiskLevel = scansByRiskLevel; }
    public List<URLScanResponse> getRecentScans() { return recentScans; }
    public void setRecentScans(List<URLScanResponse> recentScans) { this.recentScans = recentScans; }
}
