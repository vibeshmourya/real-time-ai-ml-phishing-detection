package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detailed explainability breakdown explaining how the composite risk score was formed,
 * distinguishing available signals, missing checks, primary drivers, and uncertainty factors.
 */
public class ScanExplainabilityDetails {

    private String scoringEngineVersion = "2.0.0-multi-signal-fusion";
    private int normalizedScore;
    private String scoreBand; // "SAFE", "SUSPICIOUS", "HIGH_RISK"
    private Map<String, Object> signalContributions = new LinkedHashMap<>();
    private List<String> primaryDrivers = new ArrayList<>();
    private List<String> unavailableChecks = new ArrayList<>();
    private List<String> uncertaintyFactors = new ArrayList<>();
    private boolean inconclusive;
    private String scoringRationale;
    private String safetyNotice = "Automated risk scores are heuristic and probabilistic assessments, not mathematical guarantees. Always verify sender identity independently.";

    public ScanExplainabilityDetails() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String scoringEngineVersion = "2.0.0-multi-signal-fusion";
        private int normalizedScore;
        private String scoreBand;
        private Map<String, Object> signalContributions = new LinkedHashMap<>();
        private List<String> primaryDrivers = new ArrayList<>();
        private List<String> unavailableChecks = new ArrayList<>();
        private List<String> uncertaintyFactors = new ArrayList<>();
        private boolean inconclusive = false;
        private String scoringRationale;
        private String safetyNotice = "Automated risk scores are heuristic and probabilistic assessments, not mathematical guarantees. Always verify sender identity independently.";

        public Builder scoringEngineVersion(String v) { this.scoringEngineVersion = v; return this; }
        public Builder normalizedScore(int s) { this.normalizedScore = s; return this; }
        public Builder scoreBand(String b) { this.scoreBand = b; return this; }
        public Builder signalContributions(Map<String, Object> c) { this.signalContributions = c != null ? c : new LinkedHashMap<>(); return this; }
        public Builder primaryDrivers(List<String> d) { this.primaryDrivers = d != null ? d : new ArrayList<>(); return this; }
        public Builder unavailableChecks(List<String> u) { this.unavailableChecks = u != null ? u : new ArrayList<>(); return this; }
        public Builder uncertaintyFactors(List<String> f) { this.uncertaintyFactors = f != null ? f : new ArrayList<>(); return this; }
        public Builder inconclusive(boolean i) { this.inconclusive = i; return this; }
        public Builder scoringRationale(String r) { this.scoringRationale = r; return this; }
        public Builder safetyNotice(String n) { this.safetyNotice = n; return this; }

        public ScanExplainabilityDetails build() {
            ScanExplainabilityDetails d = new ScanExplainabilityDetails();
            d.scoringEngineVersion = this.scoringEngineVersion;
            d.normalizedScore = this.normalizedScore;
            d.scoreBand = this.scoreBand;
            d.signalContributions = this.signalContributions;
            d.primaryDrivers = this.primaryDrivers;
            d.unavailableChecks = this.unavailableChecks;
            d.uncertaintyFactors = this.uncertaintyFactors;
            d.inconclusive = this.inconclusive;
            d.scoringRationale = this.scoringRationale;
            d.safetyNotice = this.safetyNotice;
            return d;
        }
    }

    // Getters and Setters
    public String getScoringEngineVersion() { return scoringEngineVersion; }
    public void setScoringEngineVersion(String scoringEngineVersion) { this.scoringEngineVersion = scoringEngineVersion; }

    public int getNormalizedScore() { return normalizedScore; }
    public void setNormalizedScore(int normalizedScore) { this.normalizedScore = normalizedScore; }

    public String getScoreBand() { return scoreBand; }
    public void setScoreBand(String scoreBand) { this.scoreBand = scoreBand; }

    public Map<String, Object> getSignalContributions() { return signalContributions; }
    public void setSignalContributions(Map<String, Object> signalContributions) { this.signalContributions = signalContributions; }

    public List<String> getPrimaryDrivers() { return primaryDrivers; }
    public void setPrimaryDrivers(List<String> primaryDrivers) { this.primaryDrivers = primaryDrivers; }

    public List<String> getUnavailableChecks() { return unavailableChecks; }
    public void setUnavailableChecks(List<String> unavailableChecks) { this.unavailableChecks = unavailableChecks; }

    public List<String> getUncertaintyFactors() { return uncertaintyFactors; }
    public void setUncertaintyFactors(List<String> uncertaintyFactors) { this.uncertaintyFactors = uncertaintyFactors; }

    public boolean isInconclusive() { return inconclusive; }
    public void setInconclusive(boolean inconclusive) { this.inconclusive = inconclusive; }

    public String getScoringRationale() { return scoringRationale; }
    public void setScoringRationale(String scoringRationale) { this.scoringRationale = scoringRationale; }

    public String getSafetyNotice() { return safetyNotice; }
    public void setSafetyNotice(String safetyNotice) { this.safetyNotice = safetyNotice; }
}
