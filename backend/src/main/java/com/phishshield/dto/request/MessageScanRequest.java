package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageScanRequest {

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 5000, message = "Message content cannot exceed 5,000 characters")
    private String messageContent;

    public MessageScanRequest() {}

    public MessageScanRequest(String messageContent) {
        this.messageContent = messageContent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String messageContent;

        public Builder messageContent(String messageContent) { this.messageContent = messageContent; return this; }

        public MessageScanRequest build() {
            return new MessageScanRequest(messageContent);
        }
    }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
}
