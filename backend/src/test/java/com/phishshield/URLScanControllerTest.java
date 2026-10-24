package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.dto.request.EmailScanRequest;
import com.phishshield.dto.request.MessageScanRequest;
import com.phishshield.dto.request.URLScanRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class URLScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should scan a public URL and return risk metrics with prevention guide")
    void testScanPublicUrl() throws Exception {
        URLScanRequest request = URLScanRequest.builder()
                .url("http://secure-login-hdfcbank.account-verify.tk/login.php")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scanId").isNotEmpty())
                .andExpect(jsonPath("$.data.riskLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.threatCategory").value("BANKING_PHISHING"))
                .andExpect(jsonPath("$.data.preventionGuide.title").isNotEmpty());
    }

    @Test
    @DisplayName("Should block SSRF attempt against localhost")
    void testBlockSsrfLocalhost() throws Exception {
        URLScanRequest request = URLScanRequest.builder()
                .url("http://localhost:8080/actuator/env")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SSRF_VIOLATION_BLOCKED"));
    }

    @Test
    @DisplayName("Should analyze raw email body and extract phishing indicators")
    void testScanEmailBody() throws Exception {
        EmailScanRequest request = EmailScanRequest.builder()
                .emailContent("URGENT: Your bank account will be suspended within 24 hours. Update KYC at http://update-kyc-alert.xyz to avoid disruption.")
                .build();

        mockMvc.perform(post("/api/scans/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.threatCategory").isNotEmpty());
    }

    @Test
    @DisplayName("Should analyze SMS / WhatsApp text and extract prize bait indicators")
    void testScanMessageBody() throws Exception {
        MessageScanRequest request = MessageScanRequest.builder()
                .messageContent("Congratulations! You won a $500 gift card reward. Claim here: http://free-gift-reward.top/claim")
                .build();

        mockMvc.perform(post("/api/scans/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.detection").isNotEmpty());
    }

    @Test
    @DisplayName("Should fetch active security advisories without authentication")
    void testGetActiveAlerts() throws Exception {
        mockMvc.perform(get("/api/alerts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should fetch prevention guide for banking phishing")
    void testGetPreventionGuide() throws Exception {
        mockMvc.perform(get("/api/prevention/BANKING_PHISHING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.threatCategory").value("BANKING_PHISHING"))
                .andExpect(jsonPath("$.data.immediateActions").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject spoofed executable upload disguised as screenshot image")
    void testRejectSpoofedImageScreenshot() throws Exception {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "malicious.png",
                "image/png",
                exeBytes
        );

        mockMvc.perform(multipart("/api/scans/screenshot")
                        .file(fakeFile)
                        .param("text", "urgent verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported or unrecognized file format")));
    }

    @Test
    @DisplayName("Should reject corrupted QR code image upload")
    void testRejectCorruptedQrUpload() throws Exception {
        byte[] corruptedBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02};
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "broken-qr.png",
                "image/png",
                corruptedBytes
        );

        mockMvc.perform(multipart("/api/scans/qr")
                        .file(fakeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Corrupted or malformed image data")));
    }

    @Test
    @DisplayName("Should accept valid synthetic screenshot PNG and perform threat analysis")
    void testAcceptValidScreenshot() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "clean-screenshot.png",
                "image/png",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/scans/screenshot")
                        .file(validFile)
                        .param("text", "Urgent: update KYC at http://secure-hdfc-kyc.tk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scanType").value("SCREENSHOT"));
    }

    @Test
    @DisplayName("Should accept valid synthetic PNG on /api/scans/unified-image and perform multi-vector analysis")
    void testAcceptValidUnifiedImage() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(250, 150, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "unified-test.png",
                "image/png",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/scans/unified-image")
                        .file(validFile)
                        .param("text", "Urgent KYC notice: verify at http://secure-hdfc-kyc.tk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(org.hamcrest.Matchers.isOneOf("SUCCESS", "PARTIAL_SUCCESS")))
                .andExpect(jsonPath("$.data.imageValidation.valid").value(true))
                .andExpect(jsonPath("$.data.imageValidation.format").value("PNG"))
                .andExpect(jsonPath("$.data.urlCandidates").isArray());
    }

    @Test
    @DisplayName("Should reject malformed image on /api/scans/unified-image")
    void testRejectMalformedUnifiedImage() throws Exception {
        MockMultipartFile corruptedFile = new MockMultipartFile(
                "file",
                "corrupted.png",
                "image/png",
                new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00 }
        );

        mockMvc.perform(multipart("/api/scans/unified-image")
                        .file(corruptedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
