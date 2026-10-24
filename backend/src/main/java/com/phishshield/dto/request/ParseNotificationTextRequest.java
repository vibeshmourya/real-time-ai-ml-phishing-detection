package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ParseNotificationTextRequest {

    @NotBlank(message = "Notification text cannot be empty")
    @Size(max = 2000, message = "Text exceeds maximum 2000 characters")
    private String text;

    public ParseNotificationTextRequest() {}

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
