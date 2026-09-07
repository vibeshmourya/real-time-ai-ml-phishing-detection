package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailScanRequest {

    @NotBlank(message = "Email content cannot be empty")
    @Size(max = 10000, message = "Email content cannot exceed 10,000 characters")
    private String emailContent;

    public EmailScanRequest() {}

    public EmailScanRequest(String emailContent) {
        this.emailContent = emailContent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String emailContent;

        public Builder emailContent(String emailContent) { this.emailContent = emailContent; return this; }

        public EmailScanRequest build() {
            return new EmailScanRequest(emailContent);
        }
    }

    public String getEmailContent() { return emailContent; }
    public void setEmailContent(String emailContent) { this.emailContent = emailContent; }
}
