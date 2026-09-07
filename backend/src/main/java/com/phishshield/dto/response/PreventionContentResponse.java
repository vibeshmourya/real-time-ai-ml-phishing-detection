package com.phishshield.dto.response;

import com.phishshield.domain.ThreatCategory;

public class PreventionContentResponse {

    private Long id;
    private ThreatCategory threatCategory;
    private String title;
    private String shortExplanation;
    private String attackMechanism;
    private String whyItMatters;
    private String immediateActions;
    private String checklistJson;
    private String videoUrl;
    private String audioUrl;
    private String languageCode;

    public PreventionContentResponse() {}

    public PreventionContentResponse(Long id, ThreatCategory threatCategory, String title,
                                     String shortExplanation, String attackMechanism, String whyItMatters,
                                     String immediateActions, String checklistJson, String videoUrl,
                                     String audioUrl, String languageCode) {
        this.id = id;
        this.threatCategory = threatCategory;
        this.title = title;
        this.shortExplanation = shortExplanation;
        this.attackMechanism = attackMechanism;
        this.whyItMatters = whyItMatters;
        this.immediateActions = immediateActions;
        this.checklistJson = checklistJson;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.languageCode = languageCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private ThreatCategory threatCategory;
        private String title;
        private String shortExplanation;
        private String attackMechanism;
        private String whyItMatters;
        private String immediateActions;
        private String checklistJson;
        private String videoUrl;
        private String audioUrl;
        private String languageCode;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder threatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder shortExplanation(String shortExplanation) { this.shortExplanation = shortExplanation; return this; }
        public Builder attackMechanism(String attackMechanism) { this.attackMechanism = attackMechanism; return this; }
        public Builder whyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; return this; }
        public Builder immediateActions(String immediateActions) { this.immediateActions = immediateActions; return this; }
        public Builder checklistJson(String checklistJson) { this.checklistJson = checklistJson; return this; }
        public Builder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }
        public Builder audioUrl(String audioUrl) { this.audioUrl = audioUrl; return this; }
        public Builder languageCode(String languageCode) { this.languageCode = languageCode; return this; }

        public PreventionContentResponse build() {
            return new PreventionContentResponse(id, threatCategory, title, shortExplanation, attackMechanism, whyItMatters, immediateActions, checklistJson, videoUrl, audioUrl, languageCode);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ThreatCategory getThreatCategory() { return threatCategory; }
    public void setThreatCategory(ThreatCategory threatCategory) { this.threatCategory = threatCategory; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getShortExplanation() { return shortExplanation; }
    public void setShortExplanation(String shortExplanation) { this.shortExplanation = shortExplanation; }
    public String getAttackMechanism() { return attackMechanism; }
    public void setAttackMechanism(String attackMechanism) { this.attackMechanism = attackMechanism; }
    public String getWhyItMatters() { return whyItMatters; }
    public void setWhyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; }
    public String getImmediateActions() { return immediateActions; }
    public void setImmediateActions(String immediateActions) { this.immediateActions = immediateActions; }
    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
}
