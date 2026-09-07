package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private URLScan scan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 30, nullable = false)
    private FeedbackType feedbackType;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Feedback() {}

    public Feedback(Long id, URLScan scan, User user, FeedbackType feedbackType, String comments, LocalDateTime createdAt) {
        this.id = id;
        this.scan = scan;
        this.user = user;
        this.feedbackType = feedbackType;
        this.comments = comments;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private URLScan scan;
        private User user;
        private FeedbackType feedbackType;
        private String comments;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder scan(URLScan scan) { this.scan = scan; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder feedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; return this; }
        public Builder comments(String comments) { this.comments = comments; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Feedback build() {
            return new Feedback(id, scan, user, feedbackType, comments, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public URLScan getScan() { return scan; }
    public void setScan(URLScan scan) { this.scan = scan; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
