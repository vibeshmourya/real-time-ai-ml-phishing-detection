package com.phishshield.service;

import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionType;
import com.phishshield.dto.response.StatementPreviewResponseDto;
import com.phishshield.dto.response.StatementPreviewRowDto;
import com.phishshield.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UnifiedStatementParser {

    private static final Logger log = LoggerFactory.getLogger(UnifiedStatementParser.class);

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB limit
    private static final int MAX_ROWS_LIMIT = 5000;

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\b\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}\\b|\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b|\\b\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{2,4}\\b)");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);

    public StatementPreviewResponseDto parseStatementPreview(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement";
        String lowerName = originalFilename.toLowerCase();

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }

        StatementPreviewResponseDto response = new StatementPreviewResponseDto();
        response.setFileName(originalFilename);

        List<StatementPreviewRowDto> allParsedRows;

        if (lowerName.endsWith(".xlsx")) {
            response.setFileType("EXCEL_XLSX");
            allParsedRows = parseXlsx(fileBytes, response);
        } else if (lowerName.endsWith(".xls")) {
            response.setFileType("EXCEL_XLS");
            allParsedRows = parseXlsOrDelimited(fileBytes, response);
        } else if (lowerName.endsWith(".pdf")) {
            response.setFileType("PDF");
            allParsedRows = parsePdf(fileBytes, response);
        } else if (lowerName.endsWith(".csv") || lowerName.endsWith(".txt")) {
            response.setFileType("CSV");
            allParsedRows = parseCsv(fileBytes, response);
        } else {
            throw new BadRequestException("Unsupported statement format. Please upload CSV (.csv), Excel (.xlsx, .xls), or PDF (.pdf) statements.");
        }

        response.setTotalRowsFound(allParsedRows.size());

        int validCount = 0;
        int invalidCount = 0;
        List<StatementPreviewRowDto> previewSamples = new ArrayList<>();

        for (StatementPreviewRowDto row : allParsedRows) {
            if (row.isValid()) {
                validCount++;
            } else {
                invalidCount++;
                if (response.getInvalidRows().size() < 20) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("rowNumber", row.getRowNumber());
                    err.put("error", row.getValidationError());
                    err.put("description", row.getDescription());
                    response.getInvalidRows().add(err);
                }
            }
            if (previewSamples.size() < 25) {
                previewSamples.add(row);
            }
        }

        response.setValidRowsCount(validCount);
        response.setInvalidCount(invalidCount);
        response.setPreviewRows(previewSamples);

        if (validCount == 0 && invalidCount > 0) {
            response.setNotice("No valid transaction rows could be parsed from this file. Please check column format or upload an unencrypted statement.");
        } else if (response.isAmbiguous()) {
            response.setNotice("Ambiguity detected during extraction: " + response.getAmbiguityReason() + ". Please review the preview columns before committing.");
        } else {
            response.setNotice(String.format("Successfully parsed %d valid transaction records from %s.", validCount, originalFilename));
        }

        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded statement file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Uploaded file exceeds the 10 MB maximum limit.");
        }
    }

    // ==========================================
    // 1. CSV Statement Parser
    // ==========================================
    private List<StatementPreviewRowDto> parseCsv(byte[] bytes, StatementPreviewResponseDto response) {
        List<StatementPreviewRowDto> rows = new ArrayList<>();
        List<String[]> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                lines.add(parseCsvLine(line));
                if (lines.size() > MAX_ROWS_LIMIT) break;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV statement: " + e.getMessage());
        }

        if (lines.isEmpty()) {
            throw new BadRequestException("CSV file contains no data rows.");
        }

        int headerIndex = findHeaderRowIndex(lines);
        String[] headers = lines.get(headerIndex);
        Map<String, Integer> colMap = detectColumnIndices(headers);

        response.getDetectedColumns().put("dateColumn", colMap.containsKey("date") ? headers[colMap.get("date")] : "Not Found");
        response.getDetectedColumns().put("descriptionColumn", colMap.containsKey("description") ? headers[colMap.get("description")] : "Not Found");
        response.getDetectedColumns().put("debitColumn", colMap.containsKey("debit") ? headers[colMap.get("debit")] : "Not Found");
        response.getDetectedColumns().put("creditColumn", colMap.containsKey("credit") ? headers[colMap.get("credit")] : "Not Found");
        response.getDetectedColumns().put("amountColumn", colMap.containsKey("amount") ? headers[colMap.get("amount")] : "Not Found");

        boolean hasDebitCredit = colMap.containsKey("debit") || colMap.containsKey("credit");
        boolean hasAmount = colMap.containsKey("amount");

        if (!hasDebitCredit && !hasAmount) {
            response.setAmbiguous(true);
            response.setAmbiguityReason("No explicit Debit/Credit or Amount header identified. Falling back to heuristic column detection.");
        }

        for (int i = headerIndex + 1; i < lines.size(); i++) {
            String[] cols = lines.get(i);
            StatementPreviewRowDto row = processTabularRow(i + 1, cols, colMap);
            rows.add(row);
        }

        return rows;
    }

    // ==========================================
    // 2. OpenXML Excel (.xlsx) Parser (Pure Java SE)
    // ==========================================
    private List<StatementPreviewRowDto> parseXlsx(byte[] bytes, StatementPreviewResponseDto response) {
        // Validate XLSX magic bytes: PK\x03\x04
        if (bytes.length < 4 || bytes[0] != 0x50 || bytes[1] != 0x4B || bytes[2] != 0x03 || bytes[3] != 0x04) {
            throw new BadRequestException("Invalid XLSX file format (missing ZIP magic bytes).");
        }

        List<String> sharedStrings = new ArrayList<>();
        List<String[]> sheetRows = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] sheetXmlBytes = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("xl/sharedStrings.xml")) {
                    sharedStrings = parseSharedStringsXml(readStreamFully(zis));
                } else if (name.equals("xl/worksheets/sheet1.xml") || (sheetXmlBytes == null && name.startsWith("xl/worksheets/sheet"))) {
                    sheetXmlBytes = readStreamFully(zis);
                }
                zis.closeEntry();
            }

            if (sheetXmlBytes != null) {
                sheetRows = parseSheetXml(sheetXmlBytes, sharedStrings);
            }
        } catch (Exception e) {
            log.error("Failed to parse XLSX workbook", e);
            throw new BadRequestException("Excel XLSX statement parsing failed: " + e.getMessage());
        }

        if (sheetRows.isEmpty()) {
            throw new BadRequestException("Excel workbook sheet contains no readable transaction rows.");
        }

        int headerIndex = findHeaderRowIndex(sheetRows);
        String[] headers = sheetRows.get(headerIndex);
        Map<String, Integer> colMap = detectColumnIndices(headers);

        response.getDetectedColumns().put("dateColumn", colMap.containsKey("date") ? headers[colMap.get("date")] : "Not Found");
        response.getDetectedColumns().put("descriptionColumn", colMap.containsKey("description") ? headers[colMap.get("description")] : "Not Found");
        response.getDetectedColumns().put("debitColumn", colMap.containsKey("debit") ? headers[colMap.get("debit")] : "Not Found");
        response.getDetectedColumns().put("creditColumn", colMap.containsKey("credit") ? headers[colMap.get("credit")] : "Not Found");
        response.getDetectedColumns().put("amountColumn", colMap.containsKey("amount") ? headers[colMap.get("amount")] : "Not Found");

        List<StatementPreviewRowDto> rows = new ArrayList<>();
        for (int i = headerIndex + 1; i < sheetRows.size(); i++) {
            String[] cols = sheetRows.get(i);
            StatementPreviewRowDto row = processTabularRow(i + 1, cols, colMap);
            rows.add(row);
        }

        return rows;
    }

    private List<String> parseSharedStringsXml(byte[] xmlBytes) {
        List<String> list = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlBytes));
            NodeList siList = doc.getElementsByTagName("si");
            for (int i = 0; i < siList.getLength(); i++) {
                Element si = (Element) siList.item(i);
                NodeList tList = si.getElementsByTagName("t");
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < tList.getLength(); j++) {
                    sb.append(tList.item(j).getTextContent());
                }
                list.add(sb.toString().trim());
            }
        } catch (Exception e) {
            log.warn("Error parsing sharedStrings.xml: {}", e.getMessage());
        }
        return list;
    }

    private List<String[]> parseSheetXml(byte[] xmlBytes, List<String> sharedStrings) {
        List<String[]> rows = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlBytes));
            NodeList rowNodes = doc.getElementsByTagName("row");

            for (int i = 0; i < rowNodes.getLength(); i++) {
                Element rowEl = (Element) rowNodes.item(i);
                NodeList cNodes = rowEl.getElementsByTagName("c");
                Map<Integer, String> cellValues = new TreeMap<>();
                int maxCol = -1;

                for (int j = 0; j < cNodes.getLength(); j++) {
                    Element cEl = (Element) cNodes.item(j);
                    String r = cEl.getAttribute("r");
                    int colIdx = columnRefToIndex(r);
                    if (colIdx < 0) colIdx = j;
                    if (colIdx > maxCol) maxCol = colIdx;

                    String t = cEl.getAttribute("t");
                    NodeList vNodes = cEl.getElementsByTagName("v");
                    String cellVal = "";
                    if (vNodes.getLength() > 0) {
                        String rawV = vNodes.item(0).getTextContent();
                        if ("s".equals(t)) {
                            try {
                                int sIdx = Integer.parseInt(rawV);
                                if (sIdx >= 0 && sIdx < sharedStrings.size()) {
                                    cellVal = sharedStrings.get(sIdx);
                                }
                            } catch (Exception ignored) {}
                        } else {
                            cellVal = rawV;
                        }
                    } else {
                        NodeList isNodes = cEl.getElementsByTagName("is");
                        if (isNodes.getLength() > 0) {
                            cellVal = isNodes.item(0).getTextContent();
                        }
                    }
                    cellValues.put(colIdx, cellVal);
                }

                if (maxCol >= 0) {
                    String[] rowArr = new String[maxCol + 1];
                    Arrays.fill(rowArr, "");
                    for (Map.Entry<Integer, String> entry : cellValues.entrySet()) {
                        rowArr[entry.getKey()] = entry.getValue();
                    }
                    rows.add(rowArr);
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing sheet1.xml: {}", e.getMessage());
        }
        return rows;
    }

    private int columnRefToIndex(String cellRef) {
        if (cellRef == null || cellRef.isEmpty()) return -1;
        int col = 0;
        int i = 0;
        while (i < cellRef.length() && Character.isLetter(cellRef.charAt(i))) {
            col = col * 26 + (Character.toUpperCase(cellRef.charAt(i)) - 'A' + 1);
            i++;
        }
        return col - 1;
    }

    private List<StatementPreviewRowDto> parseXlsOrDelimited(byte[] bytes, StatementPreviewResponseDto response) {
        // Fallback for .xls: Attempt CSV/tab parsing if text or structured TSV
        return parseCsv(bytes, response);
    }

    // ==========================================
    // 3. Native PDF Bank Statement Parser (Pure Java SE)
    // ==========================================
    private List<StatementPreviewRowDto> parsePdf(byte[] bytes, StatementPreviewResponseDto response) {
        // Validate PDF magic bytes: %PDF-
        String headerCheck = new String(bytes, 0, Math.min(bytes.length, 10), StandardCharsets.ISO_8859_1);
        if (!headerCheck.startsWith("%PDF")) {
            throw new BadRequestException("Invalid PDF file (missing %PDF header).");
        }

        String fullRaw = new String(bytes, StandardCharsets.ISO_8859_1);

        // Check for PDF Encryption / Password Protection
        if (fullRaw.contains("/Encrypt")) {
            throw new BadRequestException("This PDF statement is password-protected or encrypted. Please remove password protection or upload an unencrypted bank statement.");
        }

        // Extract decompressed text streams from PDF
        List<String> textLines = extractPdfTextLines(bytes);

        if (textLines.isEmpty()) {
            throw new BadRequestException("No selectable text could be extracted from this PDF. It may be a scanned image-only PDF. Please export a digital PDF statement or use CSV/Excel.");
        }

        List<StatementPreviewRowDto> rows = new ArrayList<>();
        int rowNumber = 1;

        for (String line : textLines) {
            StatementPreviewRowDto candidate = parsePdfLineToRow(rowNumber, line);
            if (candidate != null) {
                rows.add(candidate);
                rowNumber++;
            }
        }

        if (rows.isEmpty()) {
            response.setAmbiguous(true);
            response.setAmbiguityReason("Text lines extracted but standard date/amount columns could not be correlated with high confidence.");
        } else {
            response.setConfidence("HIGH");
            response.getDetectedColumns().put("format", "PDF Bank Statement Stream Extractor");
        }

        return rows;
    }

    private List<String> extractPdfTextLines(byte[] bytes) {
        List<String> lines = new ArrayList<>();
        int i = 0;
        byte[] streamMarker = "stream".getBytes(StandardCharsets.ISO_8859_1);
        byte[] endStreamMarker = "endstream".getBytes(StandardCharsets.ISO_8859_1);

        while (i < bytes.length) {
            int streamStart = indexOfBytes(bytes, streamMarker, i);
            if (streamStart == -1) break;

            int dataStart = streamStart + streamMarker.length;
            if (dataStart < bytes.length && bytes[dataStart] == '\r') dataStart++;
            if (dataStart < bytes.length && bytes[dataStart] == '\n') dataStart++;

            int streamEnd = indexOfBytes(bytes, endStreamMarker, dataStart);
            if (streamEnd == -1) break;

            int length = streamEnd - dataStart;
            if (length > 0) {
                byte[] streamBytes = Arrays.copyOfRange(bytes, dataStart, streamEnd);
                // Try decompressing with Inflater (FlateDecode)
                byte[] decompressed = inflateFlate(streamBytes);
                byte[] target = (decompressed != null) ? decompressed : streamBytes;
                String content = new String(target, StandardCharsets.ISO_8859_1);

                extractStringsFromPdfStream(content, lines);
            }

            i = streamEnd + endStreamMarker.length;
        }

        // Group spaced tokens into structured transaction lines
        return groupPdfTokensIntoLines(lines);
    }

    private byte[] inflateFlate(byte[] compressed) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(compressed);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                if (count <= 0 && inflater.needsInput()) break;
                baos.write(buf, 0, count);
            }
            inflater.end();
            return baos.toByteArray();
        } catch (Exception e) {
            return null; // Not compressed or corrupted flate
        }
    }

    private void extractStringsFromPdfStream(String stream, List<String> tokens) {
        // PDF text operators: (text) Tj or [(str1) kern (str2)] TJ
        Pattern tjPattern = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*Tj");
        Matcher tjMatcher = tjPattern.matcher(stream);
        while (tjMatcher.find()) {
            String decoded = decodePdfString(tjMatcher.group(1));
            if (!decoded.isBlank()) tokens.add(decoded);
        }

        Pattern arrayPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*TJ");
        Matcher arrayMatcher = arrayPattern.matcher(stream);
        while (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Matcher strMatcher = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)").matcher(arrayContent);
            StringBuilder combined = new StringBuilder();
            while (strMatcher.find()) {
                combined.append(decodePdfString(strMatcher.group(1)));
            }
            if (combined.length() > 0 && !combined.toString().isBlank()) {
                tokens.add(combined.toString().trim());
            }
        }
    }

    private String decodePdfString(String raw) {
        return raw.replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
                .trim();
    }

    private List<String> groupPdfTokensIntoLines(List<String> rawTokens) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String token : rawTokens) {
            if (DATE_PATTERN.matcher(token).find() && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine.setLength(0);
            }
            if (currentLine.length() > 0) currentLine.append("  ");
            currentLine.append(token);
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        return lines;
    }

    private StatementPreviewRowDto parsePdfLineToRow(int rowNumber, String line) {
        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (!dateMatcher.find()) return null;

        String dateStr = dateMatcher.group(1);
        LocalDateTime dt = parseFlexibleDate(dateStr);

        // Find all monetary numbers
        Matcher amtMatcher = AMOUNT_PATTERN.matcher(line);
        List<BigDecimal> amounts = new ArrayList<>();
        while (amtMatcher.find()) {
            String clean = amtMatcher.group(1).replace(",", "");
            try {
                BigDecimal val = new BigDecimal(clean);
                if (val.compareTo(BigDecimal.ZERO) > 0 && !val.toPlainString().equals(dateStr.replace("-", "").replace("/", ""))) {
                    amounts.add(val);
                }
            } catch (Exception ignored) {}
        }

        if (amounts.isEmpty()) return null;

        // Typical bank statement: Date, Description, Debit, Credit, Balance
        // If 1 amount: determine DR/CR from keywords
        // If 2 amounts: usually Debit/Credit and Balance
        BigDecimal amount = amounts.get(0);
        TransactionDirection direction = TransactionDirection.EXPENSE;
        String lower = line.toLowerCase();

        if (lower.contains("cr") || lower.contains("credit") || lower.contains("deposit") || lower.contains("refund")) {
            direction = TransactionDirection.INCOME;
        } else if (lower.contains("dr") || lower.contains("debit") || lower.contains("withdrawal") || lower.contains("spent")) {
            direction = TransactionDirection.EXPENSE;
        }

        // Clean description
        String desc = line.replace(dateStr, "");
        for (BigDecimal a : amounts) {
            desc = desc.replace(a.toPlainString(), "");
        }
        desc = desc.replaceAll("\\s+", " ").trim();
        if (desc.isEmpty()) desc = "Statement Transaction";

        StatementPreviewRowDto row = new StatementPreviewRowDto();
        row.setRowNumber(rowNumber);
        row.setDate(dt);
        row.setDateStr(dateStr);
        row.setDescription(desc);
        row.setAmount(amount);
        row.setDirection(direction.name());
        row.setCategory(inferCategory(desc).name());
        row.setTransactionType(inferType(desc).name());
        row.setValid(true);

        return row;
    }

    // ==========================================
    // Tabular Row Normalization
    // ==========================================
    private StatementPreviewRowDto processTabularRow(int rowNum, String[] cols, Map<String, Integer> colMap) {
        StatementPreviewRowDto row = new StatementPreviewRowDto();
        row.setRowNumber(rowNum);

        int dateIdx = colMap.getOrDefault("date", -1);
        int descIdx = colMap.getOrDefault("description", -1);
        int debitIdx = colMap.getOrDefault("debit", -1);
        int creditIdx = colMap.getOrDefault("credit", -1);
        int amountIdx = colMap.getOrDefault("amount", -1);
        int refIdx = colMap.getOrDefault("reference", -1);
        int acctIdx = colMap.getOrDefault("account", -1);

        String rawDate = getCol(cols, dateIdx);
        String rawDesc = getCol(cols, descIdx);
        String rawDebit = getCol(cols, debitIdx);
        String rawCredit = getCol(cols, creditIdx);
        String rawAmount = getCol(cols, amountIdx);
        String rawRef = getCol(cols, refIdx);
        String rawAcct = getCol(cols, acctIdx);

        // Date Normalization
        LocalDateTime dt = parseFlexibleDate(rawDate);
        if (dt == null) {
            row.setValid(false);
            row.setValidationError("Unparseable date: '" + rawDate + "'");
            row.setDescription(rawDesc.isEmpty() ? "Invalid row" : rawDesc);
            return row;
        }
        row.setDate(dt);
        row.setDateStr(rawDate);

        // Description Normalization
        row.setDescription(!rawDesc.isEmpty() ? rawDesc : "Statement Import");
        row.setReference(!rawRef.isEmpty() ? rawRef : null);
        row.setAccount(!rawAcct.isEmpty() ? rawAcct : null);

        // Debit / Credit / Amount Normalization
        BigDecimal debitVal = parseNumber(rawDebit);
        BigDecimal creditVal = parseNumber(rawCredit);
        BigDecimal amtVal = parseNumber(rawAmount);

        BigDecimal finalAmount = null;
        TransactionDirection direction = null;

        if (debitVal != null && debitVal.compareTo(BigDecimal.ZERO) > 0) {
            direction = TransactionDirection.EXPENSE;
            finalAmount = debitVal;
            row.setDebit(debitVal);
        } else if (creditVal != null && creditVal.compareTo(BigDecimal.ZERO) > 0) {
            direction = TransactionDirection.INCOME;
            finalAmount = creditVal;
            row.setCredit(creditVal);
        } else if (amtVal != null && amtVal.compareTo(BigDecimal.ZERO) != 0) {
            if (amtVal.compareTo(BigDecimal.ZERO) < 0) {
                direction = TransactionDirection.EXPENSE;
                finalAmount = amtVal.abs();
            } else {
                direction = TransactionDirection.INCOME;
                finalAmount = amtVal;
            }
            row.setAmount(finalAmount);
        }

        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            row.setValid(false);
            row.setValidationError("Missing or zero transaction amount");
            return row;
        }

        row.setAmount(finalAmount);
        row.setDirection(direction.name());
        row.setCategory(inferCategory(row.getDescription()).name());
        row.setTransactionType(inferType(row.getDescription()).name());
        row.setValid(true);

        return row;
    }

    private String getCol(String[] cols, int idx) {
        if (idx >= 0 && idx < cols.length && cols[idx] != null) {
            return cols[idx].trim().replaceAll("^\"|\"$", "");
        }
        return "";
    }

    private BigDecimal parseNumber(String val) {
        if (val == null || val.isBlank()) return null;
        String clean = val.replace(",", "")
                .replace("₹", "")
                .replace("rs.", "")
                .replace("rs", "")
                .replace("inr", "")
                .trim();
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDateTime parseFlexibleDate(String val) {
        if (val == null || val.isBlank()) return null;
        String clean = val.trim();

        String[] patterns = {
                "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy",
                "dd-MMM-yyyy", "dd MMM yyyy", "yyyy/MM/dd", "d/M/yyyy",
                "d-M-yyyy", "dd.MM.yyyy", "yyyy.MM.dd"
        };

        for (String p : patterns) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(p, Locale.ENGLISH);
                LocalDate d = LocalDate.parse(clean, dtf);
                return d.atStartOfDay();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int findHeaderRowIndex(List<String[]> lines) {
        for (int i = 0; i < Math.min(lines.size(), 10); i++) {
            String[] row = lines.get(i);
            String combined = String.join(" ", row).toLowerCase();
            if ((combined.contains("date") || combined.contains("txn")) &&
                    (combined.contains("amount") || combined.contains("debit") || combined.contains("credit") || combined.contains("balance") || combined.contains("particulars") || combined.contains("description"))) {
                return i;
            }
        }
        return 0;
    }

    private Map<String, Integer> detectColumnIndices(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim();
            if (map.get("date") == null && matchesAny(h, "date", "txn date", "transaction date", "value date", "posting date")) {
                map.put("date", i);
            } else if (map.get("description") == null && matchesAny(h, "description", "narration", "particulars", "merchant", "details", "remarks", "payee")) {
                map.put("description", i);
            } else if (map.get("debit") == null && matchesAny(h, "debit", "withdrawal", "spent", "dr", "debit amount")) {
                map.put("debit", i);
            } else if (map.get("credit") == null && matchesAny(h, "credit", "deposit", "received", "cr", "credit amount")) {
                map.put("credit", i);
            } else if (map.get("amount") == null && matchesAny(h, "amount", "txn amount", "net amount")) {
                map.put("amount", i);
            } else if (map.get("reference") == null && matchesAny(h, "ref", "reference", "chq/ref no", "txn id", "utr", "rrn")) {
                map.put("reference", i);
            } else if (map.get("account") == null && matchesAny(h, "account", "a/c", "card no", "masked account")) {
                map.put("account", i);
            }
        }
        return map;
    }

    private boolean matchesAny(String str, String... candidates) {
        for (String c : candidates) {
            if (str.contains(c)) return true;
        }
        return false;
    }

    private String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private int indexOfBytes(byte[] source, byte[] target, int fromIndex) {
        if (fromIndex >= source.length || target.length == 0) return -1;
        for (int i = fromIndex; i <= source.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    public TransactionCategory inferCategory(String description) {
        if (description == null) return TransactionCategory.OTHER;
        String lower = description.toLowerCase();
        if (lower.matches(".*(zomato|swiggy|mcdonald|kfc|starbucks|burger|cafe|restaurant|hotel|food|canteen|bakes|bakery).*")) {
            return TransactionCategory.FOOD;
        }
        if (lower.matches(".*(amazon|flipkart|myntra|meesho|nykaa|zara|h&m|mart|supermarket|dmart|shopping|retail|store).*")) {
            return TransactionCategory.SHOPPING;
        }
        if (lower.matches(".*(electricity|bescom|tneb|airtel|jio|vi|recharge|broadband|water|gas|bill|utility|cylinder|dth).*")) {
            return TransactionCategory.BILLS;
        }
        if (lower.matches(".*(uber|ola|rapido|irctc|indigo|flight|train|metro|rail|fuel|petrol|diesel|toll|fastag|bus).*")) {
            return TransactionCategory.TRAVEL;
        }
        if (lower.matches(".*(atm|cash wdl|cash withdrawal|cash).*")) {
            return TransactionCategory.CASH_WITHDRAWAL;
        }
        if (lower.matches(".*(salary|payroll|stipend|wages|bonus|interest|dividend).*")) {
            return TransactionCategory.SALARY;
        }
        if (lower.matches(".*(refund|cashback|reversal|chargeback).*")) {
            return TransactionCategory.REFUNDS;
        }
        if (lower.matches(".*(zerodha|groww|mutual fund|sip|upstox|coin|nps|sebi|investment).*")) {
            return TransactionCategory.INVESTMENTS;
        }
        if (lower.matches(".*(self transfer|transfer to self|linked account).*")) {
            return TransactionCategory.TRANSFERS;
        }
        return TransactionCategory.OTHER;
    }

    public TransactionType inferType(String text) {
        if (text == null) return TransactionType.UPI;
        String lower = text.toLowerCase();
        if (lower.contains("upi") || lower.contains("@")) return TransactionType.UPI;
        if (lower.contains("atm") || lower.contains("cash wdl")) return TransactionType.ATM;
        if (lower.contains("card") || lower.contains("pos") || lower.contains("debit card") || lower.contains("credit card")) return TransactionType.CARD;
        if (lower.contains("neft") || lower.contains("rtgs") || lower.contains("netbanking") || lower.contains("imps")) return TransactionType.NETBANKING;
        return TransactionType.UPI;
    }
}
