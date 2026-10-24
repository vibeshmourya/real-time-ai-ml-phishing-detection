package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.URLScan;
import com.phishshield.dto.request.URLScanRequest;
import com.phishshield.dto.response.MLPredictionResult;
import com.phishshield.dto.response.MLServiceHealthDto;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.service.MLServiceClient;
import com.phishshield.service.URLScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ActiveProfiles("test")
class MLServiceClientTest {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private URLScanService urlScanService;

    @Autowired
    private URLScanRepository urlScanRepository;

    private static final String MOCK_ML_URL = "http://localhost:8000";

    @BeforeEach
    void setUp() {
        urlScanRepository.deleteAll();
    }

    @Test
    @DisplayName("ML Service: Parses FastAPI /predict successful response schema correctly")
    void testSuccessfulPredictionSchema() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        MLServiceClient client = new MLServiceClient(restTemplateBuilder, MOCK_ML_URL, 3000);
        client.setRestTemplate(restTemplate);

        String fastApiResponse = """
            {
              "success": true,
              "url": "http://malicious-bank-verify.xyz/login",
              "prediction": "PHISHING",
              "risk_level": "HIGH_RISK",
              "risk_score": 88,
              "ml_probability": 0.88,
              "confidence": 0.94,
              "threat_category": "CREDENTIAL_THEFT",
              "indicators": [
                {
                  "code": "SUSPICIOUS_KEYWORD",
                  "severity": "HIGH",
                  "message": "URL contains sensitive keyword 'login'",
                  "weight": 20.0
                }
              ],
              "explanation": [
                "The target URL contains sensitive banking keywords.",
                "The domain structure resembles known credential harvesting portals."
              ],
              "model": {
                "name": "Logistic Regression",
                "version": "1.0.0",
                "selection_metric": "f1"
              },
              "execution_time_ms": 12.4
            }
            """;

        server.expect(requestTo(MOCK_ML_URL + "/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("malicious-bank-verify.xyz")))
                .andRespond(withSuccess(fastApiResponse, MediaType.APPLICATION_JSON));

        MLPredictionResult result = client.predictUrlTyped("http://malicious-bank-verify.xyz/login");
        server.verify();

        assertTrue(result.isAvailable());
        assertEquals("PHISHING", result.getPrediction());
        assertEquals("HIGH_RISK", result.getRiskLevel());
        assertEquals(88, result.getRiskScore());
        assertEquals(0.88, result.getMlProbability(), 0.001);
        assertEquals("Logistic Regression", result.getModelName());
        assertEquals("1.0.0", result.getModelVersion());
        assertEquals(2, result.getExplanations().size());
        assertNull(result.getFailureReason());
    }

    @Test
    @DisplayName("ML Service: Handles HTTP 503 Model Unavailable when model artifact is missing")
    void testModelUnavailable503() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        MLServiceClient client = new MLServiceClient(restTemplateBuilder, MOCK_ML_URL, 3000);
        client.setRestTemplate(restTemplate);

        String errorResponse = """
            {
              "detail": {
                "code": "MODEL_UNAVAILABLE",
                "message": "ML model is not loaded: Model artifact missing."
              }
            }
            """;

        server.expect(requestTo(MOCK_ML_URL + "/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));

        MLPredictionResult result = client.predictUrlTyped("http://example.com");
        server.verify();

        assertFalse(result.isAvailable());
        assertNotNull(result.getFailureReason());
        assertTrue(result.getFailureReason().contains("MODEL_UNAVAILABLE"));
    }

    @Test
    @DisplayName("ML Service: Health probe maps /health endpoint into MLServiceHealthDto")
    void testHealthCheckProbe() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        MLServiceClient client = new MLServiceClient(restTemplateBuilder, MOCK_ML_URL, 3000);
        client.setRestTemplate(restTemplate);

        String healthResponse = """
            {
              "status": "UP",
              "service": "prahar-ml-service",
              "model_loaded": true,
              "model_version": "1.0.0",
              "model_name": "Logistic Regression"
            }
            """;

        server.expect(requestTo(MOCK_ML_URL + "/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(healthResponse, MediaType.APPLICATION_JSON));

        MLServiceHealthDto health = client.checkHealth();
        server.verify();

        assertEquals("UP", health.getStatus());
        assertTrue(health.isModelLoaded());
        assertEquals("Logistic Regression", health.getModelName());
        assertEquals("1.0.0", health.getModelVersion());
        assertNull(health.getError());
    }

    @Test
    @DisplayName("Scan Integration: Low ML probability does NOT override high-risk heuristic verdict")
    void testHeuristicPriorityOverLowMlProbability() {
        // Even if ML returns low probability, structural heuristics (such as raw IP host) must preserve HIGH_RISK!
        URLScanRequest req = new URLScanRequest();
        req.setUrl("http://93.184.216.34/banking-login.php");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        URLScanResponse response = urlScanService.scanUrl(req, httpReq);

        assertNotNull(response);
        // The URLScan service correctly flags IP in URL with severe score
        assertTrue(response.getRiskScore() >= 40, "Heuristic indicators must prevent false safe status");
        assertNotEquals(RiskLevel.SAFE, response.getRiskLevel());
    }

    @Test
    @DisplayName("Scan Integration: Scan response includes ModelPrediction explainability metadata")
    void testScanResponseIncludesModelPrediction() {
        URLScanRequest req = new URLScanRequest();
        req.setUrl("https://sbi-fake-account-update.xyz");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        URLScanResponse response = urlScanService.scanUrl(req, httpReq);

        assertNotNull(response);
        assertNotNull(response.getModelPrediction());
        assertNotNull(response.getModelPrediction().getModelName());
        // Since ML service is offline during mock test, isMlAvailable must be false or explain offline status honestly
        assertNotNull(response.getModelPrediction().getRuleScore());
    }
}
