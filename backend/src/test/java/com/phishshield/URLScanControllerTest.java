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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
