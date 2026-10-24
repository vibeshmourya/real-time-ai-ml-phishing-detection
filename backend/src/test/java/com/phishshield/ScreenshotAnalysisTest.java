package com.phishshield;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.phishshield.domain.RiskLevel;
import com.phishshield.dto.response.ScreenshotAnalysisResponse;
import com.phishshield.service.OCRService;
import com.phishshield.service.ScreenshotTextAnalyzer;
import com.phishshield.service.URLScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreenshotAnalysisTest {

    @Autowired
    private URLScanService urlScanService;

    @Autowired
    private OCRService ocrService;

    @Autowired
    private ScreenshotTextAnalyzer screenshotTextAnalyzer;

    @Autowired
    private MockMvc mockMvc;

    private byte[] createSyntheticImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.drawString("PRAHAR SCREENSHOT SIMULATION", 20, 30);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createScreenshotWithEmbeddedQr(String qrUrl, int width, int height) throws Exception {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Generate QR code
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrUrl, BarcodeFormat.QR_CODE, 200, 200);
        BufferedImage qrImg = MatrixToImageWriter.toBufferedImage(matrix);

        // Draw QR code and simulated text banner
        g.drawImage(qrImg, 50, 50, null);
        g.setColor(Color.BLACK);
        g.drawString("Scan to verify KYC update immediately", 50, 280);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should extract text, correlate multi-signal phishing indicators, and scan extracted URL")
    void testOcrExtractionAndUrlScanning() throws Exception {
        byte[] imageBytes = createSyntheticImage(400, 250);
        MockMultipartFile file = new MockMultipartFile("file", "phish-sms.png", "image/png", imageBytes);

        String textHint = "URGENT: Your HDFC Bank NetBanking access will be blocked within 24 hours. " +
                "Enter your password and login credentials at http://secure-login-hdfcbank.account-verify.tk/login.php to avoid suspension.";

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, textHint, request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        // OCR Analysis
        assertNotNull(response.getOcrAnalysis());
        assertEquals(textHint, response.getOcrAnalysis().getExtractedText());
        assertTrue(response.getOcrAnalysis().getHeuristicRiskScore() >= 50);

        // Verify indicators
        assertFalse(response.getOcrAnalysis().getTextIndicators().isEmpty());
        boolean hasUrgency = response.getOcrAnalysis().getTextIndicators().stream()
                .anyMatch(i -> i.getRuleTriggered().equals("RULE_OCR_URGENCY_COERCION"));
        boolean hasCredential = response.getOcrAnalysis().getTextIndicators().stream()
                .anyMatch(i -> i.getRuleTriggered().equals("RULE_OCR_CREDENTIAL_SOLICITATION"));
        boolean hasBrand = response.getOcrAnalysis().getTextIndicators().stream()
                .anyMatch(i -> i.getRuleTriggered().equals("RULE_OCR_BRAND_REFERENCE"));

        assertTrue(hasUrgency, "Must detect coercive urgency language");
        assertTrue(hasCredential, "Must detect password/credential solicitation");
        assertTrue(hasBrand, "Must detect HDFC brand cue");

        // Extracted URL scan
        assertFalse(response.getExtractedUrlScans().isEmpty());
        assertEquals("http://secure-login-hdfcbank.account-verify.tk/login.php", response.getExtractedUrlScans().get(0).getUrl());
        assertEquals("BANKING_PHISHING", response.getPrimaryScan().getThreatCategory().name());
        assertTrue(response.getOverallRiskScore() >= 70);
        assertEquals(RiskLevel.HIGH_RISK, response.getOverallRiskLevel());
    }

    @Test
    @DisplayName("Should not classify benign screenshot text as phishing")
    void testBenignScreenshotText() throws Exception {
        byte[] imageBytes = createSyntheticImage(300, 200);
        MockMultipartFile file = new MockMultipartFile("file", "order-confirmation.png", "image/png", imageBytes);

        String benignText = "Thank you for shopping with us! Your order #44921 has been confirmed and is scheduled for delivery on Monday.";

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, benignText, request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getOcrAnalysis().getHeuristicRiskScore());
        assertEquals(RiskLevel.SAFE, response.getOverallRiskLevel());
        assertTrue(response.getOcrAnalysis().getTextIndicators().isEmpty());
    }

    @Test
    @DisplayName("Should not classify single isolated login phrase as malicious (false-positive prevention)")
    void testSingleIsolatedPromptNotMalicious() throws Exception {
        byte[] imageBytes = createSyntheticImage(300, 200);
        MockMultipartFile file = new MockMultipartFile("file", "login-form.png", "image/png", imageBytes);

        String loginOnlyText = "Sign In: Please enter your password to continue to your dashboard.";

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, loginOnlyText, request);

        // Score must stay in low/safe range (<= 25)
        assertTrue(response.getOverallRiskScore() <= 25, "Single login prompt must not trigger a malicious classification");
        assertEquals(RiskLevel.SAFE, response.getOverallRiskLevel());
    }

    @Test
    @DisplayName("Should detect high-risk OTP solicitation in screenshot text")
    void testOtpSolicitationDetection() throws Exception {
        byte[] imageBytes = createSyntheticImage(300, 200);
        MockMultipartFile file = new MockMultipartFile("file", "otp-threat.png", "image/png", imageBytes);

        String otpThreat = "State Bank security: Share your 6-digit OTP verification code immediately or account blocked within 12 hours.";

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, otpThreat, request);

        boolean hasOtp = response.getOcrAnalysis().getTextIndicators().stream()
                .anyMatch(i -> i.getRuleTriggered().equals("RULE_OCR_OTP_SOLICITATION"));
        assertTrue(hasOtp, "Must flag OTP solicitation indicator");
        assertTrue(response.getOverallRiskScore() >= 50);
    }

    @Test
    @DisplayName("Should combine optical QR code detection and OCR text in a unified response")
    void testCombinedQrAndOcrAnalysis() throws Exception {
        String qrPhishUrl = "http://secure-login-hdfcbank.account-verify.tk/login.php";
        byte[] combinedBytes = createScreenshotWithEmbeddedQr(qrPhishUrl, 500, 400);

        MockMultipartFile file = new MockMultipartFile("file", "qr-screenshot.png", "image/png", combinedBytes);
        String ocrText = "Urgent: Update KYC at bank portal within 24 hours.";

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, ocrText, request);

        assertNotNull(response.getQrAnalysis(), "Must contain QR analysis section");
        assertTrue(response.getQrAnalysis().getTotalQrCodesFound() >= 1, "Must detect embedded QR code in screenshot");
        assertNotNull(response.getOcrAnalysis(), "Must contain OCR analysis section");
        assertNotNull(response.getPrimaryScan(), "Must have primary scan result");
        assertNotNull(response.getLimitations(), "Must include analytical limitations");
    }

    @Test
    @DisplayName("Should handle image with no text and no QR code gracefully without fabricating results")
    void testBlankScreenshotNoTextNoQr() throws Exception {
        byte[] blankBytes = createSyntheticImage(200, 200);
        MockMultipartFile file = new MockMultipartFile("file", "blank-canvas.png", "image/png", blankBytes);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ScreenshotAnalysisResponse response = urlScanService.analyzeScreenshot(file, null, request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("NO_TEXT_DETECTED", response.getOcrAnalysis().getExtractionStatus());
        assertEquals(0, response.getOcrAnalysis().getHeuristicRiskScore());
        assertEquals(RiskLevel.SAFE, response.getOverallRiskLevel());
    }

    @Test
    @DisplayName("MockMvc: POST /api/scans/screenshot should return structured ScreenshotAnalysisResponse")
    void testScreenshotEndpointMockMvc() throws Exception {
        byte[] imageBytes = createSyntheticImage(200, 100);
        MockMultipartFile validFile = new MockMultipartFile("file", "test.png", "image/png", imageBytes);

        mockMvc.perform(multipart("/api/scans/screenshot")
                        .file(validFile)
                        .param("text", "Urgent KYC update required at http://secure-login-hdfcbank.account-verify.tk/login.php"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallRiskScore").isNotEmpty())
                .andExpect(jsonPath("$.data.ocrAnalysis.extractedText").isNotEmpty())
                .andExpect(jsonPath("$.data.ocrAnalysis.textIndicators").isArray())
                .andExpect(jsonPath("$.data.primaryScan.scanId").isNotEmpty())
                .andExpect(jsonPath("$.data.limitations").isArray());
    }
}
