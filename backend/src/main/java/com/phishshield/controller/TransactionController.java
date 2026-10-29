package com.phishshield.controller;

import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionStatus;
import com.phishshield.dto.request.ImportConfirmRequest;
import com.phishshield.dto.request.ParseNotificationTextRequest;
import com.phishshield.dto.request.TransactionRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.SmsAnalysisResponse;
import com.phishshield.dto.response.StatementPreviewResponseDto;
import com.phishshield.dto.response.TransactionDto;
import com.phishshield.dto.response.TransactionSummaryDto;
import com.phishshield.exception.UnauthorizedException;
import com.phishshield.service.SmsFraudDetectionService;
import com.phishshield.service.TransactionService;
import com.phishshield.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Personal Finance & Transactions", description = "Monitor authorized personal financial records, import statements, detect transaction anomalies")
public class TransactionController {

    private final TransactionService transactionService;
    private final SmsFraudDetectionService smsFraudDetectionService;

    public TransactionController(TransactionService transactionService,
                                 SmsFraudDetectionService smsFraudDetectionService) {
        this.transactionService = transactionService;
        this.smsFraudDetectionService = smsFraudDetectionService;
    }

    private Long getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required to access personal finance records."));
    }

    @GetMapping
    @Operation(summary = "Get user transactions with optional filters and pagination")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) TransactionDirection direction) {
        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDateTime").descending());
        Page<TransactionDto> result = transactionService.getUserTransactions(userId, pageable, status, category, direction);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get personal finance dashboard summary with personalized baseline metrics")
    public ResponseEntity<ApiResponse<TransactionSummaryDto>> getSummary() {
        Long userId = getAuthenticatedUserId();
        TransactionSummaryDto summary = transactionService.getTransactionSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping
    @Operation(summary = "Manually record a financial transaction")
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getAuthenticatedUserId();
        TransactionDto created = transactionService.createManualTransaction(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction recorded successfully", created));
    }

    @PostMapping("/manual")
    @Operation(summary = "Alias for manual transaction creation")
    public ResponseEntity<ApiResponse<TransactionDto>> createManualAlias(
            @Valid @RequestBody TransactionRequest request) {
        return createTransaction(request);
    }

    // ==========================================
    // Unified Statement Import Endpoints (Phase 2)
    // ==========================================

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview transactions from CSV, Excel (.xlsx/.xls), or PDF statements before committing")
    public ResponseEntity<ApiResponse<StatementPreviewResponseDto>> previewImport(
            @RequestParam("file") MultipartFile file) {
        Long userId = getAuthenticatedUserId();
        StatementPreviewResponseDto preview = transactionService.previewImport(file, userId);
        return ResponseEntity.ok(ApiResponse.success("Statement preview generated successfully", preview));
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "Commit verified transaction rows from a statement preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmImport(
            @RequestBody ImportConfirmRequest request) {
        Long userId = getAuthenticatedUserId();
        Map<String, Object> result = transactionService.confirmImport(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Transactions committed to your ledger", result));
    }

    @PostMapping(value = "/import/unified", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Direct import of statement file (CSV, Excel, or PDF)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importUnified(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmAmbiguous", defaultValue = "false") boolean confirmAmbiguous) {
        Long userId = getAuthenticatedUserId();
        Map<String, Object> result = transactionService.importUnifiedTransactions(file, confirmAmbiguous, userId);
        return ResponseEntity.ok(ApiResponse.success("Statement processed successfully", result));
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import statement records from CSV (backward-compatible)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importCsv(
            @RequestParam("file") MultipartFile file) {
        return importUnified(file, true);
    }

    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV statement alias (backward-compatible)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadCsvAlias(
            @RequestParam("file") MultipartFile file) {
        return importUnified(file, true);
    }

    // ==========================================
    // Notification & SMS Fraud Endpoints (Phase 5)
    // ==========================================

    @PostMapping("/parse-text")
    @Operation(summary = "Parse transaction notification SMS or email text")
    public ResponseEntity<ApiResponse<TransactionDto>> parseText(
            @Valid @RequestBody ParseNotificationTextRequest request) {
        Long userId = getAuthenticatedUserId();
        TransactionDto parsed = transactionService.parseAndSaveNotificationText(request.getText(), userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction extracted from notification text", parsed));
    }

    @PostMapping("/parse-notification")
    @Operation(summary = "Alias for parsing notification text")
    public ResponseEntity<ApiResponse<TransactionDto>> parseNotificationAlias(
            @Valid @RequestBody ParseNotificationTextRequest request) {
        return parseText(request);
    }

    @PostMapping("/analyze-sms")
    @Operation(summary = "Analyze SMS text for banking fraud, phishing lures, and transaction anomalies (Phase 5)")
    public ResponseEntity<ApiResponse<SmsAnalysisResponse>> analyzeSms(
            @Valid @RequestBody ParseNotificationTextRequest request) {
        Long userId = getAuthenticatedUserId();
        SmsAnalysisResponse response = smsFraudDetectionService.analyzeSms(request.getText(), userId);
        return ResponseEntity.ok(ApiResponse.success("SMS fraud analysis completed", response));
    }

    // ==========================================
    // Transaction Verification & Maintenance
    // ==========================================

    @PatchMapping("/{id}/verify")
    @Operation(summary = "Confirm/verify a transaction that was flagged for review")
    public ResponseEntity<ApiResponse<TransactionDto>> verifyTransaction(
            @PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        TransactionDto verified = transactionService.verifyTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction marked as confirmed", verified));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "POST alias for confirming a flagged transaction")
    public ResponseEntity<ApiResponse<TransactionDto>> verifyTransactionPostAlias(
            @PathVariable Long id) {
        return verifyTransaction(id);
    }

    @PatchMapping("/{id}/category")
    @Operation(summary = "Update transaction categorization")
    public ResponseEntity<ApiResponse<TransactionDto>> updateCategory(
            @PathVariable Long id,
            @RequestParam TransactionCategory category) {
        Long userId = getAuthenticatedUserId();
        TransactionDto updated = transactionService.updateCategory(id, category, userId);
        return ResponseEntity.ok(ApiResponse.success("Category updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction record")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export all user transaction records to CSV")
    public ResponseEntity<String> exportCsv() {
        Long userId = getAuthenticatedUserId();
        String csvContent = transactionService.exportCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prahar_transactions_" + userId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @GetMapping("/export-csv")
    @Operation(summary = "Alias for exporting user transaction records to CSV")
    public ResponseEntity<String> exportCsvAlias() {
        return exportCsv();
    }
}
