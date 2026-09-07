package com.phishshield.controller;

import com.phishshield.dto.request.EmailScanRequest;
import com.phishshield.dto.request.MessageScanRequest;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.service.URLScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scans")
@Tag(name = "Threat Scanning", description = "Public and authenticated URL, email, and message phishing scanners")
public class URLScanController {

    private final URLScanService urlScanService;

    public URLScanController(URLScanService urlScanService) {
        this.urlScanService = urlScanService;
    }

    @PostMapping("/url")
    @Operation(summary = "Scan a URL for phishing and security indicators (Public)")
    public ResponseEntity<ApiResponse<URLScanResponse>> scanUrl(
            @Valid @RequestBody URLScanRequest request,
            HttpServletRequest httpRequest) {
        URLScanResponse response = urlScanService.scanUrl(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("URL scan completed successfully", response));
    }

    @PostMapping("/email")
    @Operation(summary = "Analyze raw email text and embedded URLs for phishing vectors (Public)")
    public ResponseEntity<ApiResponse<URLScanResponse>> scanEmail(
            @Valid @RequestBody EmailScanRequest request,
            HttpServletRequest httpRequest) {
        URLScanResponse response = urlScanService.scanEmailContent(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Email scan completed successfully", response));
    }

    @PostMapping("/message")
    @Operation(summary = "Analyze SMS / WhatsApp text and embedded URLs for social engineering (Public)")
    public ResponseEntity<ApiResponse<URLScanResponse>> scanMessage(
            @Valid @RequestBody MessageScanRequest request,
            HttpServletRequest httpRequest) {
        URLScanResponse response = urlScanService.scanMessageContent(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Message scan completed successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed scan report by scan ID (Public)")
    public ResponseEntity<ApiResponse<URLScanResponse>> getScanById(@PathVariable Long id) {
        URLScanResponse response = urlScanService.getScanById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-scans")
    @Operation(summary = "Get current authenticated user's scan history (Authenticated)")
    public ResponseEntity<ApiResponse<Page<URLScanResponse>>> getMyScans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<URLScanResponse> responses = urlScanService.getUserScans(pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scan record from user's scan history (Privacy by Design)")
    public ResponseEntity<ApiResponse<Void>> deleteScan(@PathVariable Long id) {
        urlScanService.deleteScan(id);
        return ResponseEntity.ok(ApiResponse.success("Scan record deleted successfully", null));
    }
}
