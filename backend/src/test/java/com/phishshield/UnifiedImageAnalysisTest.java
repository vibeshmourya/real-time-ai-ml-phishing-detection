package com.phishshield;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.dto.response.CandidateUrlSource;
import com.phishshield.dto.response.UnifiedImageAnalysisResponse;
import com.phishshield.dto.response.UnifiedUrlCandidateResponse;
import com.phishshield.exception.BadRequestException;
import com.phishshield.service.UnifiedImageAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnifiedImageAnalysisTest {

    @Autowired
    private UnifiedImageAnalysisService unifiedImageAnalysisService;

    @Test
    @DisplayName("1. Image with both QR code and OCR text containing different URLs")
    void testImageWithQrAndOcrUrls() throws Exception {
        String qrUrl = "http://secure-login-hdfcbank.account-verify.tk/login.php";
        String ocrText = "Alert: Verify account at http://icici-security-update.tk within 24 hours.";

        byte[] imageBytes = createScreenshotWithQr(qrUrl, 500, 400);
        MockMultipartFile file = new MockMultipartFile("file", "qr-and-text.png", "image/png", imageBytes);
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, ocrText, request);

        assertNotNull(response);
        assertEquals(2, response.getUrlCandidates().size());
        assertTrue(response.getUrlCandidates().stream().anyMatch(c -> c.getSource() == CandidateUrlSource.QR));
        assertTrue(response.getUrlCandidates().stream().anyMatch(c -> c.getSource() == CandidateUrlSource.OCR));
        assertEquals(RiskLevel.HIGH_RISK, response.getOverallRiskLevel());
        assertNotNull(response.getPrimaryScan());
    }

    @Test
    @DisplayName("2. QR-only case without any visible OCR text")
    void testQrOnlyImage() throws Exception {
        String qrUrl = "http://phish-axisbank-net.xyz/verify";
        byte[] imageBytes = createScreenshotWithQr(qrUrl, 300, 300);
        MockMultipartFile file = new MockMultipartFile("file", "qr-only.png", "image/png", imageBytes);
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, "", request);

        assertNotNull(response);
        assertEquals(1, response.getQrAnalysis().getTotalQrCodesFound());
        assertEquals(1, response.getUrlCandidates().size());
        assertEquals(CandidateUrlSource.QR, response.getUrlCandidates().get(0).getSource());
        assertEquals("SCANNED", response.getUrlCandidates().get(0).getScanStatus());
    }

    @Test
    @DisplayName("3. OCR-only case without any QR codes on canvas")
    void testOcrOnlyImage() throws Exception {
        byte[] imageBytes = createBlankImage(300, 200);
        MockMultipartFile file = new MockMultipartFile("file", "ocr-only.png", "image/png", imageBytes);
        String ocrText = "Payment notice: please update KYC at https://sbi-card-kyc-alert.com or account blocked.";
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, ocrText, request);

        assertNotNull(response);
        assertEquals(0, response.getQrAnalysis().getTotalQrCodesFound());
        assertEquals("NO_QR_DETECTED", response.getQrAnalysis().getStatus());
        assertEquals(1, response.getUrlCandidates().size());
        assertEquals(CandidateUrlSource.OCR, response.getUrlCandidates().get(0).getSource());
    }

    @Test
    @DisplayName("4. Benign image with no QR code and no URL")
    void testBenignImageNoQrNoUrl() throws Exception {
        byte[] imageBytes = createBlankImage(300, 200);
        MockMultipartFile file = new MockMultipartFile("file", "benign.png", "image/png", imageBytes);
        String benignText = "Thank you for visiting our office today. Have a pleasant weekend!";
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, benignText, request);

        assertNotNull(response);
        assertEquals(0, response.getQrAnalysis().getTotalQrCodesFound());
        assertTrue(response.getUrlCandidates().isEmpty());
        assertEquals(0, response.getOverallRiskScore());
        assertEquals(RiskLevel.SAFE, response.getOverallRiskLevel());
        assertEquals(ThreatCategory.BENIGN, response.getThreatCategory());
    }

    @Test
    @DisplayName("5. Deduplication: exact same URL discovered through BOTH QR code and OCR text")
    void testDeduplicateDuplicateUrlFromBothSources() throws Exception {
        String sharedUrl = "http://secure-login-hdfcbank.account-verify.tk/login.php";
        byte[] imageBytes = createScreenshotWithQr(sharedUrl, 400, 400);
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.png", "image/png", imageBytes);

        String ocrText = "Please log in at http://secure-login-hdfcbank.account-verify.tk/login.php to avoid account suspension.";
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, ocrText, request);

        assertNotNull(response);
        // Must be deduplicated into a single candidate with source = BOTH
        assertEquals(1, response.getUrlCandidates().size());
        UnifiedUrlCandidateResponse candidate = response.getUrlCandidates().get(0);
        assertEquals(CandidateUrlSource.BOTH, candidate.getSource());
        assertEquals("SCANNED", candidate.getScanStatus());
        assertEquals(1, response.getSuccessfulScans().size());
    }

    @Test
    @DisplayName("6. Malformed and corrupted image rejected prior to analysis")
    void testRejectCorruptedImage() {
        MockMultipartFile corrupted = new MockMultipartFile(
                "file",
                "bad.png",
                "image/png",
                new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00 }
        );
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(BadRequestException.class, () ->
                unifiedImageAnalysisService.analyzeImage(corrupted, null, request));
    }

    @Test
    @DisplayName("7. Multiple QR codes in a single image")
    void testMultipleQrCodesInSingleImage() throws Exception {
        String url1 = "http://phish-link-one.tk";
        String url2 = "http://phish-link-two.tk";
        byte[] multiQrImage = createMultiQrImage(url1, url2, 600, 300);

        MockMultipartFile file = new MockMultipartFile("file", "multi-qr.png", "image/png", multiQrImage);
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, "", request);

        assertNotNull(response);
        assertTrue(response.getQrAnalysis().getTotalQrCodesFound() >= 2);
        assertTrue(response.getUrlCandidates().size() >= 2);
    }

    @Test
    @DisplayName("8. Failure isolation: dangerous QR scheme intercepted and reported with safe handling")
    void testDangerousQrSchemeFailureIsolation() throws Exception {
        String dangerousScheme = "javascript:alert(document.cookie)";
        byte[] imageBytes = createScreenshotWithQr(dangerousScheme, 300, 300);
        MockMultipartFile file = new MockMultipartFile("file", "xss-qr.png", "image/png", imageBytes);

        MockHttpServletRequest request = new MockHttpServletRequest();
        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, "", request);

        assertNotNull(response);
        assertEquals(1, response.getUrlCandidates().size());
        UnifiedUrlCandidateResponse candidate = response.getUrlCandidates().get(0);
        assertFalse(candidate.isScannable());
        assertEquals("BLOCKED_DANGEROUS_SCHEME", candidate.getScanStatus());
        assertEquals(RiskLevel.HIGH_RISK, response.getOverallRiskLevel());
    }

    @Test
    @DisplayName("9. Resource limits: excessive candidate URLs capped at maximum limit")
    void testResourceLimitOnCandidateUrls() throws Exception {
        byte[] imageBytes = createBlankImage(200, 100);
        MockMultipartFile file = new MockMultipartFile("file", "bulk-links.png", "image/png", imageBytes);

        String bulkText = "Links: http://test1.tk http://test2.tk http://test3.tk http://test4.tk http://test5.tk http://test6.tk http://test7.tk";
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, bulkText, request);

        assertNotNull(response);
        assertEquals(7, response.getUrlCandidates().size());
        // At most 5 candidates are marked scannable / eligible for scan execution
        long scannedCount = response.getUrlCandidates().stream()
                .filter(c -> "SCANNED".equals(c.getScanStatus()))
                .count();
        assertEquals(5, scannedCount);

        long limitedCount = response.getUrlCandidates().stream()
                .filter(c -> "RESOURCE_LIMIT_EXCEEDED".equals(c.getScanStatus()))
                .count();
        assertEquals(2, limitedCount);
    }

    @Test
    @DisplayName("10. Dual-compatibility delegation getters match primary scan output")
    void testDualCompatibilityDelegationGetters() throws Exception {
        byte[] imageBytes = createBlankImage(200, 100);
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", imageBytes);
        String text = "Visit http://secure-login-hdfcbank.account-verify.tk/login.php to verify your account.";
        MockHttpServletRequest request = new MockHttpServletRequest();

        UnifiedImageAnalysisResponse response = unifiedImageAnalysisService.analyzeImage(file, text, request);

        assertNotNull(response.getScanId());
        assertNotNull(response.getScanType());
        assertNotNull(response.getUrl());
        assertNotNull(response.getRiskScore());
        assertNotNull(response.getRiskLevel());
        assertNotNull(response.getDetection());
        assertEquals(response.getPrimaryScan().getScanId(), response.getScanId());
        assertEquals(response.getPrimaryScan().getUrl(), response.getUrl());
    }

    // --- Helper methods to generate synthetic test fixtures ---

    private byte[] createBlankImage(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createScreenshotWithQr(String qrContent, int canvasWidth, int canvasHeight) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 180, 180);
        BufferedImage qrImg = MatrixToImageWriter.toBufferedImage(matrix);

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        g.setColor(Color.BLACK);
        g.drawString("Account Security Verification Portal", 20, 30);
        g.drawImage(qrImg, 50, 60, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createMultiQrImage(String content1, String content2, int canvasWidth, int canvasHeight) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BufferedImage qr1 = MatrixToImageWriter.toBufferedImage(writer.encode(content1, BarcodeFormat.QR_CODE, 140, 140));
        BufferedImage qr2 = MatrixToImageWriter.toBufferedImage(writer.encode(content2, BarcodeFormat.QR_CODE, 140, 140));

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        g.drawImage(qr1, 40, 50, null);
        g.drawImage(qr2, 300, 50, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", baos);
        return baos.toByteArray();
    }
}
