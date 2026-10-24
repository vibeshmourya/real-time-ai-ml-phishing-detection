package com.phishshield.controller;

import com.phishshield.domain.IncidentSeverity;
import com.phishshield.domain.IncidentStatus;
import com.phishshield.dto.request.FraudIncidentRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.ComplaintDraftResponse;
import com.phishshield.dto.response.FraudIncidentDto;
import com.phishshield.exception.UnauthorizedException;
import com.phishshield.service.FraudIncidentService;
import com.phishshield.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Fraud & Safety Centre", description = "Track personal cyber fraud incidents, containment checklists, and formal complaint drafts")
public class FraudIncidentController {

    private final FraudIncidentService incidentService;

    public FraudIncidentController(FraudIncidentService incidentService) {
        this.incidentService = incidentService;
    }

    private Long getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required to manage fraud incidents."));
    }

    @GetMapping
    @Operation(summary = "Get user's logged fraud incidents")
    public ResponseEntity<ApiResponse<Page<FraudIncidentDto>>> getIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) IncidentStatus status) {
        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("incidentDateTime").descending());
        Page<FraudIncidentDto> result = incidentService.getUserIncidents(userId, pageable, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ANALYST')")
    @Operation(summary = "Admin/Analyst: View all platform incidents across citizens")
    public ResponseEntity<ApiResponse<Page<FraudIncidentDto>>> getAllIncidentsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FraudIncidentDto> result = incidentService.getAllIncidentsAdmin(pageable, status, severity);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details by ID")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> getIncidentById(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        FraudIncidentDto incident = incidentService.getIncidentById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(incident));
    }

    @PostMapping
    @Operation(summary = "Create a new fraud response incident")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> createIncident(
            @Valid @RequestBody FraudIncidentRequest request) {
        Long userId = getAuthenticatedUserId();
        FraudIncidentDto created = incidentService.createIncident(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Incident registered with containment checklist", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing fraud incident")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> updateIncident(
            @PathVariable Long id,
            @RequestBody FraudIncidentRequest request) {
        Long userId = getAuthenticatedUserId();
        FraudIncidentDto updated = incidentService.updateIncident(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Incident updated", updated));
    }

    @PatchMapping("/{id}/checklist")
    @Operation(summary = "Update checklist step completion state")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> updateChecklist(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        Long userId = getAuthenticatedUserId();
        String checklistJson = payload.get("checklistJson");
        FraudIncidentDto updated = incidentService.updateChecklist(id, checklistJson, userId);
        return ResponseEntity.ok(ApiResponse.success("Checklist progress updated", updated));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add a timestamped timeline note to an incident")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        Long userId = getAuthenticatedUserId();
        String note = payload.get("note");
        FraudIncidentDto updated = incidentService.addIncidentNote(id, note, userId);
        return ResponseEntity.ok(ApiResponse.success("Note recorded on timeline", updated));
    }

    @PatchMapping("/{id}/admin-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ANALYST')")
    @Operation(summary = "Admin/Analyst: Triage and update incident status and severity")
    public ResponseEntity<ApiResponse<FraudIncidentDto>> adminUpdateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String statusStr = payload.get("status");
        String severityStr = payload.get("severity");
        String adminNotes = payload.get("notes");

        IncidentStatus status = statusStr != null ? IncidentStatus.valueOf(statusStr) : null;
        IncidentSeverity severity = severityStr != null ? IncidentSeverity.valueOf(severityStr) : null;
        String adminUser = SecurityUtils.getCurrentUsername().orElse("system-admin");

        FraudIncidentDto updated = incidentService.adminUpdateStatus(id, status, severity, adminNotes, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Incident status updated by security team", updated));
    }

    @GetMapping("/{id}/complaint-draft")
    @Operation(summary = "Generate a formal complaint draft for cybercrime.gov.in / Bank")
    public ResponseEntity<ApiResponse<ComplaintDraftResponse>> generateComplaintDraft(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        ComplaintDraftResponse draft = incidentService.generateComplaintDraft(id, userId);
        return ResponseEntity.ok(ApiResponse.success(draft));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an incident record")
    public ResponseEntity<ApiResponse<Void>> deleteIncident(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        incidentService.deleteIncident(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Incident record deleted", null));
    }
}
