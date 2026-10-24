package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.List;

public class OCRTextAnalysisResult {

    private String extractedText;
    private int characterCount;
    private String extractionStatus; // "SUCCESS", "EMPTY_INPUT", "NO_TEXT_DETECTED", "OCR_UNAVAILABLE"
    private String ocrEngine;
    private List<ThreatIndicatorResponse> textIndicators = new ArrayList<>();
    private int heuristicRiskScore;
    private String heuristicRiskLevel; // "SAFE", "SUSPICIOUS", "HIGH_RISK"
    private String summary;

    public OCRTextAnalysisResult() {}

    public OCRTextAnalysisResult(String extractedText, int characterCount, String extractionStatus,
                                 String ocrEngine, List<ThreatIndicatorResponse> textIndicators,
                                 int heuristicRiskScore, String heuristicRiskLevel, String summary) {
        this.extractedText = extractedText;
        this.characterCount = characterCount;
        this.extractionStatus = extractionStatus;
        this.ocrEngine = ocrEngine;
        this.textIndicators = textIndicators != null ? textIndicators : new ArrayList<>();
        this.heuristicRiskScore = heuristicRiskScore;
        this.heuristicRiskLevel = heuristicRiskLevel;
        this.summary = summary;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String extractedText;
        private int characterCount;
        private String extractionStatus;
        private String ocrEngine;
        private List<ThreatIndicatorResponse> textIndicators = new ArrayList<>();
        private int heuristicRiskScore;
        private String heuristicRiskLevel;
        private String summary;

        public Builder extractedText(String extractedText) { this.extractedText = extractedText; return this; }
        public Builder characterCount(int count) { this.characterCount = count; return this; }
        public Builder extractionStatus(String status) { this.extractionStatus = status; return this; }
        public Builder ocrEngine(String engine) { this.ocrEngine = engine; return this; }
        public Builder textIndicators(List<ThreatIndicatorResponse> indicators) {
            this.textIndicators = indicators != null ? indicators : new ArrayList<>();
            return this;
        }
        public Builder heuristicRiskScore(int score) { this.heuristicRiskScore = score; return this; }
        public Builder heuristicRiskLevel(String level) { this.heuristicRiskLevel = level; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }

        public OCRTextAnalysisResult build() {
            return new OCRTextAnalysisResult(extractedText, characterCount, extractionStatus, ocrEngine,
                    textIndicators, heuristicRiskScore, heuristicRiskLevel, summary);
        }
    }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public int getCharacterCount() { return characterCount; }
    public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }

    public String getOcrEngine() { return ocrEngine; }
    public void setOcrEngine(String ocrEngine) { this.ocrEngine = ocrEngine; }

    public List<ThreatIndicatorResponse> getTextIndicators() { return textIndicators; }
    public void setTextIndicators(List<ThreatIndicatorResponse> textIndicators) { this.textIndicators = textIndicators; }

    public int getHeuristicRiskScore() { return heuristicRiskScore; }
    public void setHeuristicRiskScore(int heuristicRiskScore) { this.heuristicRiskScore = heuristicRiskScore; }

    public String getHeuristicRiskLevel() { return heuristicRiskLevel; }
    public void setHeuristicRiskLevel(String heuristicRiskLevel) { this.heuristicRiskLevel = heuristicRiskLevel; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
