package com.phishshield.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String usernameOrEmail;
        private String password;

        public Builder usernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; return this; }
        public Builder password(String password) { this.password = password; return this; }

        public LoginRequest build() {
            return new LoginRequest(usernameOrEmail, password);
        }
    }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
