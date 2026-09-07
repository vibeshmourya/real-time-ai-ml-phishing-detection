package com.phishshield.controller;

import com.phishshield.domain.ReportStatus;
import com.phishshield.dto.request.ReportStatusUpdateRequest;
import com.phishshield.dto.request.UserReportRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.UserReportResponse;
import com.phishshield.service.UserReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Threat Reporting", description = "Crowdsourced threat reporting and admin triage workflow")
public class ReportController {

    private final UserReportService userReportService;

    public ReportController(UserReportService userReportService) {
        this.userReportService = userReportService;
    }

    @PostMapping
    @Operation(summary = "Submit a suspicious URL or phishing threat report (Public)")
    public ResponseEntity<ApiResponse<UserReportResponse>> submitReport(@Valid @RequestBody UserReportRequest request) {
        UserReportResponse response = userReportService.submitReport(request);
        return new ResponseEntity<>(ApiResponse.success("Report submitted successfully. Thank you for making the internet safer.", response), HttpStatus.CREATED);
    }

    @GetMapping("/my-reports")
    @Operation(summary = "Get reports submitted by current user (Authenticated)")
    public ResponseEntity<ApiResponse<Page<UserReportResponse>>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserReportResponse> responses = userReportService.getUserReports(pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ANALYST')")
    @Operation(summary = "Get all submitted threat reports (Admin/Analyst Only)")
    public ResponseEntity<ApiResponse<Page<UserReportResponse>>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserReportResponse> responses = userReportService.getAllReports(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ANALYST')")
    @Operation(summary = "Update threat report verification status (Admin/Analyst Only)")
    public ResponseEntity<ApiResponse<UserReportResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        UserReportResponse response = userReportService.updateReportStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Report status updated", response));
    }
}
