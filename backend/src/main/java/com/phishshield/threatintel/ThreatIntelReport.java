package com.phishshield.threatintel;

import java.time.Instant;

/**
 * Encapsulates legitimate threat intelligence reputation findings.
 * Explicitly separates external threat intelligence from ML model and heuristic rule outputs.
 */
public class ThreatIntelReport {

    private boolean configured;
    private String providerName;
    private String status; // "CHECKED", "CLEAN", "MALICIOUS", "DISABLED", "NOT_CONFIGURED", "CONSENT_REQUIRED", "TIMEOUT", "ERROR"
    private Boolean reportedMalicious;
    private Integer threatConfidence; // 0-100 score
    private String threatCategory; // e.g. "SOCIAL_ENGINEERING", "MALWARE", "BENIGN", "UNKNOWN"
    private String details;
    private Instant checkedAt;
    private boolean fromCache;

    public ThreatIntelReport() {}

    public static ThreatIntelReport notConfigured(String provider) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(false);
        r.setProviderName(provider);
        r.setStatus("NOT_CONFIGURED");
        r.setReportedMalicious(null);
        r.setDetails("External threat intelligence service is not configured with an API key.");
        r.setCheckedAt(Instant.now());
        r.setFromCache(false);
        return r;
    }

    public static ThreatIntelReport disabled(String provider) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(false);
        r.setProviderName(provider);
        r.setStatus("DISABLED");
        r.setReportedMalicious(null);
        r.setDetails("External threat intelligence lookup is currently disabled by administrator configuration.");
        r.setCheckedAt(Instant.now());
        r.setFromCache(false);
        return r;
    }

    public static ThreatIntelReport consentRequired(String provider) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(true);
        r.setProviderName(provider);
        r.setStatus("CONSENT_REQUIRED");
        r.setReportedMalicious(null);
        r.setDetails("User consent required before submitting URL to third-party reputation database.");
        r.setCheckedAt(Instant.now());
        r.setFromCache(false);
        return r;
    }

    public static ThreatIntelReport clean(String provider, String details, boolean fromCache) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(true);
        r.setProviderName(provider);
        r.setStatus("CLEAN");
        r.setReportedMalicious(false);
        r.setThreatConfidence(0);
        r.setThreatCategory("BENIGN");
        r.setDetails(details);
        r.setCheckedAt(Instant.now());
        r.setFromCache(fromCache);
        return r;
    }

    public static ThreatIntelReport malicious(String provider, String threatCategory, int confidence, String details, boolean fromCache) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(true);
        r.setProviderName(provider);
        r.setStatus("MALICIOUS");
        r.setReportedMalicious(true);
        r.setThreatConfidence(confidence);
        r.setThreatCategory(threatCategory);
        r.setDetails(details);
        r.setCheckedAt(Instant.now());
        r.setFromCache(fromCache);
        return r;
    }

    public static ThreatIntelReport failure(String provider, String errorMsg) {
        ThreatIntelReport r = new ThreatIntelReport();
        r.setConfigured(true);
        r.setProviderName(provider);
        r.setStatus("ERROR");
        r.setReportedMalicious(null);
        r.setDetails("External reputation query failed: " + errorMsg);
        r.setCheckedAt(Instant.now());
        r.setFromCache(false);
        return r;
    }

    // Getters and Setters
    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getReportedMalicious() { return reportedMalicious; }
    public void setReportedMalicious(Boolean reportedMalicious) { this.reportedMalicious = reportedMalicious; }
    public Integer getThreatConfidence() { return threatConfidence; }
    public void setThreatConfidence(Integer threatConfidence) { this.threatConfidence = threatConfidence; }
    public String getThreatCategory() { return threatCategory; }
    public void setThreatCategory(String threatCategory) { this.threatCategory = threatCategory; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
}
