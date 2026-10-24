package com.phishshield.service;

import com.phishshield.domain.IndicatorSeverity;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.domain.ThreatIndicator;
import com.phishshield.dto.response.MLPredictionResult;
import com.phishshield.dto.response.ScanExplainabilityDetails;
import com.phishshield.threatintel.ThreatIntelReport;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Intelligent, explainable multi-signal threat scoring engine.
 * Combines structural heuristics, machine learning inference, and external threat intelligence
 * into a single normalized (0-100) score without double-counting correlated features.
 * Explicitly tracks available signals, missing checks, primary drivers, and uncertainty factors.
 */
@Service
public class MultiSignalScoringEngine {

    public static class CompositeScoringResult {
        private final int normalizedScore;
        private final RiskLevel riskLevel;
        private final ThreatCategory threatCategory;
        private final double confidenceScore;
        private final String explanationSummary;
        private final String recommendedActions;
        private final ScanExplainabilityDetails explainability;

        public CompositeScoringResult(int normalizedScore, RiskLevel riskLevel, ThreatCategory threatCategory,
                                      double confidenceScore, String explanationSummary, String recommendedActions,
                                      ScanExplainabilityDetails explainability) {
            this.normalizedScore = normalizedScore;
            this.riskLevel = riskLevel;
            this.threatCategory = threatCategory;
            this.confidenceScore = confidenceScore;
            this.explanationSummary = explanationSummary;
            this.recommendedActions = recommendedActions;
            this.explainability = explainability;
        }

        public int getNormalizedScore() { return normalizedScore; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public ThreatCategory getThreatCategory() { return threatCategory; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getExplanationSummary() { return explanationSummary; }
        public String getRecommendedActions() { return recommendedActions; }
        public ScanExplainabilityDetails getExplainability() { return explainability; }
    }

    public CompositeScoringResult computeCompositeScore(
            String rawUrl,
            String domain,
            RuleEngineService.RuleEvaluationResult ruleResult,
            MLPredictionResult mlResult,
            ThreatIntelReport threatIntel) {

        Map<String, Object> contributions = new LinkedHashMap<>();
        List<String> primaryDrivers = new ArrayList<>();
        List<String> unavailableChecks = new ArrayList<>();
        List<String> uncertaintyFactors = new ArrayList<>();

        int heuristicScore = ruleResult != null ? ruleResult.getRiskScore() : 0;
        ThreatCategory category = ruleResult != null ? ruleResult.getThreatCategory() : ThreatCategory.BENIGN;
        contributions.put("heuristicSubtotal", heuristicScore);

        // 1. Evaluate Heuristic Rule Primary Drivers
        if (ruleResult != null && !ruleResult.getIndicators().isEmpty()) {
            List<ThreatIndicator> sortedInds = new ArrayList<>(ruleResult.getIndicators());
            sortedInds.sort(Comparator.comparingDouble(ThreatIndicator::getWeight).reversed());
            for (int i = 0; i < Math.min(3, sortedInds.size()); i++) {
                ThreatIndicator ti = sortedInds.get(i);
                primaryDrivers.add(ti.getIndicatorName() + " (" + ti.getSeverity() + ", weight: " + ti.getWeight() + ")");
            }
        }

        // 2. Evaluate Machine Learning Signal
        boolean mlActive = mlResult != null && mlResult.isAvailable();
        Double mlProb = mlActive ? mlResult.getMlProbability() : null;
        int mlScore = 0;

        if (mlActive && mlProb != null) {
            mlScore = mlResult.getRiskScore() != null ? mlResult.getRiskScore() : (int) Math.round(mlProb * 100.0);
            contributions.put("mlProbability", Math.round(mlProb * 1000.0) / 1000.0);
            contributions.put("mlScoreContribution", mlScore);
            contributions.put("mlModelName", mlResult.getModelName() != null ? mlResult.getModelName() : "FastAPI ML");
            if (mlProb >= 0.65) {
                primaryDrivers.add("ML Classifier Phishing Probability: " + Math.round(mlProb * 100) + "%");
            }
        } else {
            unavailableChecks.add("Machine Learning Inference (" +
                    (mlResult != null && mlResult.getFailureReason() != null ? mlResult.getFailureReason() : "ML Service Offline") + ")");
            uncertaintyFactors.add("Classification evaluated without statistical feature weighting from ML microservice.");
            contributions.put("mlStatus", "UNAVAILABLE");
        }

        // 3. Evaluate External Threat Intelligence Signal
        boolean threatIntelActive = threatIntel != null && "CHECKED".equalsIgnoreCase(threatIntel.getStatus())
                || (threatIntel != null && ("CLEAN".equalsIgnoreCase(threatIntel.getStatus()) || "MALICIOUS".equalsIgnoreCase(threatIntel.getStatus())));
        Boolean reportedMalicious = threatIntel != null ? threatIntel.getReportedMalicious() : null;

        if (threatIntel != null) {
            contributions.put("threatIntelProvider", threatIntel.getProviderName());
            contributions.put("threatIntelStatus", threatIntel.getStatus());
            contributions.put("threatIntelFromCache", threatIntel.isFromCache());

            if (Boolean.TRUE.equals(reportedMalicious)) {
                primaryDrivers.add(0, "External Threat Intelligence: Confirmed malicious listing by " + threatIntel.getProviderName());
                contributions.put("threatIntelVerdict", "MALICIOUS");
            } else if (Boolean.FALSE.equals(reportedMalicious)) {
                contributions.put("threatIntelVerdict", "CLEAN");
                uncertaintyFactors.add("Threat intelligence found no known listings; zero-hour phishing domains may not yet be indexed.");
            } else {
                unavailableChecks.add("External Threat Intelligence (" + threatIntel.getDetails() + ")");
            }
        } else {
            unavailableChecks.add("External Threat Intelligence (Not evaluated)");
            contributions.put("threatIntelStatus", "SKIPPED");
        }

        // 4. Multi-Signal Fusion Scoring Logic
        int fusedScore;
        String rationale;

        if (Boolean.TRUE.equals(reportedMalicious)) {
            // Signal 1: External Threat Intel Confirmed Malicious
            int confidenceBoost = threatIntel.getThreatConfidence() != null ? threatIntel.getThreatConfidence() : 90;
            fusedScore = Math.max(90, Math.min(99, confidenceBoost));
            rationale = "High-confidence external threat intelligence match overrides baseline score.";
            if ("MALWARE".equalsIgnoreCase(threatIntel.getThreatCategory())) {
                category = ThreatCategory.MALWARE_DISTRIBUTION;
            } else if (category == ThreatCategory.BENIGN) {
                category = ThreatCategory.CREDENTIAL_THEFT;
            }
        } else if (mlActive) {
            // Signal 2: Hybrid Heuristics + Machine Learning
            if (heuristicScore >= 50 && mlScore >= 50) {
                // Both independent engines detect high risk -> Reinforcing consensus
                fusedScore = Math.max(heuristicScore, mlScore);
                rationale = "Corroborated detection: Both heuristic structural analysis and ML feature inference indicate high threat.";
            } else if (heuristicScore >= 40 && mlScore < 40) {
                // Heuristic flags clear deception; do not allow low ML probability to suppress critical structural rules
                fusedScore = heuristicScore;
                rationale = "Heuristic override: Critical structural indicators (e.g. brand impersonation or punycode) take precedence over conservative ML model.";
                uncertaintyFactors.add("ML probability was lower than structural indicator score.");
            } else if (heuristicScore < 30 && mlScore >= 60) {
                // ML detected anomalous feature combination not covered by explicit rules
                fusedScore = (int) Math.round((0.3 * heuristicScore) + (0.7 * mlScore));
                rationale = "ML-driven detection: Statistical feature distribution indicates phishing patterns despite lack of keyword matches.";
            } else {
                // Low risk consensus
                fusedScore = Math.max(heuristicScore, mlScore);
                rationale = "Consensus benign: Neither heuristic rules nor statistical model identified significant deception signals.";
            }
        } else {
            // Signal 3: Heuristics Only (Fallback)
            fusedScore = heuristicScore;
            rationale = "Assessed exclusively via Prahar deterministic heuristic rule engine.";
        }

        // Bound to [0, 100]
        fusedScore = Math.max(0, Math.min(100, fusedScore));

        // Protocol Check: Flag unencrypted plain HTTP if not local
        if (rawUrl != null && rawUrl.startsWith("http://") && !rawUrl.contains(".internal")) {
            uncertaintyFactors.add("Unencrypted HTTP protocol in use; susceptible to on-path interception.");
        }

        // 5. Derive Risk Level Band
        RiskLevel finalRiskLevel;
        if (fusedScore >= 70) {
            finalRiskLevel = RiskLevel.HIGH_RISK;
            if (category == ThreatCategory.BENIGN) {
                category = ThreatCategory.CREDENTIAL_THEFT;
            }
        } else if (fusedScore >= 35) {
            finalRiskLevel = RiskLevel.SUSPICIOUS;
            if (category == ThreatCategory.BENIGN) {
                category = ThreatCategory.UNKNOWN;
            }
        } else {
            finalRiskLevel = RiskLevel.SAFE;
            category = ThreatCategory.BENIGN;
        }

        // Inconclusive Check: Borderline score with missing ML and missing Threat Intel
        boolean inconclusive = (fusedScore >= 30 && fusedScore <= 45) && !mlActive && !threatIntelActive;
        if (inconclusive) {
            uncertaintyFactors.add("Score is borderline (30-45) and both ML and Threat Intelligence checks were unavailable.");
        }

        // Calculate Confidence Score
        double baseConfidence = 0.65;
        if (mlActive) baseConfidence += 0.15;
        if (threatIntelActive) baseConfidence += 0.15;
        if (ruleResult != null && !ruleResult.getIndicators().isEmpty()) {
            baseConfidence += Math.min(0.10, ruleResult.getIndicators().size() * 0.03);
        }
        double finalConfidence = Math.min(0.99, Math.max(0.50, baseConfidence));

        // Synthesize Explanation and Recommended Actions
        String explanationSummary;
        String recommendedActions;

        if (finalRiskLevel == RiskLevel.HIGH_RISK) {
            explanationSummary = "High-confidence phishing indicators identified. " +
                    (primaryDrivers.isEmpty() ? "Deceptive patterns detected." : "Primary driver: " + primaryDrivers.get(0));
            recommendedActions = "DO NOT enter credentials, OTPs, or financial details. Leave this web page immediately.";
        } else if (finalRiskLevel == RiskLevel.SUSPICIOUS) {
            explanationSummary = "Potential threat indicators observed. " +
                    (primaryDrivers.isEmpty() ? "Atypical domain characteristics." : "Leading indicator: " + primaryDrivers.get(0));
            recommendedActions = "Proceed with caution. Independently verify the organization before interacting.";
        } else {
            explanationSummary = "No typical phishing anomalies or high-risk deception patterns detected.";
            recommendedActions = "Maintain standard cyber hygiene. Confirm HTTPS lock and genuine address bar domain before logging in.";
        }

        ScanExplainabilityDetails explainability = ScanExplainabilityDetails.builder()
                .scoringEngineVersion("2.1.0-explainable-fusion")
                .normalizedScore(fusedScore)
                .scoreBand(finalRiskLevel.name())
                .signalContributions(contributions)
                .primaryDrivers(primaryDrivers)
                .unavailableChecks(unavailableChecks)
                .uncertaintyFactors(uncertaintyFactors)
                .inconclusive(inconclusive)
                .scoringRationale(rationale)
                .build();

        return new CompositeScoringResult(
                fusedScore,
                finalRiskLevel,
                category,
                finalConfidence,
                explanationSummary,
                recommendedActions,
                explainability
        );
    }
}
