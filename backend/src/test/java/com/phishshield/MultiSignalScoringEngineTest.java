package com.phishshield;

import com.phishshield.domain.IndicatorSeverity;
import com.phishshield.domain.IndicatorType;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.domain.ThreatIndicator;
import com.phishshield.dto.response.MLPredictionResult;
import com.phishshield.dto.response.ScanExplainabilityDetails;
import com.phishshield.service.MultiSignalScoringEngine;
import com.phishshield.service.RuleEngineService;
import com.phishshield.threatintel.ThreatIntelReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiSignalScoringEngineTest {

    private MultiSignalScoringEngine scoringEngine;

    @BeforeEach
    void setUp() {
        scoringEngine = new MultiSignalScoringEngine();
    }

    @Test
    @DisplayName("Threat Intel Dominance: Confirmed malicious listing forces HIGH_RISK score")
    void testThreatIntelMaliciousForcesHighRisk() {
        RuleEngineService.RuleEvaluationResult ruleResult = new RuleEngineService.RuleEvaluationResult(
                20, RiskLevel.SAFE, ThreatCategory.BENIGN, 0.65, "Few rules", "None", new ArrayList<>()
        );
        ThreatIntelReport threatIntel = ThreatIntelReport.malicious("GoogleSafeBrowsing", "MALWARE", 95, "Known malware site", false);

        MultiSignalScoringEngine.CompositeScoringResult result = scoringEngine.computeCompositeScore(
                "http://malware-drop.com", "malware-drop.com", ruleResult, null, threatIntel
        );

        assertTrue(result.getNormalizedScore() >= 90, "Score must be elevated to >=90 by verified threat intel");
        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertEquals(ThreatCategory.MALWARE_DISTRIBUTION, result.getThreatCategory());
        assertTrue(result.getExplainability().getPrimaryDrivers().get(0).contains("GoogleSafeBrowsing"));
    }

    @Test
    @DisplayName("Clean Threat Intel Does Not Override Severe Heuristic Brand Impersonation")
    void testCleanThreatIntelDoesNotOverrideHeuristic() {
        List<ThreatIndicator> indicators = new ArrayList<>();
        indicators.add(ThreatIndicator.builder()
                .indicatorName("Brand Impersonation (sbi)")
                .severity(IndicatorSeverity.CRITICAL)
                .weight(30.0)
                .build());
        indicators.add(ThreatIndicator.builder()
                .indicatorName("Suspicious TLD (.top)")
                .severity(IndicatorSeverity.HIGH)
                .weight(25.0)
                .build());

        RuleEngineService.RuleEvaluationResult ruleResult = new RuleEngineService.RuleEvaluationResult(
                75, RiskLevel.HIGH_RISK, ThreatCategory.BANKING_PHISHING, 0.85, "Multiple phishing flags", "Avoid", indicators
        );
        ThreatIntelReport cleanReport = ThreatIntelReport.clean("VirusTotal", "0/70 engines detected", false);

        MultiSignalScoringEngine.CompositeScoringResult result = scoringEngine.computeCompositeScore(
                "http://sbi-portal-verify.top", "sbi-portal-verify.top", ruleResult, null, cleanReport
        );

        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel(), "Clean threat intel must not override critical structural flags");
        assertTrue(result.getNormalizedScore() >= 70);
        assertTrue(result.getExplainability().getUncertaintyFactors().stream().anyMatch(u -> u.contains("zero-hour")));
    }

    @Test
    @DisplayName("Heuristic Override: Low ML probability does not suppress critical brand spoofing")
    void testHeuristicOverrideOverLowMlProbability() {
        List<ThreatIndicator> indicators = new ArrayList<>();
        indicators.add(ThreatIndicator.builder()
                .indicatorName("Brand Impersonation (hdfc)")
                .severity(IndicatorSeverity.CRITICAL)
                .weight(35.0)
                .build());

        RuleEngineService.RuleEvaluationResult ruleResult = new RuleEngineService.RuleEvaluationResult(
                65, RiskLevel.SUSPICIOUS, ThreatCategory.BANKING_PHISHING, 0.80, "Brand spoofing", "Do not enter credentials", indicators
        );

        // ML model predicting 0.20 (low confidence)
        MLPredictionResult mlResult = new MLPredictionResult();
        mlResult.setAvailable(true);
        mlResult.setMlProbability(0.20);
        mlResult.setRiskScore(20);

        MultiSignalScoringEngine.CompositeScoringResult result = scoringEngine.computeCompositeScore(
                "http://hdfc-bank-login.xyz", "hdfc-bank-login.xyz", ruleResult, mlResult, null
        );

        assertEquals(65, result.getNormalizedScore(), "Heuristic score must override low ML probability");
        assertTrue(result.getExplainability().getUncertaintyFactors().stream().anyMatch(f -> f.contains("ML probability was lower")));
    }

    @Test
    @DisplayName("Missing Checks Tracking: Unconfigured threat intel and offline ML are tracked without false confidence")
    void testMissingChecksTrackedAccurately() {
        RuleEngineService.RuleEvaluationResult ruleResult = new RuleEngineService.RuleEvaluationResult(
                10, RiskLevel.SAFE, ThreatCategory.BENIGN, 0.65, "Benign", "Standard hygiene", new ArrayList<>()
        );
        ThreatIntelReport disabledIntel = ThreatIntelReport.disabled("GoogleSafeBrowsing");

        MultiSignalScoringEngine.CompositeScoringResult result = scoringEngine.computeCompositeScore(
                "https://example.com", "example.com", ruleResult, null, disabledIntel
        );

        ScanExplainabilityDetails exp = result.getExplainability();
        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertTrue(exp.getUnavailableChecks().stream().anyMatch(u -> u.contains("Machine Learning")));
        assertTrue(exp.getUnavailableChecks().stream().anyMatch(u -> u.contains("External Threat Intelligence")));
        assertNotNull(exp.getSafetyNotice());
    }

    @Test
    @DisplayName("Inconclusive Borderline Detection: Flags ambiguity when score is 30-45 and dependencies are missing")
    void testInconclusiveDetectionOnBorderlineMissingDependencies() {
        RuleEngineService.RuleEvaluationResult ruleResult = new RuleEngineService.RuleEvaluationResult(
                35, RiskLevel.SUSPICIOUS, ThreatCategory.UNKNOWN, 0.65, "Suspicious length", "Verify", new ArrayList<>()
        );

        MultiSignalScoringEngine.CompositeScoringResult result = scoringEngine.computeCompositeScore(
                "http://borderline-domain.com", "borderline-domain.com", ruleResult, null, null
        );

        assertTrue(result.getExplainability().isInconclusive(), "Borderline score without ML or Threat Intel must be marked inconclusive");
    }
}
