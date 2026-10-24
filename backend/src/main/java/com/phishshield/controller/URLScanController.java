package com.phishshield.controller;

import com.phishshield.dto.request.EmailScanRequest;
import com.phishshield.dto.request.MessageScanRequest;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.MLServiceHealthDto;
import com.phishshield.dto.response.QRCodeAnalysisResponse;
import com.phishshield.dto.response.ScreenshotAnalysisResponse;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.service.MLServiceClient;
import com.phishshield.service.URLScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import com.phishshield.dto.response.UnifiedImageAnalysisResponse;
import com.phishshield.service.UnifiedImageAnalysisService;

@RestController
@RequestMapping("/api/scans")
@Tag(name = "Threat Scanning", description = "Public and authenticated URL, email, message, screenshot, and QR phishing scanners")
public class URLScanController {

    private final URLScanService urlScanService;
    private final MLServiceClient mlServiceClient;
    private final UnifiedImageAnalysisService unifiedImageAnalysisService;

    public URLScanController(URLScanService urlScanService,
                             MLServiceClient mlServiceClient,
                             UnifiedImageAnalysisService unifiedImageAnalysisService) {
        this.urlScanService = urlScanService;
        this.mlServiceClient = mlServiceClient;
        this.unifiedImageAnalysisService = unifiedImageAnalysisService;
    }

    @GetMapping("/ml-status")
    @Operation(summary = "Check status and health of the Python ML microservice (Public)")
    public ResponseEntity<ApiResponse<MLServiceHealthDto>> getMlStatus() {
        MLServiceHealthDto status = mlServiceClient.checkHealth();
        return ResponseEntity.ok(ApiResponse.success("ML service status", status));
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

    @PostMapping(value = "/screenshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze uploaded screenshot image and extracted text/URLs for threats (Public)")
    public ResponseEntity<ApiResponse<ScreenshotAnalysisResponse>> scanScreenshot(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            HttpServletRequest httpRequest) {
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, text, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Screenshot analysis completed successfully", response));
    }

    @PostMapping(value = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Decode and analyze QR code image destination URL for threats (Public)")
    public ResponseEntity<ApiResponse<QRCodeAnalysisResponse>> scanQRCode(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "decodedUrl", required = false) String decodedUrl,
            HttpServletRequest httpRequest) {
        QRCodeAnalysisResponse response = urlScanService.analyzeQRCode(file, decodedUrl, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("QR Code scan completed successfully", response));
    }

    @PostMapping(value = "/unified-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Unified multi-vector image analysis: QR decoding, OCR text, and candidate URL scanning (Public)")
    public ResponseEntity<ApiResponse<UnifiedImageAnalysisResponse>> analyzeUnifiedImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            HttpServletRequest httpRequest) {
        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, text, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Unified image analysis completed", response));
    }

    @GetMapping("/model/evaluations")
    @Operation(summary = "Get empirical model evaluation metrics from benchmark files (Public / Analyst)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getModelEvaluations() {
        Map<String, Object> evaluations = mlServiceClient.getModelEvaluations();
        return ResponseEntity.ok(ApiResponse.success("Model evaluation benchmarks", evaluations));
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
