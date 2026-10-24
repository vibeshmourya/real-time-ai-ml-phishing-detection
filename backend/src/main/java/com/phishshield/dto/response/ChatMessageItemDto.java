package com.phishshield.dto.response;

import java.time.LocalDateTime;

public class ChatMessageItemDto {

    private Long id;
    private String role;
    private String content;
    private String provider;
    private Boolean isLiveAi;
    private String intent;
    private LocalDateTime createdAt;

    public ChatMessageItemDto() {}

    public ChatMessageItemDto(Long id, String role, String content, String provider, Boolean isLiveAi, String intent, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.isLiveAi = isLiveAi;
        this.intent = intent;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Boolean getIsLiveAi() { return isLiveAi; }
    public void setIsLiveAi(Boolean isLiveAi) { this.isLiveAi = isLiveAi; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
