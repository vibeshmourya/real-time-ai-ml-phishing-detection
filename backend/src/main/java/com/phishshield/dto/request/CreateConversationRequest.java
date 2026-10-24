package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateConversationRequest {

    @NotBlank(message = "Conversation title cannot be blank")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    public CreateConversationRequest() {}

    public CreateConversationRequest(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
