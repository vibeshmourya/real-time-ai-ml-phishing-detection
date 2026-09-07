package com.phishshield.controller;

import com.phishshield.dto.response.AdminDashboardStatsResponse;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.DashboardStatsResponse;
import com.phishshield.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboards & Analytics", description = "User personal security statistics and Admin threat intelligence telemetry")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/user")
    @Operation(summary = "Get user personal security stats and recent scans (Authenticated)")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getUserDashboard() {
        DashboardStatsResponse response = dashboardService.getUserDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ANALYST')")
    @Operation(summary = "Get global threat intelligence metrics, risk distribution, and reports queue (Admin/Analyst Only)")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getAdminDashboard() {
        AdminDashboardStatsResponse response = dashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
