package com.phishshield;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.service.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineServiceTest {

    private RuleEngineService ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngineService();
    }

    @Test
    @DisplayName("Should detect high risk on banking brand impersonation keyword on free TLD")
    void testDetectBankingPhishingOnFreeTld() {
        String url = "http://hdfc-bank-verify-account.tk/login.php";
        String domain = "hdfc-bank-verify-account.tk";

        RuleEngineService.RuleEvaluationResult result = ruleEngine.evaluateUrl(url, domain);

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 50);
        assertEquals(ThreatCategory.BANKING_PHISHING, result.getThreatCategory());
        assertFalse(result.getIndicators().isEmpty());
    }

    @Test
    @DisplayName("Should detect IP address in hostname")
    void testDetectIpAddressHostname() {
        String url = "http://198.51.100.24/secure/login";
        String domain = "198.51.100.24";

        RuleEngineService.RuleEvaluationResult result = ruleEngine.evaluateUrl(url, domain);

        assertNotNull(result);
        assertTrue(result.getIndicators().stream()
                .anyMatch(i -> "RULE_IP_HOST".equals(i.getRuleTriggered())));
    }

    @Test
    @DisplayName("Should detect @ symbol authentication evasion")
    void testDetectAtSymbolEvasion() {
        String url = "http://google.com@attacker-controlled-site.com/login";
        String domain = "attacker-controlled-site.com";

        RuleEngineService.RuleEvaluationResult result = ruleEngine.evaluateUrl(url, domain);

        assertNotNull(result);
        assertTrue(result.getIndicators().stream()
                .anyMatch(i -> "RULE_AT_SYMBOL".equals(i.getRuleTriggered())));
    }

    @Test
    @DisplayName("Should classify standard legitimate URL as SAFE")
    void testClassifyLegitimateUrlAsSafe() {
        String url = "https://example.com/about";
        String domain = "example.com";

        RuleEngineService.RuleEvaluationResult result = ruleEngine.evaluateUrl(url, domain);

        assertNotNull(result);
        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(ThreatCategory.BENIGN, result.getThreatCategory());
        assertTrue(result.getRiskScore() < 40);
    }
}
