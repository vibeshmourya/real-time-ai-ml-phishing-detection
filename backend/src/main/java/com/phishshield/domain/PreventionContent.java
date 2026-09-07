package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prevention_content", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cat_lang", columnNames = {"threat_category", "language_code"})
})
public class PreventionContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_category", length = 60, nullable = false)
    private ThreatCategory threatCategory;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "short_explanation", columnDefinition = "TEXT", nullable = false)
    private String shortExplanation;

    @Column(name = "attack_mechanism", columnDefinition = "TEXT", nullable = false)
    private String attackMechanism;

    @Column(name = "why_it_matters", columnDefinition = "TEXT", nullable = false)
    private String whyItMatters;

    @Column(name = "immediate_actions", columnDefinition = "TEXT", nullable = false)
    private String immediateActions;

    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "language_code", length = 10, nullable = false)
    private String languageCode = "en";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PreventionContent() {}

    public PreventionContent(Long id, ThreatCategory threatCategory, String title,
                             String shortExplanation, String attackMechanism, String whyItMatters,
                             String immediateActions, String checklistJson, String videoUrl,
                             String audioUrl, String languageCode, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.languageCode = languageCode != null ? languageCode : "en";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        private String languageCode = "en";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PreventionContent build() {
            return new PreventionContent(id, threatCategory, title, shortExplanation, attackMechanism, whyItMatters,
                    immediateActions, checklistJson, videoUrl, audioUrl, languageCode, createdAt, updatedAt);
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
