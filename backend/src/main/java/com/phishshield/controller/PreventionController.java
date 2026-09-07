package com.phishshield.controller;

import com.phishshield.domain.ThreatCategory;
import com.phishshield.dto.request.CreatePreventionContentRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.PreventionContentResponse;
import com.phishshield.service.PreventionContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prevention")
@Tag(name = "Prevention & Cyber Awareness", description = "Targeted awareness guides, videos, audio warnings, and checklists")
public class PreventionController {

    private final PreventionContentService preventionContentService;

    public PreventionController(PreventionContentService preventionContentService) {
        this.preventionContentService = preventionContentService;
    }

    @GetMapping("/{category}")
    @Operation(summary = "Get contextual safety guide by threat category (Public)")
    public ResponseEntity<ApiResponse<PreventionContentResponse>> getGuide(
            @PathVariable ThreatCategory category,
            @RequestParam(defaultValue = "en") String lang) {
        PreventionContentResponse response = preventionContentService.getPreventionGuide(category, lang);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get list of all supported threat categories (Public)")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = preventionContentService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or update educational prevention content (Admin Only)")
    public ResponseEntity<ApiResponse<PreventionContentResponse>> createOrUpdateGuide(
            @Valid @RequestBody CreatePreventionContentRequest request) {
        PreventionContentResponse response = preventionContentService.createOrUpdatePreventionContent(request);
        return new ResponseEntity<>(ApiResponse.success("Prevention guide saved successfully", response), HttpStatus.CREATED);
    }
}
