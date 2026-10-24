package com.phishshield.service;

import com.phishshield.domain.*;
import com.phishshield.dto.request.TransactionRequest;
import com.phishshield.dto.response.TransactionDto;
import com.phishshield.dto.response.TransactionSummaryDto;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:ending|no|[*x]+)?\\s*([0-9x*]{3,6})");
    private static final Pattern UPI_VPA_PATTERN = Pattern.compile("(?i)(?:to|vpa|at)\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|rrn|upi\\s*ref|txn\\s*id)\\s*[:#-]?\\s*([a-zA-Z0-9]{6,22})");

    public TransactionService(FinancialTransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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
                : autoCategorize(request.getMerchant(), request.getNotes());

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

        // Run anomaly detection
        evaluateAnomaly(txn, userId);

        FinancialTransaction saved = transactionRepository.save(txn);
        return TransactionDto.fromEntity(saved);
    }

    @Transactional
    public Map<String, Object> importCsvTransactions(MultipartFile file, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded CSV file is empty");
        }

        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<String> importErrors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BadRequestException("CSV file contains no headers");
            }

            String[] headers = headerLine.toLowerCase().split(",");
            int dateIdx = findColumnIndex(headers, "date", "txn date", "transaction date");
            int descIdx = findColumnIndex(headers, "description", "narration", "particulars", "merchant");
            int debitIdx = findColumnIndex(headers, "debit", "withdrawal", "spent");
            int creditIdx = findColumnIndex(headers, "credit", "deposit");
            int amountIdx = findColumnIndex(headers, "amount");
            int refIdx = findColumnIndex(headers, "ref", "reference", "chq/ref no", "txn id");

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    
                    // Parse date
                    LocalDateTime txnDate = parseCsvDate(cols, dateIdx);
                    String description = descIdx >= 0 && descIdx < cols.length ? cleanCol(cols[descIdx]) : "CSV Import";
                    String reference = refIdx >= 0 && refIdx < cols.length ? cleanCol(cols[refIdx]) : null;

                    BigDecimal debitAmount = parseAmount(cols, debitIdx);
                    BigDecimal creditAmount = parseAmount(cols, creditIdx);

                    TransactionDirection direction;
                    BigDecimal amount;

                    if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
                        direction = TransactionDirection.EXPENSE;
                        amount = debitAmount;
                    } else if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
                        direction = TransactionDirection.INCOME;
                        amount = creditAmount;
                    } else if (amountIdx >= 0 && amountIdx < cols.length) {
                        BigDecimal rawAmt = parseAmount(cols, amountIdx);
                        if (rawAmt == null) continue;
                        if (rawAmt.compareTo(BigDecimal.ZERO) < 0) {
                            direction = TransactionDirection.EXPENSE;
                            amount = rawAmt.abs();
                        } else {
                            direction = TransactionDirection.INCOME;
                            amount = rawAmt;
                        }
                    } else {
                        continue;
                    }

                    String fingerprint = generateFingerprint(userId, txnDate, amount, description, reference);
                    if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fingerprint)) {
                        duplicateCount++;
                        continue;
                    }

                    TransactionCategory category = autoCategorize(description, null);
                    TransactionType type = inferTransactionType(description);

                    FinancialTransaction txn = FinancialTransaction.builder()
                            .user(user)
                            .transactionDateTime(txnDate)
                            .amount(amount)
                            .currency("INR")
                            .direction(direction)
                            .transactionType(type)
                            .category(category)
                            .merchant(description)
                            .transactionReference(reference)
                            .importSource(TransactionImportSource.CSV)
                            .status(TransactionStatus.CONFIRMED)
                            .notes("Imported from CSV file: " + file.getOriginalFilename())
                            .duplicateFingerprint(fingerprint)
                            .build();

                    evaluateAnomaly(txn, userId);
                    transactionRepository.save(txn);
                    importedCount++;
                } catch (Exception ex) {
                    errorCount++;
                    if (importErrors.size() < 5) {
                        importErrors.add("Row " + rowNum + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage(), e);
            throw new BadRequestException("CSV parsing failed: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        result.put("duplicateSkippedCount", duplicateCount);
        result.put("errorCount", errorCount);
        result.put("errors", importErrors);
        return result;
    }

    @Transactional
    public TransactionDto parseAndSaveNotificationText(String text, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (text == null || text.isBlank()) {
            throw new BadRequestException("Notification text cannot be empty");
        }

        String lower = text.toLowerCase();
        TransactionDirection direction;
        if (lower.contains("debited") || lower.contains("spent") || lower.contains("paid") || lower.contains("sent") || lower.contains("withdrawn")) {
            direction = TransactionDirection.EXPENSE;
        } else if (lower.contains("credited") || lower.contains("received") || lower.contains("refund") || lower.contains("deposited")) {
            direction = TransactionDirection.INCOME;
        } else {
            direction = TransactionDirection.EXPENSE; // Default conservative assumption
        }

        // Extract Amount
        BigDecimal amount = BigDecimal.valueOf(100.00); // Default placeholder if unfound
        Matcher amtMatcher = AMOUNT_PATTERN.matcher(text);
        if (amtMatcher.find()) {
            String raw = amtMatcher.group(1).replace(",", "");
            try {
                amount = new BigDecimal(raw);
            } catch (Exception ignored) {}
        }

        // Extract Account
        String maskedAccount = null;
        Matcher acctMatcher = ACCOUNT_PATTERN.matcher(text);
        if (acctMatcher.find()) {
            maskedAccount = acctMatcher.group(1);
        }

        // Extract Merchant / Counterparty
        String merchant = "Unknown Counterparty";
        Matcher vpaMatcher = UPI_VPA_PATTERN.matcher(text);
        if (vpaMatcher.find()) {
            merchant = vpaMatcher.group(1);
        } else {
            // Find "at [Merchant]" or "to [Merchant]"
            Matcher atMatcher = Pattern.compile("(?i)(?:at|to|info:)\\s+([A-Za-z0-9 &._-]{3,30})").matcher(text);
            if (atMatcher.find()) {
                merchant = atMatcher.group(1).trim();
            }
        }

        // Extract Reference
        String reference = null;
        Matcher refMatcher = REF_PATTERN.matcher(text);
        if (refMatcher.find()) {
            reference = refMatcher.group(1);
        }

        TransactionType type = inferTransactionType(text);
        TransactionCategory category = autoCategorize(merchant, text);
        LocalDateTime now = LocalDateTime.now();

        String fingerprint = generateFingerprint(userId, now, amount, merchant, reference);
        if (transactionRepository.existsByUserIdAndDuplicateFingerprint(userId, fingerprint)) {
            throw new BadRequestException("This transaction notification has already been recorded.");
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
                .notes("Extracted from notification text: \"" + (text.length() > 80 ? text.substring(0, 80) + "..." : text) + "\"")
                .duplicateFingerprint(fingerprint)
                .build();

        evaluateAnomaly(txn, userId);
        FinancialTransaction saved = transactionRepository.save(txn);
        return TransactionDto.fromEntity(saved);
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

        return page.map(TransactionDto::fromEntity);
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
        List<TransactionDto> recentDtos = recent.stream().map(TransactionDto::fromEntity).collect(Collectors.toList());

        TransactionSummaryDto summary = new TransactionSummaryDto();
        summary.setTotalInflows(totalIn);
        summary.setTotalOutflows(totalOut);
        summary.setNetCashFlow(netFlow);
        summary.setFlaggedCount(flagged);
        summary.setNeedsReviewCount(review);
        summary.setCategoryExpenses(categoryExpenses);
        summary.setRecentTransactions(recentDtos);
        summary.setCoverageNote("Records based on user-entered transactions, CSV statements, and notification entries. Prahar does not connect directly to bank servers without authorization.");

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

    // Helper: Categorization Engine
    public TransactionCategory autoCategorize(String merchant, String notes) {
        String combined = ((merchant != null ? merchant : "") + " " + (notes != null ? notes : "")).toLowerCase();
        if (combined.matches(".*(zomato|swiggy|mcdonald|kfc|starbucks|burger|cafe|restaurant|hotel|food|canteen|bakes).*")) {
            return TransactionCategory.FOOD;
        }
        if (combined.matches(".*(amazon|flipkart|myntra|meesho|nykaa|zara|h&m|mart|supermarket|dmart|shopping|retail|store).*")) {
            return TransactionCategory.SHOPPING;
        }
        if (combined.matches(".*(electricity|bescom|tneb|airtel|jio|vi|recharge|broadband|water|gas|bill|utility|cylinder|dth).*")) {
            return TransactionCategory.BILLS;
        }
        if (combined.matches(".*(uber|ola|rapido|irctc|indigo|flight|train|metro|rail|fuel|petrol|diesel|toll|fastag|bus).*")) {
            return TransactionCategory.TRAVEL;
        }
        if (combined.matches(".*(atm|cash wdl|cash withdrawal|cash).*")) {
            return TransactionCategory.CASH_WITHDRAWAL;
        }
        if (combined.matches(".*(salary|payroll|stipend|wages|bonus|interest|dividend).*")) {
            return TransactionCategory.SALARY;
        }
        if (combined.matches(".*(refund|cashback|reversal|chargeback).*")) {
            return TransactionCategory.REFUNDS;
        }
        if (combined.matches(".*(zerodha|groww|mutual fund|sip|upstox|coin|nps|sebi|investment).*")) {
            return TransactionCategory.INVESTMENTS;
        }
        if (combined.matches(".*(self transfer|transfer to self|linked account).*")) {
            return TransactionCategory.TRANSFERS;
        }
        return TransactionCategory.OTHER;
    }

    // Helper: Anomaly Detection Rules
    private void evaluateAnomaly(FinancialTransaction txn, Long userId) {
        // Rule 1: High debit threshold (> Rs 50,000)
        if (txn.getDirection() == TransactionDirection.EXPENSE && txn.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            txn.setStatus(TransactionStatus.FLAGGED_SUSPICIOUS);
            txn.setFlagReason("High-value debit (> ₹50,000) requires verification");
            return;
        }

        // Rule 2: Suspicious keywords in counterparty/notes
        String text = (txn.getMerchant() != null ? txn.getMerchant() : "") + " " + (txn.getNotes() != null ? txn.getNotes() : "");
        if (text.toLowerCase().matches(".*(crypto|bet|lottery|mandate auto|unknown debit|overdraft fee).*")) {
            txn.setStatus(TransactionStatus.FLAGGED_SUSPICIOUS);
            txn.setFlagReason("Transaction description contains high-risk keywords");
            return;
        }

        // Rule 3: Missing merchant/reference on large debit (> Rs 10,000)
        if (txn.getDirection() == TransactionDirection.EXPENSE && txn.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            if (txn.getMerchant() == null || txn.getMerchant().isBlank() || "Unknown Counterparty".equalsIgnoreCase(txn.getMerchant())) {
                txn.setStatus(TransactionStatus.NEEDS_REVIEW);
                txn.setFlagReason("Uncategorized large debit with missing merchant information");
            }
        }
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

    private int findColumnIndex(String[] headers, String... candidates) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim();
            for (String cand : candidates) {
                if (h.contains(cand)) return i;
            }
        }
        return -1;
    }

    private String cleanCol(String val) {
        if (val == null) return "";
        return val.trim().replaceAll("^\"|\"$", "");
    }

    private BigDecimal parseAmount(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        String val = cleanCol(cols[idx]).replace(",", "").replace("₹", "").replace("rs", "").trim();
        if (val.isEmpty()) return null;
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseCsvDate(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return LocalDateTime.now();
        String val = cleanCol(cols[idx]).trim();
        if (val.isEmpty()) return LocalDateTime.now();

        String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MMM-yyyy", "yyyy/MM/dd"};
        for (String f : formats) {
            try {
                LocalDate d = LocalDate.parse(val, DateTimeFormatter.ofPattern(f, Locale.ENGLISH));
                return d.atStartOfDay();
            } catch (Exception ignored) {}
        }
        return LocalDateTime.now();
    }

    private TransactionType inferTransactionType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("upi") || lower.contains("@")) return TransactionType.UPI;
        if (lower.contains("atm") || lower.contains("cash wdl")) return TransactionType.ATM;
        if (lower.contains("card") || lower.contains("pos") || lower.contains("debit card") || lower.contains("credit card")) return TransactionType.CARD;
        if (lower.contains("neft") || lower.contains("rtgs") || lower.contains("netbanking") || lower.contains("imps")) return TransactionType.NETBANKING;
        return TransactionType.UPI;
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
