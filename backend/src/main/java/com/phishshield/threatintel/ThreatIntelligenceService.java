package com.phishshield.threatintel;

import com.phishshield.service.SSRFProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service orchestrating legitimate external threat intelligence queries.
 * Handles provider selection, TTL caching, SSRF outbound protection,
 * user disclosure/consent compliance, and safe error handling without fabricating results.
 */
@Service
public class ThreatIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelligenceService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final boolean enabled;
    private final String selectedProvider;
    private final boolean requireConsent;
    private final GoogleSafeBrowsingProvider googleProvider;
    private final VirusTotalProvider virusTotalProvider;
    private final SSRFProtectionService ssrfProtectionService;

    // In-memory cache: normalized domain/URL -> CachedReport
    private final Map<String, CachedReport> cache = new ConcurrentHashMap<>();

    public ThreatIntelligenceService(
            @Value("${app.threat-intel.enabled:false}") boolean enabled,
            @Value("${app.threat-intel.provider:google}") String selectedProvider,
            @Value("${app.threat-intel.require-consent:true}") boolean requireConsent,
            GoogleSafeBrowsingProvider googleProvider,
            VirusTotalProvider virusTotalProvider,
            SSRFProtectionService ssrfProtectionService) {
        this.enabled = enabled;
        this.selectedProvider = selectedProvider != null ? selectedProvider.trim().toLowerCase() : "google";
        this.requireConsent = requireConsent;
        this.googleProvider = googleProvider;
        this.virusTotalProvider = virusTotalProvider;
        this.ssrfProtectionService = ssrfProtectionService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ThreatIntelligenceProvider getActiveProvider() {
        if ("virustotal".equalsIgnoreCase(selectedProvider)) {
            return virusTotalProvider;
        }
        return googleProvider;
    }

    /**
     * Queries legitimate threat intelligence provider for a given URL and domain.
     * Respects user consent, caches results, and prevents SSRF.
     */
    public ThreatIntelReport checkReputation(String url, String domain, boolean userConsent) {
        if (!enabled) {
            return ThreatIntelReport.disabled(getActiveProvider().getProviderName());
        }

        ThreatIntelligenceProvider provider = getActiveProvider();

        if (!provider.isConfigured()) {
            return ThreatIntelReport.notConfigured(provider.getProviderName());
        }

        // Privacy & Disclosure: require explicit consent before forwarding URL to third party
        if (requireConsent && !userConsent) {
            return ThreatIntelReport.consentRequired(provider.getProviderName());
        }

        // SSRF outbound target check
        try {
            ssrfProtectionService.validateUrl(url);
        } catch (Exception e) {
            log.warn("SSRF check rejected target URL before threat intel lookup: {}", e.getMessage());
            return ThreatIntelReport.failure(provider.getProviderName(), "Target URL blocked by SSRF policy: " + e.getMessage());
        }

        // Check Cache
        String cacheKey = (domain != null && !domain.isBlank()) ? domain.toLowerCase() : url;
        CachedReport cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            ThreatIntelReport report = cached.report;
            report.setFromCache(true);
            return report;
        }

        // Query active provider
        ThreatIntelReport freshReport = provider.checkReputation(url, domain);

        // Cache valid clean or malicious responses
        if ("CLEAN".equals(freshReport.getStatus()) || "MALICIOUS".equals(freshReport.getStatus())) {
            cache.put(cacheKey, new CachedReport(freshReport, Instant.now().plus(CACHE_TTL)));
        }

        return freshReport;
    }

    public void clearCache() {
        cache.clear();
    }

    private static class CachedReport {
        final ThreatIntelReport report;
        final Instant expiresAt;

        CachedReport(ThreatIntelReport report, Instant expiresAt) {
            this.report = report;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
