package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.domain.URLScan;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.repository.URLScanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase34IntegrationRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private URLScanRepository urlScanRepository;

    @Test
    @DisplayName("REG-01: Benign Indian banking portal whitelisting produces SAFE verdict with 0 risk score")
    void testBenignBankingPortalWhitelisting() throws Exception {
        URLScanRequest request = URLScanRequest.builder()
                .url("https://onlinesbi.sbi/portal/login.htm")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("SAFE"))
                .andExpect(jsonPath("$.data.riskScore").value(0))
                .andExpect(jsonPath("$.data.domain").value("onlinesbi.sbi"))
                .andExpect(jsonPath("$.data.threatCategory").value("BENIGN"));
    }

    @Test
    @DisplayName("REG-02: Phishing brand spoofing on suspicious TLD produces HIGH_RISK verdict with explainability")
    void testPhishingBrandSpoofingHighRisk() throws Exception {
        URLScanRequest request = URLScanRequest.builder()
                .url("http://secure-login-update.sbi.bank.account-verification.xyz/auth/signin?token=928374")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH_RISK"))
                .andExpect(jsonPath("$.data.riskScore", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.data.explainability").exists())
                .andExpect(jsonPath("$.data.detection.indicators", not(empty())));
    }

    @Test
    @DisplayName("REG-03: Punycode IDN homoglyph domain is detected and penalized")
    void testIdnHomoglyphDetection() throws Exception {
        URLScanRequest request = URLScanRequest.builder()
                .url("http://xn--pypal-4ve.com/signin/authenticate")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskLevel", is(oneOf("SUSPICIOUS", "HIGH_RISK"))))
                .andExpect(jsonPath("$.data.riskScore", greaterThan(0)));
    }

    @Test
    @DisplayName("REG-04: SSRF protection blocks loopback 127.0.0.1 and private RFC-1918 subnets")
    void testSsrfBlocksLoopbackAndPrivateSubnets() throws Exception {
        // 1. Loopback
        URLScanRequest loopbackReq = URLScanRequest.builder()
                .url("http://127.0.0.1:8080/actuator/health")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loopbackReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("SSRF_VIOLATION_BLOCKED"))
                .andExpect(jsonPath("$.message", containsString("Security Policy Violation")));

        // 2. RFC-1918 Private Subnet
        URLScanRequest privateReq = URLScanRequest.builder()
                .url("http://192.168.1.1/router-login")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("SSRF_VIOLATION_BLOCKED"))
                .andExpect(jsonPath("$.message", containsString("Security Policy Violation")));
    }

    @Test
    @DisplayName("REG-05: SSRF protection blocks AWS cloud metadata link-local address 169.254.169.254")
    void testSsrfBlocksAwsMetadata() throws Exception {
        URLScanRequest metadataReq = URLScanRequest.builder()
                .url("http://169.254.169.254/latest/meta-data/")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metadataReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("SSRF_VIOLATION_BLOCKED"))
                .andExpect(jsonPath("$.message", containsString("Security Policy Violation")));
    }

    @Test
    @DisplayName("REG-06: Malformed, empty, or unsupported protocol inputs fail validation cleanly")
    void testMalformedInputRejection() throws Exception {
        // Empty URL
        URLScanRequest emptyReq = URLScanRequest.builder()
                .url("")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Unsupported scheme (e.g. ftp)
        URLScanRequest ftpReq = URLScanRequest.builder()
                .url("ftp://files.example.com/exploit.sh")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ftpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("REG-07: ML status endpoint responds with structured health DTO")
    void testMlStatusEndpointHealth() throws Exception {
        mockMvc.perform(get("/api/scans/ml-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    @DisplayName("REG-08: Database persistence stores scan record and links threat detection")
    void testDatabasePersistenceAndAuditIntegrity() throws Exception {
        long initialCount = urlScanRepository.count();

        URLScanRequest request = URLScanRequest.builder()
                .url("http://audit-integrity-test.example.org/verify")
                .build();

        mockMvc.perform(post("/api/scans/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long afterCount = urlScanRepository.count();
        assertThat(afterCount).isGreaterThan(initialCount);

        List<URLScan> scans = urlScanRepository.findAll();
        URLScan lastScan = scans.get(scans.size() - 1);
        assertThat(lastScan.getUrl()).isEqualTo("http://audit-integrity-test.example.org/verify");
        assertThat(lastScan.getStatus()).isEqualTo("COMPLETED");
        assertThat(lastScan.getClientIp()).isNotNull();
    }

    @Test
    @DisplayName("REG-09: Request correlation ID is echoed in response headers via X-Request-ID")
    void testCorrelationIdEchoInResponseHeader() throws Exception {
        String testCorrelationId = "phase34-trace-uuid-98765";

        mockMvc.perform(get("/api/health")
                        .header("X-Request-ID", testCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", testCorrelationId));
    }

    @Test
    @DisplayName("REG-10: Health check endpoint responds with healthy UP status")
    void testAggregatedHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("prahar-backend"));
    }
}
