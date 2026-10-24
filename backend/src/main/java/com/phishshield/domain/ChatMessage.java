package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @Column(name = "role", length = 20, nullable = false) // "user" or "assistant"
    private String role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "is_live_ai")
    private Boolean isLiveAi = false;

    @Column(name = "intent", length = 50)
    private String intent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {}

    public ChatMessage(ChatConversation conversation, String role, String content, String provider, Boolean isLiveAi, String intent) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.isLiveAi = isLiveAi != null ? isLiveAi : false;
        this.intent = intent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChatConversation getConversation() { return conversation; }
    public void setConversation(ChatConversation conversation) { this.conversation = conversation; }

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
