package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.domain.URLScan;
import com.phishshield.dto.request.ChatRequest;
import com.phishshield.dto.response.ChatResponse;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.ChatService;
import com.phishshield.service.FraudIncidentService;
import com.phishshield.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ActiveProfiles("test")
class GeminiIntegrationTest {

    @Autowired
    private URLScanRepository urlScanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private FraudIncidentService fraudIncidentService;

    private static final String MOCK_GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String MOCK_KEY = "mock-secret-gemini-key-xyz123";

    @BeforeEach
    void setUp() {
        urlScanRepository.deleteAll();
    }

    private ChatService createTestService(String provider, String apiKey, String endpoint, String model, int timeout) {
        return new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                provider,
                apiKey,
                endpoint,
                model,
                timeout
        );
    }

    @Test
    @DisplayName("Gemini Provider: Fails safely when API Key is missing and reports MISSING_CREDENTIALS")
    void testGeminiMissingCredentials() {
        ChatService service = createTestService("gemini", "", MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);

        Map<String, Object> status = service.getAiProviderStatus(false);
        assertEquals("gemini", status.get("provider"));
        assertEquals("gemini-1.5-flash", status.get("model"));
        assertEquals("MISSING_CREDENTIALS", status.get("status"));
        assertFalse((Boolean) status.get("hasApiKey"));

        // Sending a chat message falls back cleanly to the rule-based safety engine
        ChatRequest req = new ChatRequest();
        req.setMessage("What is phishing?");
        ChatResponse resp = service.processChat(req);

        assertNotNull(resp);
        assertNotNull(resp.getReply());
        assertEquals("gemini_unconfigured", resp.getProvider());
        assertFalse(resp.isLiveAi());
        assertTrue(resp.getReply().contains("phishing") || resp.getReply().contains("Prahar") || resp.getReply().contains("Safety Advice"));
    }

    @Test
    @DisplayName("Gemini Provider: Parses OpenAI-compatible response correctly with mocked RestClient")
    void testGeminiSuccessfulMockedResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        ChatService service = createTestService("gemini", MOCK_KEY, MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);
        service.setCustomRestClient(mockClient);

        // OpenAI-compatible format returned by Google Gemini
        String mockGeminiResponse = """
            {
              "id": "chatcmpl-mock-123",
              "object": "chat.completion",
              "created": 1710000000,
              "model": "gemini-1.5-flash",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Phishing is a deceptive social engineering attack designed to steal credentials or financial details."
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 20,
                "completion_tokens": 18,
                "total_tokens": 38
              }
            }
            """;

        server.expect(requestTo(MOCK_GEMINI_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + MOCK_KEY))
                .andRespond(withSuccess(mockGeminiResponse, MediaType.APPLICATION_JSON));

        ChatRequest req = new ChatRequest();
        req.setMessage("Explain phishing clearly");
        ChatResponse resp = service.processChat(req);

        server.verify();

        assertNotNull(resp);
        assertTrue(resp.isLiveAi());
        assertEquals("gemini", resp.getProvider());
        assertTrue(resp.getReply().contains("social engineering attack"));
    }

    @Test
    @DisplayName("Gemini Provider: Gracefully handles HTTP 401 Unauthorized without leaking secrets")
    void testGeminiUnauthorized401Handling() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        ChatService service = createTestService("gemini", MOCK_KEY, MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);
        service.setCustomRestClient(mockClient);

        server.expect(requestTo(MOCK_GEMINI_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": {\"message\": \"API key not valid\"}}"));

        ChatRequest req = new ChatRequest();
        req.setMessage("Check this scam message");
        ChatResponse resp = service.processChat(req);

        server.verify();

        assertNotNull(resp);
        assertFalse(resp.isLiveAi());
        assertEquals("gemini_fallback", resp.getProvider());
        // Verify key was NOT printed or leaked into the response text
        assertFalse(resp.getReply().contains(MOCK_KEY));
        assertFalse(resp.getReply().contains("Bearer"));

        // Status check reports error
        Map<String, Object> status = service.getAiProviderStatus(false);
        assertEquals("PROVIDER_ERROR", status.get("status"));
        assertTrue(status.get("lastError").toString().contains("Invalid or unauthorized API key"));
        assertFalse(status.get("lastError").toString().contains(MOCK_KEY));
    }

    @Test
    @DisplayName("Gemini Provider: Gracefully handles HTTP 429 Quota Exceeded")
    void testGeminiRateLimit429Handling() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        ChatService service = createTestService("gemini", MOCK_KEY, MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);
        service.setCustomRestClient(mockClient);

        server.expect(requestTo(MOCK_GEMINI_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\": {\"message\": \"Quota exceeded\"}}"));

        ChatRequest req = new ChatRequest();
        req.setMessage("Is this OTP request safe?");
        ChatResponse resp = service.processChat(req);

        server.verify();

        assertNotNull(resp);
        assertFalse(resp.isLiveAi());
        assertEquals("gemini_fallback", resp.getProvider());

        Map<String, Object> status = service.getAiProviderStatus(false);
        assertEquals("PROVIDER_ERROR", status.get("status"));
        assertTrue(status.get("lastError").toString().contains("Rate limit or quota exceeded"));
    }

    @Test
    @DisplayName("Scan Context: Injects real scan result into prompt when scanId is provided")
    void testRealScanContextInjection() {
        // Create and save an actual scan in the repository
        URLScan scan = new URLScan();
        scan.setUrl("https://fake-sbi-login.xyz");
        scan.setNormalizedUrl("https://fake-sbi-login.xyz");
        scan.setDomain("fake-sbi-login.xyz");
        scan.setRiskScore(92);
        scan.setRiskLevel(RiskLevel.HIGH_RISK);
        scan.setThreatCategory(ThreatCategory.BANKING_PHISHING);
        scan = urlScanRepository.save(scan);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        ChatService service = createTestService("gemini", MOCK_KEY, MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);
        service.setCustomRestClient(mockClient);

        // Expect the payload sent to Gemini to contain real scan details
        server.expect(requestTo(MOCK_GEMINI_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://fake-sbi-login.xyz")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HIGH_RISK")))
                .andRespond(withSuccess("""
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "This URL https://fake-sbi-login.xyz was flagged as HIGH_RISK banking phishing by Prahar."
                          }
                        }
                      ]
                    }
                    """, MediaType.APPLICATION_JSON));

        ChatRequest req = new ChatRequest();
        req.setMessage("Why was this site blocked?");
        req.setScanId(scan.getId());
        ChatResponse resp = service.processChat(req);

        server.verify();

        assertNotNull(resp);
        assertTrue(resp.isLiveAi());
        assertTrue(resp.getReply().contains("fake-sbi-login.xyz"));
    }

    @Test
    @DisplayName("Diagnostics: Probe endpoint verifies connection and reports PROVIDER_AVAILABLE")
    void testProbeEndpointSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        ChatService service = createTestService("gemini", MOCK_KEY, MOCK_GEMINI_ENDPOINT, "gemini-1.5-flash", 5000);
        service.setCustomRestClient(mockClient);

        server.expect(requestTo(MOCK_GEMINI_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "PONG"
                          }
                        }
                      ]
                    }
                    """, MediaType.APPLICATION_JSON));

        Map<String, Object> status = service.getAiProviderStatus(true); // probe = true
        server.verify();

        assertEquals("PROVIDER_AVAILABLE", status.get("status"));
        assertNull(status.get("lastError"));
    }
}
