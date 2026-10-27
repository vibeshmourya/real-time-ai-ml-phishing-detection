package com.phishshield.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private Long conversationId;
    private String reply;
    private boolean isLiveAi;
    private String provider; // "openai", "gemini", or "prahar_nlp"
    private String intent;
    private Double confidence;
    private boolean isFollowUp;
    private java.util.Map<String, Object> extractedEntities;
    private List<String> followUpSuggestions;
    private List<String> actionChecklist;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatResponse() {}

    public ChatResponse(String reply, boolean isLiveAi, String provider, List<String> followUpSuggestions) {
        this.reply = reply;
        this.isLiveAi = isLiveAi;
        this.provider = provider;
        this.followUpSuggestions = followUpSuggestions;
        this.timestamp = LocalDateTime.now();
    }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public boolean isLiveAi() { return isLiveAi; }
    public void setLiveAi(boolean liveAi) { isLiveAi = liveAi; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public boolean isFollowUp() { return isFollowUp; }
    public void setFollowUp(boolean followUp) { isFollowUp = followUp; }

    public java.util.Map<String, Object> getExtractedEntities() { return extractedEntities; }
    public void setExtractedEntities(java.util.Map<String, Object> extractedEntities) { this.extractedEntities = extractedEntities; }

    public List<String> getFollowUpSuggestions() { return followUpSuggestions; }
    public void setFollowUpSuggestions(List<String> followUpSuggestions) { this.followUpSuggestions = followUpSuggestions; }

    public List<String> getActionChecklist() { return actionChecklist; }
    public void setActionChecklist(List<String> actionChecklist) { this.actionChecklist = actionChecklist; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
