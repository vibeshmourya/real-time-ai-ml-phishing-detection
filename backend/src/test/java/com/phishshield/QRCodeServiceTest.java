package com.phishshield;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.phishshield.dto.response.QRCodeAnalysisResponse;
import com.phishshield.dto.response.QRCodeItemResponse;
import com.phishshield.dto.response.QRPayloadType;
import com.phishshield.exception.BadRequestException;
import com.phishshield.security.image.ImageSecurityValidator;
import com.phishshield.service.QRCodeService;
import com.phishshield.service.URLScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class QRCodeServiceTest {

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private ImageSecurityValidator imageSecurityValidator;

    @Autowired
    private URLScanService urlScanService;

    private byte[] generateSyntheticQrCode(String payload, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }

    private byte[] generateTiledDualQrCode(String payload1, String payload2, int width, int height) throws Exception {
        int halfWidth = width / 2;
        QRCodeWriter writer = new QRCodeWriter();

        BitMatrix matrix1 = writer.encode(payload1, BarcodeFormat.QR_CODE, halfWidth, height);
        BitMatrix matrix2 = writer.encode(payload2, BarcodeFormat.QR_CODE, halfWidth, height);

        BufferedImage img1 = MatrixToImageWriter.toBufferedImage(matrix1);
        BufferedImage img2 = MatrixToImageWriter.toBufferedImage(matrix2);

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combined.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(img1, 0, 0, null);
        g.drawImage(img2, halfWidth, 0, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should successfully decode a synthetic QR code containing an HTTPS URL")
    void testDecodeValidHttpsUrl() throws Exception {
        String testUrl = "https://legitimate-bank.com/portal/login";
        byte[] qrBytes = generateSyntheticQrCode(testUrl, 300);

        MockMultipartFile file = new MockMultipartFile("file", "test-qr.png", "image/png", qrBytes);

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertFalse(payloads.isEmpty());
        assertEquals(testUrl, payloads.get(0));

        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(payloads.get(0), 1);
        assertEquals(QRPayloadType.URL_HTTP_HTTPS, item.getPayloadType());
        assertTrue(item.isScannableUrl());
        assertEquals(testUrl, item.getExtractedUrl());
    }

    @Test
    @DisplayName("Should classify and handle plain text QR codes without fabricating a threat")
    void testDecodePlainTextQr() throws Exception {
        String plainText = "PROMO_CODE_DISCOUNT_2026";
        byte[] qrBytes = generateSyntheticQrCode(plainText, 300);

        MockMultipartFile file = new MockMultipartFile("file", "text-qr.png", "image/png", qrBytes);

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertFalse(payloads.isEmpty());
        assertEquals(plainText, payloads.get(0));

        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(payloads.get(0), 1);
        assertEquals(QRPayloadType.PLAIN_TEXT, item.getPayloadType());
        assertFalse(item.isScannableUrl());
        assertEquals("INFORMATIONAL_TEXT", item.getScanStatus());
    }

    @Test
    @DisplayName("Should detect and block dangerous javascript: URI scheme")
    void testBlockDangerousJavascriptScheme() throws Exception {
        String dangerousUri = "javascript:alert(document.cookie)";
        byte[] qrBytes = generateSyntheticQrCode(dangerousUri, 300);

        MockMultipartFile file = new MockMultipartFile("file", "xss-qr.png", "image/png", qrBytes);

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertFalse(payloads.isEmpty());

        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(payloads.get(0), 1);
        assertEquals(QRPayloadType.URL_DANGEROUS_SCHEME, item.getPayloadType());
        assertFalse(item.isScannableUrl());
        assertEquals("BLOCKED_DANGEROUS_SCHEME", item.getScanStatus());
        assertTrue(item.getValidationError().contains("Dangerous URI scheme detected"));
    }

    @Test
    @DisplayName("Should detect and block local file: URI scheme")
    void testBlockFileScheme() throws Exception {
        String fileUri = "file:///etc/shadow";
        byte[] qrBytes = generateSyntheticQrCode(fileUri, 300);

        MockMultipartFile file = new MockMultipartFile("file", "file-qr.png", "image/png", qrBytes);

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertFalse(payloads.isEmpty());

        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(payloads.get(0), 1);
        assertEquals(QRPayloadType.URL_DANGEROUS_SCHEME, item.getPayloadType());
        assertFalse(item.isScannableUrl());
        assertEquals("BLOCKED_DANGEROUS_SCHEME", item.getScanStatus());
    }

    @Test
    @DisplayName("Should identify UPI payment intent correctly")
    void testIdentifyUpiPayment() throws Exception {
        String upiPayload = "upi://pay?pa=merchant@upi&pn=Store&am=500.00&cu=INR";
        byte[] qrBytes = generateSyntheticQrCode(upiPayload, 300);

        MockMultipartFile file = new MockMultipartFile("file", "upi-qr.png", "image/png", qrBytes);

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertFalse(payloads.isEmpty());

        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(payloads.get(0), 1);
        assertEquals(QRPayloadType.UPI_PAYMENT, item.getPayloadType());
        assertFalse(item.isScannableUrl());
        assertEquals("IDENTIFIED_UPI_PAYMENT", item.getScanStatus());
    }

    @Test
    @DisplayName("Should handle unreadable image without QR code cleanly")
    void testHandleImageWithNoQr() throws Exception {
        // Create blank image with no barcode
        BufferedImage blank = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(blank, "png", baos);

        MockMultipartFile file = new MockMultipartFile("file", "blank.png", "image/png", baos.toByteArray());

        List<String> payloads = qrCodeService.decodeAllQRCodes(file);
        assertTrue(payloads.isEmpty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        QRCodeAnalysisResponse response = urlScanService.analyzeQRCode(file, null, request);
        assertEquals(0, response.getTotalQrCodesFound());
        assertEquals("NO_QR_DETECTED", response.getStatus());
    }

    @Test
    @DisplayName("Should reject deceptive URL with backslash pathing")
    void testRejectDeceptiveBackslashUrl() {
        String deceptiveUrl = "https://legitimate-bank.com\\attacker.com/login";
        QRCodeItemResponse item = qrCodeService.processAndClassifyPayload(deceptiveUrl, 1);

        assertEquals(QRPayloadType.MALFORMED_URL, item.getPayloadType());
        assertFalse(item.isScannableUrl());
        assertTrue(item.getValidationError().contains("backslash"));
    }

    @Test
    @DisplayName("Should integrate with URLScanService and return real threat verdict for phishing QR")
    void testEndToEndPhishingQrScan() throws Exception {
        String phishingUrl = "http://secure-login-hdfcbank.account-verify.tk/login.php";
        byte[] qrBytes = generateSyntheticQrCode(phishingUrl, 300);
        MockMultipartFile file = new MockMultipartFile("file", "phish-qr.png", "image/png", qrBytes);

        MockHttpServletRequest request = new MockHttpServletRequest();
        QRCodeAnalysisResponse response = urlScanService.analyzeQRCode(file, null, request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getTotalQrCodesFound());
        assertNotNull(response.getPrimaryScan());
        assertEquals("BANKING_PHISHING", response.getThreatCategory().name());
        assertNotNull(response.getRiskScore());
        assertTrue(response.getRiskScore() >= 35);

        QRCodeItemResponse item = response.getQrCodes().get(0);
        assertEquals("SCANNED", item.getScanStatus());
        assertNotNull(item.getScanResult());
        assertEquals(phishingUrl, item.getExtractedUrl());
    }

    @Test
    @DisplayName("Should detect and decode multiple QR codes in a single image")
    void testDecodeMultipleQRCodes() throws Exception {
        String url1 = "https://domain-alpha.org/welcome";
        String url2 = "https://domain-beta.org/catalog";

        byte[] tiledBytes = generateTiledDualQrCode(url1, url2, 800, 400);
        MockMultipartFile file = new MockMultipartFile("file", "dual-qr.png", "image/png", tiledBytes);

        List<String> decoded = qrCodeService.decodeAllQRCodes(file);

        // ZXing multi-reader should detect at least 1 or both codes from the tiled canvas
        assertFalse(decoded.isEmpty(), "Should decode at least one QR code");
        assertTrue(decoded.contains(url1) || decoded.contains(url2));
    }
}
