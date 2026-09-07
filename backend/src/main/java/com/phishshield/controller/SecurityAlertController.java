package com.phishshield.controller;

import com.phishshield.dto.request.CreateSecurityAlertRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.SecurityAlertResponse;
import com.phishshield.service.SecurityAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Security Advisories", description = "Public alerts and emerging cyber threat advisories")
public class SecurityAlertController {

    private final SecurityAlertService securityAlertService;

    public SecurityAlertController(SecurityAlertService securityAlertService) {
        this.securityAlertService = securityAlertService;
    }

    @GetMapping("/active")
    @Operation(summary = "Get active emerging threat alerts and advisories (Public)")
    public ResponseEntity<ApiResponse<List<SecurityAlertResponse>>> getActiveAlerts() {
        List<SecurityAlertResponse> alerts = securityAlertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create and broadcast a new security advisory (Admin Only)")
    public ResponseEntity<ApiResponse<SecurityAlertResponse>> createAlert(
            @Valid @RequestBody CreateSecurityAlertRequest request) {
        SecurityAlertResponse response = securityAlertService.createAlert(request);
        return new ResponseEntity<>(ApiResponse.success("Security advisory broadcasted successfully", response), HttpStatus.CREATED);
    }
}
