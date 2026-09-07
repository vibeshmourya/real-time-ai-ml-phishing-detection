package com.phishshield.dto.request;

import com.phishshield.domain.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @NotNull(message = "Scan ID is required")
    private Long scanId;

    @NotNull(message = "Feedback type is required")
    private FeedbackType feedbackType;

    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;

    public FeedbackRequest() {}

    public FeedbackRequest(Long scanId, FeedbackType feedbackType, String comments) {
        this.scanId = scanId;
        this.feedbackType = feedbackType;
        this.comments = comments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long scanId;
        private FeedbackType feedbackType;
        private String comments;

        public Builder scanId(Long scanId) { this.scanId = scanId; return this; }
        public Builder feedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; return this; }
        public Builder comments(String comments) { this.comments = comments; return this; }

        public FeedbackRequest build() {
            return new FeedbackRequest(scanId, feedbackType, comments);
        }
    }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
