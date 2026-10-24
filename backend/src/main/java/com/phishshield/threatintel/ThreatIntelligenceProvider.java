package com.phishshield.threatintel;

/**
 * Interface abstraction for legitimate URL/Domain reputation threat intelligence providers.
 */
public interface ThreatIntelligenceProvider {

    /**
     * Unique identifier for the provider (e.g. "GoogleSafeBrowsing", "VirusTotal").
     */
    String getProviderName();

    /**
     * Returns true if the provider is enabled and properly configured with an API key.
     */
    boolean isConfigured();

    /**
     * Checks URL/Domain reputation against legitimate external threat database.
     * Never fabricates results when unavailable or unconfigured.
     */
    ThreatIntelReport checkReputation(String url, String domain);
}
