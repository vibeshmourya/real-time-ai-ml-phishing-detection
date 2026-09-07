package com.phishshield.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String message;
    private int status;
    private String error;
    private String path;
    private Map<String, String> validationErrors;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse() {}

    public ErrorResponse(boolean success, String message, int status, String error, String path,
                         Map<String, String> validationErrors, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.error = error;
        this.path = path;
        this.validationErrors = validationErrors;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String message;
        private int status;
        private String error;
        private String path;
        private Map<String, String> validationErrors;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder status(int status) { this.status = status; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder validationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ErrorResponse build() {
            return new ErrorResponse(success, message, status, error, path, validationErrors, timestamp);
        }
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Map<String, String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
