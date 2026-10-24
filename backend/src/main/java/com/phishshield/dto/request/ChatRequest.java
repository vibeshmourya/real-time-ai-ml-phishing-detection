package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message exceeds maximum allowed length of 2000 characters")
    private String message;

    private Long conversationId;
    private Long scanId;
    private String scanUrl;
    private Integer scanRiskScore;
    private String scanRiskLevel;
    private String scanThreatCategory;
    private String scanExplanation;

    private List<ChatMessageDto> conversationHistory;

    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }

    public String getScanUrl() { return scanUrl; }
    public void setScanUrl(String scanUrl) { this.scanUrl = scanUrl; }

    public Integer getScanRiskScore() { return scanRiskScore; }
    public void setScanRiskScore(Integer scanRiskScore) { this.scanRiskScore = scanRiskScore; }

    public String getScanRiskLevel() { return scanRiskLevel; }
    public void setScanRiskLevel(String scanRiskLevel) { this.scanRiskLevel = scanRiskLevel; }

    public String getScanThreatCategory() { return scanThreatCategory; }
    public void setScanThreatCategory(String scanThreatCategory) { this.scanThreatCategory = scanThreatCategory; }

    public String getScanExplanation() { return scanExplanation; }
    public void setScanExplanation(String scanExplanation) { this.scanExplanation = scanExplanation; }

    public List<ChatMessageDto> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ChatMessageDto> conversationHistory) { this.conversationHistory = conversationHistory; }
}
