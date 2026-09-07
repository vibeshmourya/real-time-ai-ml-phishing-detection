package com.phishshield.controller;

import com.phishshield.domain.User;
import com.phishshield.dto.request.LoginRequest;
import com.phishshield.dto.request.RegisterRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.AuthResponse;
import com.phishshield.dto.response.UserProfileResponse;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.AuthService;
import com.phishshield.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, authentication and profile endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerUser(request);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("Not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(authService.mapToUserProfile(user)));
    }
}
