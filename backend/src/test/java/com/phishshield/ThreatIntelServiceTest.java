package com.phishshield;

import com.phishshield.threatintel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ThreatIntelServiceTest {

    @Autowired
    private ThreatIntelligenceService threatIntelService;

    @Autowired
    private com.phishshield.service.SSRFProtectionService ssrfProtectionService;

    @BeforeEach
    void setUp() {
        threatIntelService.clearCache();
    }

    @Test
    @DisplayName("Threat Intel: When disabled by default, returns honest DISABLED status without fabricating")
    void testDisabledThreatIntelReturnsDisabled() {
        ThreatIntelReport report = threatIntelService.checkReputation("http://phishing-test-domain.com", "phishing-test-domain.com", true);

        assertNotNull(report);
        assertEquals("DISABLED", report.getStatus(), "Disabled threat intel must report DISABLED");
        assertNull(report.getReportedMalicious(), "Must not fabricate malicious verdict when disabled");
        assertFalse(report.isConfigured());
        assertFalse(report.isFromCache());
    }

    @Test
    @DisplayName("Privacy & Consent: When consent is required but not granted, returns CONSENT_REQUIRED")
    void testConsentRequiredWhenConfigured() {
        // Construct a service instance with enabled=true, requireConsent=true, but unconfigured key
        GoogleSafeBrowsingProvider provider = new GoogleSafeBrowsingProvider("dummy-key", 1000, new org.springframework.boot.web.client.RestTemplateBuilder());
        VirusTotalProvider vt = new VirusTotalProvider("", 1000, new org.springframework.boot.web.client.RestTemplateBuilder());
        ThreatIntelligenceService consentService = new ThreatIntelligenceService(true, "google", true, provider, vt, ssrfProtectionService);

        ThreatIntelReport report = consentService.checkReputation("http://test-bank.com", "test-bank.com", false);

        assertEquals("CONSENT_REQUIRED", report.getStatus(), "Should return CONSENT_REQUIRED when user consent is false");
        assertNull(report.getReportedMalicious());
    }

    @Test
    @DisplayName("SSRF Defense: Blocks private IP / localhost destinations before outbound reputation check")
    void testSsrfBlockedBeforeOutboundCheck() {
        GoogleSafeBrowsingProvider provider = new GoogleSafeBrowsingProvider("dummy-key", 1000, new org.springframework.boot.web.client.RestTemplateBuilder());
        VirusTotalProvider vt = new VirusTotalProvider("", 1000, new org.springframework.boot.web.client.RestTemplateBuilder());
        ThreatIntelligenceService activeService = new ThreatIntelligenceService(true, "google", false, provider, vt, ssrfProtectionService);

        ThreatIntelReport report = activeService.checkReputation("http://127.0.0.1:8080/admin", "127.0.0.1", true);

        assertEquals("ERROR", report.getStatus());
        assertTrue(report.getDetails().contains("SSRF"), "Report should record SSRF prevention blocking");
    }

    @Test
    @DisplayName("Separation of Concerns: ThreatIntel report cleanly distinguishes external vendor from local rule heuristics")
    void testThreatIntelSeparation() {
        ThreatIntelReport clean = ThreatIntelReport.clean("GoogleSafeBrowsing", "No threats found", false);
        assertEquals("CLEAN", clean.getStatus());
        assertFalse(clean.getReportedMalicious());
        assertEquals("BENIGN", clean.getThreatCategory());

        ThreatIntelReport mal = ThreatIntelReport.malicious("VirusTotal", "MALWARE", 95, "2 detections", false);
        assertEquals("MALICIOUS", mal.getStatus());
        assertTrue(mal.getReportedMalicious());
        assertEquals(95, mal.getThreatConfidence());
    }
}
