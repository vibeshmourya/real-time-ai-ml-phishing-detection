package com.phishshield.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ChatConversationDto {

    private Long id;
    private String title;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long messageCount;
    private List<ChatMessageItemDto> messages = Collections.emptyList();

    public ChatConversationDto() {}

    public ChatConversationDto(Long id, String title, Boolean isPinned, LocalDateTime createdAt, LocalDateTime updatedAt, long messageCount) {
        this.id = id;
        this.title = title;
        this.isPinned = isPinned != null ? isPinned : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messageCount = messageCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getIsPinned() { return isPinned; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public long getMessageCount() { return messageCount; }
    public void setMessageCount(long messageCount) { this.messageCount = messageCount; }

    public List<ChatMessageItemDto> getMessages() { return messages; }
    public void setMessages(List<ChatMessageItemDto> messages) {
        this.messages = messages != null ? messages : Collections.emptyList();
    }
}
