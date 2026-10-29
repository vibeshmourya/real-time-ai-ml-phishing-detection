package com.phishshield.service;

import com.phishshield.domain.*;
import com.phishshield.dto.request.ImportConfirmRequest;
import com.phishshield.dto.request.TransactionRequest;
import com.phishshield.dto.response.*;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UnifiedStatementParser statementParser;
    private final PersonalizedRiskAssessmentService riskAssessmentService;

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:ending|no|[*x]+)?\\s*([0-9x*]{3,6})");
    private static final Pattern UPI_VPA_PATTERN = Pattern.compile("(?i)(?:to|vpa|at)\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|rrn|upi\\s*ref|txn\\s*id)\\s*[:#-]?\\s*([a-zA-Z0-9]{6,22})");

    public TransactionService(FinancialTransactionRepository transactionRepository,
                              UserRepository userRepository,
                              UnifiedStatementParser statementParser,
                              PersonalizedRiskAssessmentService riskAssessmentService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.statementParser = statementParser;
        this.riskAssessmentService = riskAssessmentService;
    }

    @Transactional
    public TransactionDto createManualTransaction(TransactionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDateTime txnDate = request.getTransactionDateTime() != null 
                ? request.getTransactionDateTime() 
                : LocalDateTime.now();

        TransactionCategory category = request.getCategory() != null && request.getCategory() != TransactionCategory.OTHER
                ? request.getCategory()
                : statementParser.inferCategory(request.getMerchant());

        String fingerprint = generateFingerprint(userId, txnDate, request.getAmount(), 
                request.getMerchant(), request.getTransactionReference());

        if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fingerprint)) {
            throw new BadRequestException("A duplicate transaction with the same date, amount, and reference already exists.");
        }

        FinancialTransaction txn = FinancialTransaction.builder()
                .user(user)
                .transactionDateTime(txnDate)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .direction(request.getDirection())
                .transactionType(request.getTransactionType() != null ? request.getTransactionType() : TransactionType.UPI)
                .category(category)
                .sourceInstitution(request.getSourceInstitution())
                .maskedAccount(request.getMaskedAccount())
                .merchant(request.getMerchant())
                .transactionReference(request.getTransactionReference())
                .importSource(TransactionImportSource.MANUAL)
                .status(TransactionStatus.CONFIRMED)
                .notes(request.getNotes())
                .duplicateFingerprint(fingerprint)
                .build();

        // Personalized Anomaly Assessment (real-time manual entry)
        PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(txn, userId, false);
        txn.setStatus(assessment.getSuggestedStatus());
        if (assessment.getSuggestedStatus() != TransactionStatus.CONFIRMED) {
            txn.setFlagReason(assessment.getExplanation());
        }

        FinancialTransaction saved = transactionRepository.save(txn);
        TransactionDto dto = TransactionDto.fromEntity(saved);
        dto.setRiskScore(assessment.getRiskScore());
        dto.setSeverity(assessment.getSeverity());
        dto.setRecommendation(assessment.getRecommendation());
        return dto;
    }

    // ==========================================
    // Unified Statement Import Workflow (Phase 2)
    // ==========================================

    @Transactional(readOnly = true)
    public StatementPreviewResponseDto previewImport(MultipartFile file, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        StatementPreviewResponseDto preview = statementParser.parseStatementPreview(file);

        int duplicateCount = 0;
        for (StatementPreviewRowDto row : preview.getPreviewRows()) {
            if (row.isValid()) {
                String fp = generateFingerprint(userId, row.getDate(), row.getAmount(), row.getDescription(), row.getReference());
                if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fp)) {
                    row.setDuplicate(true);
                    duplicateCount++;
                }
            }
        }
        preview.setDuplicateCount(duplicateCount);
        return preview;
    }

    @Transactional
    public Map<String, Object> confirmImport(ImportConfirmRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request == null || request.getRows() == null || request.getRows().isEmpty()) {
            throw new BadRequestException("No transaction rows selected for commit.");
        }

        int importedCount = 0;
        int duplicateSkippedCount = 0;
        int invalidCount = 0;
        int flaggedCount = 0;
        List<String> errors = new ArrayList<>();

        List<FinancialTransaction> toSave = new ArrayList<>();

        for (StatementPreviewRowDto row : request.getRows()) {
            if (!row.isValid()) {
                invalidCount++;
                continue;
            }

            try {
                LocalDateTime txnDate = row.getDate() != null ? row.getDate() : LocalDateTime.now();
                BigDecimal amount = row.getAmount();
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    invalidCount++;
                    continue;
                }

                String fingerprint = generateFingerprint(userId, txnDate, amount, row.getDescription(), row.getReference());
                if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fingerprint)) {
                    duplicateSkippedCount++;
                    continue;
                }

                TransactionDirection direction = "INCOME".equalsIgnoreCase(row.getDirection())
                        ? TransactionDirection.INCOME
                        : TransactionDirection.EXPENSE;

                TransactionCategory category = TransactionCategory.OTHER;
                if (row.getCategory() != null) {
                    try {
                        category = TransactionCategory.valueOf(row.getCategory());
                    } catch (Exception ignored) {}
                }

                TransactionType type = TransactionType.UPI;
                if (row.getTransactionType() != null) {
                    try {
                        type = TransactionType.valueOf(row.getTransactionType());
                    } catch (Exception ignored) {}
                }

                String sourceFileName = request.getFileName() != null ? request.getFileName() : "statement";
                TransactionImportSource importSource = sourceFileName.toLowerCase().endsWith(".pdf")
                        ? TransactionImportSource.STATEMENT
                        : sourceFileName.toLowerCase().endsWith(".xlsx")
                        ? TransactionImportSource.STATEMENT
                        : TransactionImportSource.CSV;

                FinancialTransaction txn = FinancialTransaction.builder()
                        .user(user)
                        .transactionDateTime(txnDate)
                        .amount(amount)
                        .currency("INR")
                        .direction(direction)
                        .transactionType(type)
                        .category(category)
                        .merchant(row.getDescription())
                        .transactionReference(row.getReference())
                        .maskedAccount(row.getAccount())
                        .importSource(importSource)
                        .status(TransactionStatus.CONFIRMED)
                        .notes("Imported from statement: " + sourceFileName)
                        .duplicateFingerprint(fingerprint)
                        .build();

                // Personalized Anomaly Detection (batch historical statement import suppresses real-time alerts)
                PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(txn, userId, true);
                txn.setStatus(assessment.getSuggestedStatus());
                if (assessment.getSuggestedStatus() != TransactionStatus.CONFIRMED) {
                    txn.setFlagReason(assessment.getExplanation());
                    flaggedCount++;
                }

                toSave.add(txn);
                importedCount++;
            } catch (Exception ex) {
                invalidCount++;
                if (errors.size() < 5) {
                    errors.add("Row " + row.getRowNumber() + ": " + ex.getMessage());
                }
            }
        }

        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        result.put("duplicateSkippedCount", duplicateSkippedCount);
        result.put("invalidCount", invalidCount);
        result.put("flaggedCount", flaggedCount);
        result.put("errors", errors);
        result.put("message", String.format("Successfully imported %d transactions. %d duplicates skipped. %d flagged for review.",
                importedCount, duplicateSkippedCount, flaggedCount));

        return result;
    }

    @Transactional
    public Map<String, Object> importUnifiedTransactions(MultipartFile file, boolean confirmAmbiguous, Long userId) {
        StatementPreviewResponseDto preview = previewImport(file, userId);

        if (preview.isAmbiguous() && !confirmAmbiguous) {
            Map<String, Object> ambigResult = new HashMap<>();
            ambigResult.put("requiresConfirmation", true);
            ambigResult.put("preview", preview);
            ambigResult.put("message", "PDF/Statement structure contains ambiguous columns. User confirmation required.");
            return ambigResult;
        }

        ImportConfirmRequest confirmRequest = new ImportConfirmRequest();
        confirmRequest.setRows(preview.getPreviewRows());
        confirmRequest.setFileName(file.getOriginalFilename());
        confirmRequest.setConfirmAmbiguous(confirmAmbiguous);

        return confirmImport(confirmRequest, userId);
    }

    @Transactional
    public Map<String, Object> importCsvTransactions(MultipartFile file, Long userId) {
        return importUnifiedTransactions(file, true, userId);
    }

    // ==========================================
    // Notification & SMS Parser (Phase 5)
    // ==========================================

    @Transactional
    public TransactionDto parseAndSaveNotificationText(String text, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (text == null || text.isBlank()) {
            throw new BadRequestException("Notification text cannot be empty");
        }

        String lower = text.toLowerCase();
        TransactionDirection direction = (lower.contains("credited") || lower.contains("received") || lower.contains("refund"))
                ? TransactionDirection.INCOME
                : TransactionDirection.EXPENSE;

        BigDecimal amount = BigDecimal.valueOf(100.00);
        Matcher amtMatcher = AMOUNT_PATTERN.matcher(text);
        if (amtMatcher.find()) {
            try {
                amount = new BigDecimal(amtMatcher.group(1).replace(",", ""));
            } catch (Exception ignored) {}
        }

        String maskedAccount = null;
        Matcher acctMatcher = ACCOUNT_PATTERN.matcher(text);
        if (acctMatcher.find()) {
            maskedAccount = acctMatcher.group(1);
        }

        String merchant = "Unknown Counterparty";
        Matcher vpaMatcher = UPI_VPA_PATTERN.matcher(text);
        if (vpaMatcher.find()) {
            merchant = vpaMatcher.group(1);
        } else {
            Matcher atMatcher = Pattern.compile("(?i)(?:at|to|info:)\\s+([A-Za-z0-9 &._-]{3,30})").matcher(text);
            if (atMatcher.find()) {
                merchant = atMatcher.group(1).trim();
            }
        }

        String reference = null;
        Matcher refMatcher = REF_PATTERN.matcher(text);
        if (refMatcher.find()) {
            reference = refMatcher.group(1);
        }

        TransactionType type = statementParser.inferType(text);
        TransactionCategory category = statementParser.inferCategory(merchant + " " + text);
        LocalDateTime now = LocalDateTime.now();

        String fingerprint = generateFingerprint(userId, now, amount, merchant, reference);
        if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fingerprint)) {
            throw new BadRequestException("This transaction notification has already been recorded in your ledger.");
        }

        FinancialTransaction txn = FinancialTransaction.builder()
                .user(user)
                .transactionDateTime(now)
                .amount(amount)
                .currency("INR")
                .direction(direction)
                .transactionType(type)
                .category(category)
                .sourceInstitution(inferBank(text))
                .maskedAccount(maskedAccount)
                .merchant(merchant)
                .transactionReference(reference)
                .importSource(TransactionImportSource.NOTIFICATION_TEXT)
                .status(TransactionStatus.CONFIRMED)
                .notes("Extracted from notification: \"" + (text.length() > 60 ? text.substring(0, 60) + "..." : text) + "\"")
                .duplicateFingerprint(fingerprint)
                .build();

        // Personalized Anomaly Assessment (real-time notification alert)
        PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(txn, userId, false);
        txn.setStatus(assessment.getSuggestedStatus());
        if (assessment.getSuggestedStatus() != TransactionStatus.CONFIRMED) {
            txn.setFlagReason(assessment.getExplanation());
        }

        FinancialTransaction saved = transactionRepository.save(txn);
        TransactionDto dto = TransactionDto.fromEntity(saved);
        dto.setRiskScore(assessment.getRiskScore());
        dto.setSeverity(assessment.getSeverity());
        dto.setRecommendation(assessment.getRecommendation());
        return dto;
    }

    public Page<TransactionDto> getUserTransactions(Long userId, Pageable pageable, 
                                                    TransactionStatus status, 
                                                    TransactionCategory category, 
                                                    TransactionDirection direction) {
        Page<FinancialTransaction> page;
        if (status != null) {
            page = transactionRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (category != null) {
            page = transactionRepository.findByUserIdAndCategory(userId, category, pageable);
        } else {
            page = transactionRepository.findByUserId(userId, pageable);
        }

        return page.map(t -> {
            TransactionDto dto = TransactionDto.fromEntity(t);
            PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(t, userId, true);
            dto.setRiskScore(assessment.getRiskScore());
            dto.setSeverity(assessment.getSeverity());
            dto.setRecommendation(assessment.getRecommendation());
            return dto;
        });
    }

    public TransactionSummaryDto getTransactionSummary(Long userId) {
        BigDecimal totalIn = transactionRepository.sumAmountByUserIdAndDirection(userId, TransactionDirection.INCOME);
        BigDecimal totalOut = transactionRepository.sumAmountByUserIdAndDirection(userId, TransactionDirection.EXPENSE);
        BigDecimal netFlow = totalIn.subtract(totalOut);

        long flagged = transactionRepository.countFlaggedByUserId(userId);
        long review = transactionRepository.countNeedsReviewByUserId(userId);

        List<Object[]> breakdownList = transactionRepository.getCategoryExpenseBreakdownByUserId(userId);
        Map<String, BigDecimal> categoryExpenses = new HashMap<>();
        for (Object[] row : breakdownList) {
            TransactionCategory cat = (TransactionCategory) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            categoryExpenses.put(cat.name(), amt);
        }

        List<FinancialTransaction> recent = transactionRepository.findTop10ByUserIdOrderByTransactionDateTimeDesc(userId);
        List<TransactionDto> recentDtos = recent.stream().map(t -> {
            TransactionDto dto = TransactionDto.fromEntity(t);
            PersonalizedRiskAssessment assessment = riskAssessmentService.evaluateTransaction(t, userId, true);
            dto.setRiskScore(assessment.getRiskScore());
            dto.setSeverity(assessment.getSeverity());
            dto.setRecommendation(assessment.getRecommendation());
            return dto;
        }).collect(Collectors.toList());

        TransactionSummaryDto summary = new TransactionSummaryDto();
        summary.setTotalInflows(totalIn);
        summary.setTotalOutflows(totalOut);
        summary.setNetCashFlow(netFlow);
        summary.setFlaggedCount(flagged);
        summary.setNeedsReviewCount(review);
        summary.setCategoryExpenses(categoryExpenses);
        summary.setRecentTransactions(recentDtos);
        summary.setCoverageNote("Records based on user-entered transactions, CSV/Excel/PDF statements, and authorized notifications. Prahar does not connect directly to bank servers without authorization.");

        // Attach Personalized Baseline Metrics
        PersonalizedRiskAssessmentService.BaselineProfile baseline = riskAssessmentService.computeUserProfile(userId);
        summary.setPersonalizationEstablished(baseline.isEstablished());
        summary.setBaselineMedian(baseline.getMedian());
        summary.setBaselineMad(baseline.getMad());
        summary.setHistoryCount(baseline.getSampleSize());
        summary.setRecurringExpensesCount(baseline.getRecurringPatterns().size());
        summary.setBaselineStatusMessage(baseline.isEstablished()
                ? String.format("Personalized baseline active (median: ₹%s, dispersion: ₹%s, %d recurring patterns recognized).",
                    baseline.getMedian().toPlainString(), baseline.getMad().toPlainString(), baseline.getRecurringPatterns().size())
                : "Personalization baseline is establishing (requires at least 5 transactions). Conservative safety rules active.");

        return summary;
    }

    @Transactional
    public TransactionDto updateCategory(Long transactionId, TransactionCategory newCategory, Long userId) {
        FinancialTransaction txn = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        txn.setCategory(newCategory);
        return TransactionDto.fromEntity(transactionRepository.save(txn));
    }

    @Transactional
    public TransactionDto verifyTransaction(Long transactionId, Long userId) {
        FinancialTransaction txn = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        txn.setStatus(TransactionStatus.CONFIRMED);
        txn.setFlagReason(null);
        return TransactionDto.fromEntity(transactionRepository.save(txn));
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        FinancialTransaction txn = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        transactionRepository.delete(txn);
    }

    public String exportCsv(Long userId) {
        List<FinancialTransaction> txns = transactionRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Date,Amount,Currency,Direction,Type,Category,Merchant,Reference,Status,Notes\n");
        for (FinancialTransaction t : txns) {
            sb.append(t.getId()).append(",")
                    .append(t.getTransactionDateTime()).append(",")
                    .append(t.getAmount()).append(",")
                    .append(t.getCurrency()).append(",")
                    .append(t.getDirection()).append(",")
                    .append(t.getTransactionType()).append(",")
                    .append(t.getCategory()).append(",")
                    .append("\"").append(t.getMerchant() != null ? t.getMerchant().replace("\"", "\"\"") : "").append("\",")
                    .append(t.getTransactionReference() != null ? t.getTransactionReference() : "").append(",")
                    .append(t.getStatus()).append(",")
                    .append("\"").append(t.getNotes() != null ? t.getNotes().replace("\"", "\"\"") : "").append("\"\n");
        }
        return sb.toString();
    }

    private String generateFingerprint(Long userId, LocalDateTime dt, BigDecimal amount, String merchant, String ref) {
        String raw = userId + "_" + (dt != null ? dt.toLocalDate() : "") + "_" 
                + (amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : "") + "_" 
                + (merchant != null ? merchant.trim().toLowerCase() : "") + "_"
                + (ref != null ? ref.trim() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private String inferBank(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("sbi")) return "State Bank of India";
        if (lower.contains("hdfc")) return "HDFC Bank";
        if (lower.contains("icici")) return "ICICI Bank";
        if (lower.contains("axis")) return "Axis Bank";
        if (lower.contains("kotak")) return "Kotak Mahindra Bank";
        if (lower.contains("pnb")) return "Punjab National Bank";
        if (lower.contains("bob") || lower.contains("baroda")) return "Bank of Baroda";
        return "Bank Account";
    }
}
