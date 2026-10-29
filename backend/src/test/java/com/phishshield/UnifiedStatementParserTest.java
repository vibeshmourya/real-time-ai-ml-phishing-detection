package com.phishshield;

import com.phishshield.dto.response.StatementPreviewResponseDto;
import com.phishshield.dto.response.StatementPreviewRowDto;
import com.phishshield.exception.BadRequestException;
import com.phishshield.service.UnifiedStatementParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnifiedStatementParserTest {

    private UnifiedStatementParser parser;

    @BeforeEach
    void setUp() {
        parser = new UnifiedStatementParser();
    }

    @Test
    @DisplayName("PARSER-01: CSV with multiple date formats and debit/credit columns parses accurately")
    void testCsvMultipleDatesAndDebitCredit() {
        String csvContent = "Date,Description,Debit,Credit,Reference\n" +
                "2026-10-15,Swiggy Food Delivery,450.50,,UPI123456\n" +
                "16/10/2026,Salary Credit,,65000.00,SAL987654\n" +
                "18-10-2026,Electricity Bescom,1850.00,,BES246810\n" +
                "20-Oct-2026,Refund Swiggy,,250.00,REF135790\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        StatementPreviewResponseDto response = parser.parseStatementPreview(file);

        assertThat(response.getFileType()).isEqualTo("CSV");
        assertThat(response.getValidRowsCount()).isEqualTo(4);
        assertThat(response.getInvalidCount()).isEqualTo(0);

        StatementPreviewRowDto row1 = response.getPreviewRows().get(0);
        assertThat(row1.getDescription()).isEqualTo("Swiggy Food Delivery");
        assertThat(row1.getAmount()).isEqualByComparingTo(new BigDecimal("450.50"));
        assertThat(row1.getDirection()).isEqualTo("EXPENSE");

        StatementPreviewRowDto row2 = response.getPreviewRows().get(1);
        assertThat(row2.getDescription()).isEqualTo("Salary Credit");
        assertThat(row2.getAmount()).isEqualByComparingTo(new BigDecimal("65000.00"));
        assertThat(row2.getDirection()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("PARSER-02: CSV with single signed Amount column normalizes debit and credit correctly")
    void testCsvSignedAmountColumn() {
        String csvContent = "Transaction Date,Particulars,Amount,Chq/Ref No\n" +
                "2026-09-01,Groceries Supermarket,-2340.00,REF100\n" +
                "2026-09-05,Client Payment,15000.00,REF101\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "bank_export.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        StatementPreviewResponseDto response = parser.parseStatementPreview(file);

        assertThat(response.getValidRowsCount()).isEqualTo(2);
        assertThat(response.getPreviewRows().get(0).getDirection()).isEqualTo("EXPENSE");
        assertThat(response.getPreviewRows().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("2340.00"));
        assertThat(response.getPreviewRows().get(1).getDirection()).isEqualTo("INCOME");
        assertThat(response.getPreviewRows().get(1).getAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("PARSER-03: Malformed dates and missing amounts are recorded as invalid rows without aborting import")
    void testCsvMalformedRowsHandling() {
        String csvContent = "Date,Description,Debit,Credit\n" +
                "2026-10-10,Valid Lunch,250.00,\n" +
                "INVALID_DATE,Corrupted Row,500.00,\n" +
                "2026-10-12,Missing Amount,,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement_dirty.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        StatementPreviewResponseDto response = parser.parseStatementPreview(file);

        assertThat(response.getValidRowsCount()).isEqualTo(1);
        assertThat(response.getInvalidCount()).isEqualTo(2);
        assertThat(response.getInvalidRows()).hasSize(2);
    }

    @Test
    @DisplayName("PARSER-04: OpenXML Excel (.xlsx) workbook is unpacked and parsed natively")
    void testXlsxParsing() throws IOException {
        byte[] xlsxBytes = createMockXlsxWorkbook(
                new String[]{"Date", "Description", "Debit", "Credit"},
                new String[][]{
                        {"2026-10-01", "Internet Bill Broadband", "999.00", ""},
                        {"2026-10-02", "Monthly Salary Deposit", "", "50000.00"}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "bank_statement.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes
        );

        StatementPreviewResponseDto response = parser.parseStatementPreview(file);

        assertThat(response.getFileType()).isEqualTo("EXCEL_XLSX");
        assertThat(response.getValidRowsCount()).isEqualTo(2);
        assertThat(response.getPreviewRows().get(0).getDescription()).isEqualTo("Internet Bill Broadband");
        assertThat(response.getPreviewRows().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("999.00"));
        assertThat(response.getPreviewRows().get(1).getDirection()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("PARSER-05: PDF bank statement text extraction extracts structured transaction rows")
    void testPdfStatementParsing() {
        byte[] pdfBytes = createMockPdfWithText(
                "State Bank of India Account Statement\n" +
                "15/10/2026 UPI/SWIGGY/481920 450.00 DR 12450.00\n" +
                "18/10/2026 SALARY/OCT2026 50000.00 CR 62450.00\n"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "sbi_statement.pdf", "application/pdf", pdfBytes
        );

        StatementPreviewResponseDto response = parser.parseStatementPreview(file);

        assertThat(response.getFileType()).isEqualTo("PDF");
        assertThat(response.getValidRowsCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("PARSER-06: Encrypted PDF statement triggers password protection explanation")
    void testEncryptedPdfRejection() {
        String encryptedPdf = "%PDF-1.4\n1 0 obj\n<< /Filter /Standard /V 2 /R 3 /Encrypt >>\nendobj\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "locked_statement.pdf", "application/pdf", encryptedPdf.getBytes(StandardCharsets.ISO_8859_1)
        );

        assertThatThrownBy(() -> parser.parseStatementPreview(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("password-protected or encrypted");
    }

    @Test
    @DisplayName("PARSER-07: Empty file or non-matching magic bytes are rejected with clean error")
    void testEmptyAndCorruptFileValidation() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThatThrownBy(() -> parser.parseStatementPreview(emptyFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");

        MockMultipartFile fakePdf = new MockMultipartFile("file", "fake.pdf", "application/pdf", "NOT_A_PDF".getBytes());
        assertThatThrownBy(() -> parser.parseStatementPreview(fakePdf))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid PDF file");
    }

    // Helper: Build pure-Java OpenXML ZIP bytes
    private byte[] createMockXlsxWorkbook(String[] headers, String[][] rows) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. sharedStrings.xml
            zos.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            StringBuilder ssb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            for (String h : headers) {
                ssb.append("<si><t>").append(h).append("</t></si>");
            }
            for (String[] r : rows) {
                for (String c : r) {
                    ssb.append("<si><t>").append(c).append("</t></si>");
                }
            }
            ssb.append("</sst>");
            zos.write(ssb.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. sheet1.xml
            zos.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            
            // Header Row
            sb.append("<row r=\"1\">");
            for (int col = 0; col < headers.length; col++) {
                sb.append("<c r=\"").append((char)('A' + col)).append("1\" t=\"s\"><v>").append(col).append("</v></c>");
            }
            sb.append("</row>");

            // Data Rows
            int strIdx = headers.length;
            for (int r = 0; r < rows.length; r++) {
                sb.append("<row r=\"").append(r + 2).append("\">");
                for (int c = 0; c < rows[r].length; c++) {
                    sb.append("<c r=\"").append((char)('A' + c)).append(r + 2).append("\" t=\"s\"><v>").append(strIdx++).append("</v></c>");
                }
                sb.append("</row>");
            }
            sb.append("</sheetData></worksheet>");
            zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    // Helper: Build mock PDF stream with Tj operators
    private byte[] createMockPdfWithText(String content) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write("%PDF-1.4\n1 0 obj\n<< /Length 200 >>\nstream\nBT\n/F1 12 Tf\n".getBytes(StandardCharsets.ISO_8859_1));
            for (String line : content.split("\n")) {
                baos.write(("(" + line.replace("(", "\\(").replace(")", "\\)") + ") Tj\nT*\n").getBytes(StandardCharsets.ISO_8859_1));
            }
            baos.write("ET\nendstream\nendobj\nxref\n0 2\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException ignored) {}
        return baos.toByteArray();
    }
}
