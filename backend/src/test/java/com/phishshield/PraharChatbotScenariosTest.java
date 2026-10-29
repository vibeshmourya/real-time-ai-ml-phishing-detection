package com.phishshield;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.domain.URLScan;
import com.phishshield.dto.request.ChatMessageDto;
import com.phishshield.dto.request.ChatRequest;
import com.phishshield.dto.response.ChatResponse;
import com.phishshield.nlp.IncidentState;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Automated end-to-end regression tests covering Prahar AI Chatbot Scenarios A through H.
 */
@SpringBootTest
@ActiveProfiles("test")
public class PraharChatbotScenariosTest {

    @Autowired
    private URLScanRepository urlScanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private FraudIncidentService fraudIncidentService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                "none",
                "",
                "",
                "gemini-1.5-flash",
                5000
        );
        urlScanRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario A: Basic Knowledge Query - 'What is phishing?'")
    void testScenarioA_BasicKnowledgeQuery() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is phishing?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertFalse(response.getReply().isEmpty());
        // Verify concise definition without excessive static checklists
        assertTrue(response.getReply().toLowerCase().contains("phishing"));
        assertTrue(response.getActionChecklist() == null || response.getActionChecklist().isEmpty());
    }

    @Test
    @DisplayName("Scenario B: Multi-Turn Context Retention - link clicked followed by 'Will changing my password help?'")
    void testScenarioB_MultiTurnContextRetention() {
        // Turn 1: Clicked suspicious link
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("I clicked a suspicious link.");
        ChatResponse res1 = chatService.processChat(turn1);

        assertEquals("PHISHING_URL_ANALYSIS", res1.getIntent());
        assertTrue(res1.getReply().contains("Close that browser tab immediately"));

        // Turn 2: Follow-up asking if changing password will help
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("Will changing my password help?");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "I clicked a suspicious link."),
                new ChatMessageDto("assistant", res1.getReply())
        ));
        ChatResponse res2 = chatService.processChat(turn2);

        assertNotNull(res2);
        assertTrue(res2.isFollowUp());
        // Must address password change directly AND provide context on financial/1930 safety
        assertTrue(res2.getReply().toLowerCase().contains("changing your password"));
        assertTrue(res2.getReply().contains("1930"));
    }

    @Test
    @DisplayName("Scenario C: Active Incident Containment - 'I gave my OTP to a caller' followed by 'They said they were from SBI'")
    void testScenarioC_ActiveIncidentContainment() {
        // Turn 1: Disclosed OTP
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("I gave my OTP to a caller");
        ChatResponse res1 = chatService.processChat(turn1);

        assertEquals("OTP_DISCLOSURE", res1.getIntent());
        assertTrue(res1.getReply().contains("1930"));
        assertTrue(res1.getReply().toLowerCase().contains("stop sharing"));

        // Turn 2: Caller claimed to be SBI
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("They said they were from SBI");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "I gave my OTP to a caller"),
                new ChatMessageDto("assistant", res1.getReply())
        ));
        ChatResponse res2 = chatService.processChat(turn2);

        assertNotNull(res2);
        // Retains that OTP was compromised and mentions bank / 1930 containment
        assertTrue(res2.getReply().contains("1930") || res2.getReply().toLowerCase().contains("bank"));
    }

    @Test
    @DisplayName("Scenario D: Indian Financial Fraud / UPI Scam - 'I scanned a QR code to receive money but money was deducted'")
    void testScenarioD_UpiQrCodeScam() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Someone sent me a QR code to receive money, I scanned it and entered my UPI PIN, and 5000 rupees was deducted!");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("UPI_FRAUD", response.getIntent());
        // Recognizes UPI QR deception (receiving money never requires scanning or entering PIN)
        assertTrue(response.getReply().toLowerCase().contains("qr") || response.getReply().toLowerCase().contains("pin") || response.getReply().toLowerCase().contains("upi"));
        // Mentions National Cyber Helpline 1930 and cybercrime.gov.in
        assertTrue(response.getReply().contains("1930"));
        assertTrue(response.getReply().contains("cybercrime.gov.in"));
    }

    @Test
    @DisplayName("Scenario E: URL Analysis Request with Heuristic & Scan Context")
    void testScenarioE_UrlAnalysisRequest() {
        URLScan scan = new URLScan();
        scan.setUrl("https://secure-sbi-kyc-update.com");
        scan.setNormalizedUrl("https://secure-sbi-kyc-update.com");
        scan.setDomain("secure-sbi-kyc-update.com");
        scan.setRiskScore(95);
        scan.setRiskLevel(RiskLevel.HIGH_RISK);
        scan.setThreatCategory(ThreatCategory.BANKING_PHISHING);
        scan = urlScanRepository.save(scan);

        ChatRequest request = new ChatRequest();
        request.setMessage("Can you explain why this URL was flagged?");
        request.setScanId(scan.getId());

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("EXPLAIN_SCAN", response.getIntent());
        assertTrue(response.getReply().contains("secure-sbi-kyc-update.com"));
        assertTrue(response.getReply().contains("95"));
    }

    @Test
    @DisplayName("Scenario F: Unavailable Gemini Fallback - Graceful degradation with verified rule engine")
    void testScenarioF_UnavailableGeminiFallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        ChatService service = new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                "gemini",
                "mock-secret-key-12345",
                endpoint,
                "gemini-1.5-flash",
                5000
        );
        service.setCustomRestClient(mockClient);

        // Simulate 503 Service Unavailable from external API
        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\": \"Backend overloaded\"}"));

        ChatRequest request = new ChatRequest();
        request.setMessage("What is phishing?");

        ChatResponse response = service.processChat(request);

        server.verify();
        assertNotNull(response);
        assertFalse(response.isLiveAi());
        assertEquals("gemini_fallback", response.getProvider());
        // Service notice present and safe rules answer provided
        assertTrue(response.getReply().contains("External AI provider (gemini) unavailable"));
        assertTrue(response.getReply().toLowerCase().contains("phishing"));
    }

    @Test
    @DisplayName("Scenario G: Conversation Isolation - Context does not bleed between distinct sessions")
    void testScenarioG_ConversationIsolation() {
        // Session A user reports credit card fraud
        ChatRequest sessionA = new ChatRequest();
        sessionA.setMessage("My credit card was stolen and 10000 rupees was spent");
        ChatResponse resA = chatService.processChat(sessionA);
        assertTrue("ATM_CARD_FRAUD".equals(resA.getIntent()) || "UNKNOWN_DEBIT".equals(resA.getIntent()));

        // Session B user asks a fresh, independent question with an empty history
        ChatRequest sessionB = new ChatRequest();
        sessionB.setMessage("What is phishing?");
        sessionB.setConversationHistory(Collections.emptyList());
        ChatResponse resB = chatService.processChat(sessionB);

        assertEquals("GENERAL_CYBER_SAFETY", resB.getIntent());
        assertFalse(resB.getReply().toLowerCase().contains("credit card"));
        assertFalse(resB.getReply().contains("10000"));
    }

    @Test
    @DisplayName("Scenario H: Response Quality & Anti-Repetition - follow-up does not repeat introductory greeting")
    void testScenarioH_AntiRepetitionAndConciseness() {
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("can u help me with account safety");
        ChatResponse res1 = chatService.processChat(turn1);

        assertTrue(res1.getReply().contains("Of course!"));

        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("How can I report it?");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "can u help me with account safety"),
                new ChatMessageDto("assistant", res1.getReply())
        ));
        ChatResponse res2 = chatService.processChat(turn2);

        // Follow-up immediately provides reporting channels without re-introducing
        assertFalse(res2.getReply().startsWith("Of course! I can help you"));
        assertTrue(res2.getReply().contains("1930"));
        assertTrue(res2.getReply().contains("cybercrime.gov.in"));
    }

    // ==========================================
    // NATURAL LANGUAGE UNDERSTANDING & EDUCATIONAL TESTS
    // ==========================================

    @Test
    @DisplayName("Natural Language 1: 'What are online scams?' - Direct explanation with examples, no generic checklist")
    void testNaturalLanguage1_WhatAreOnlineScams() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What are online scams?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("online scams"));
        assertTrue(response.getReply().toLowerCase().contains("phishing") || response.getReply().toLowerCase().contains("fake shopping"));
        assertTrue(response.getActionChecklist() == null || response.getActionChecklist().isEmpty());
    }

    @Test
    @DisplayName("Natural Language 2: 'What are suspicious links?' - Explains malicious links and safe verification with Prahar")
    void testNaturalLanguage2_WhatAreSuspiciousLinks() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What are suspicious links?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("suspicious links"));
        assertTrue(response.getReply().toLowerCase().contains("prahar") || response.getReply().toLowerCase().contains("scan"));
        assertFalse(response.getReply().toLowerCase().contains("close that browser tab immediately")); // Not an incident
        assertTrue(response.getActionChecklist() == null || response.getActionChecklist().isEmpty());
    }

    @Test
    @DisplayName("Natural Language 3: 'How do I recognize a replica website?' - Explains warning signs of imitation websites")
    void testNaturalLanguage3_HowDoIRecognizeReplicaWebsite() {
        ChatRequest request = new ChatRequest();
        request.setMessage("How do I recognize a replica website?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("replica") || response.getReply().toLowerCase().contains("imitation"));
        assertTrue(response.getReply().toLowerCase().contains("domain") || response.getReply().toLowerCase().contains("typosquatting"));
        assertFalse(response.getReply().toLowerCase().contains("did you pay online")); // Not an active order incident
    }

    @Test
    @DisplayName("Natural Language 4: 'What is OTP?' - Defines OTP, explains purpose, warns never to share")
    void testNaturalLanguage4_WhatIsOtp() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is OTP?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("one-time password") || response.getReply().toLowerCase().contains("otp"));
        assertTrue(response.getReply().toLowerCase().contains("never share"));
    }

    @Test
    @DisplayName("Natural Language 5: 'What is the difference between phishing and online scams?' - Explains distinction without treating as fraud incident")
    void testNaturalLanguage5_DifferenceBetweenPhishingAndOnlineScams() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is the difference between phishing and online scams?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("difference between phishing and online scams"));
        assertTrue(response.getReply().toLowerCase().contains("umbrella"));
        assertFalse(response.getReply().toLowerCase().contains("call 1930 immediately")); // Not an active emergency
    }

    @Test
    @DisplayName("Natural Language 6: 'How can I stay safe online?' - Practical, actionable safety tips")
    void testNaturalLanguage6_HowCanIStaySafeOnline() {
        ChatRequest request = new ChatRequest();
        request.setMessage("How can I stay safe online?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("passwords") || response.getReply().toLowerCase().contains("2fa"));
        assertTrue(response.getReply().toLowerCase().contains("upi") || response.getReply().toLowerCase().contains("otp"));
    }

    @Test
    @DisplayName("Natural Language 7: 'What is the 1930 helpline?' - Explains National Cyber Crime Helpline and Golden Hour")
    void testNaturalLanguage7_WhatIs1930Helpline() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is the 1930 helpline?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertTrue(response.getReply().contains("1930"));
        assertTrue(response.getReply().toLowerCase().contains("cyber") || response.getReply().toLowerCase().contains("helpline"));
        assertTrue(response.getReply().toLowerCase().contains("golden hour") || response.getReply().toLowerCase().contains("cfcfrms") || response.getReply().toLowerCase().contains("freeze"));
    }

    @Test
    @DisplayName("Natural Language 8: Context Follow-Up - 'What is phishing?' -> 'How can I identify it?'")
    void testNaturalLanguage8_FollowUpContext_Phishing_IdentifyIt() {
        // Turn 1
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("What is phishing?");
        ChatResponse res1 = chatService.processChat(turn1);
        assertEquals("GENERAL_CYBER_SAFETY", res1.getIntent());

        // Turn 2
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("How can I identify it?");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "What is phishing?"),
                new ChatMessageDto("assistant", res1.getReply())
        ));
        ChatResponse res2 = chatService.processChat(turn2);

        assertNotNull(res2);
        assertEquals("GENERAL_CYBER_SAFETY", res2.getIntent());
        assertTrue(res2.isFollowUp());
        assertTrue(res2.getReply().toLowerCase().contains("identify") || res2.getReply().toLowerCase().contains("spot"));
        assertTrue(res2.getReply().toLowerCase().contains("phishing") || res2.getReply().toLowerCase().contains("domains"));
    }

    @Test
    @DisplayName("Natural Language 9-11: Incident Multi-Turn Sequence - Fake website -> Paid using UPI -> What should I do now?")
    void testNaturalLanguage9_11_MultiTurnIncidentSequence() {
        // Turn 1: User reports entering details on fake site
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("I entered my details on a fake website.");
        ChatResponse res1 = chatService.processChat(turn1);

        assertNotNull(res1);
        assertTrue(res1.getReply().toLowerCase().contains("details") || res1.getReply().toLowerCase().contains("panic") || res1.getReply().toLowerCase().contains("steps"));

        // Turn 2: User specifies payment method: UPI
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("I paid using UPI.");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "I entered my details on a fake website."),
                new ChatMessageDto("assistant", res1.getReply())
        ));
        ChatResponse res2 = chatService.processChat(turn2);

        assertNotNull(res2);
        assertTrue(res2.isFollowUp());
        assertTrue(res2.getReply().toLowerCase().contains("upi"));
        assertTrue(res2.getReply().contains("1930"));
        assertTrue(res2.getReply().toLowerCase().contains("utr"));

        // Turn 3: User asks next steps based on retained context
        ChatRequest turn3 = new ChatRequest();
        turn3.setMessage("What should I do now?");
        turn3.setConversationHistory(List.of(
                new ChatMessageDto("user", "I entered my details on a fake website."),
                new ChatMessageDto("assistant", res1.getReply()),
                new ChatMessageDto("user", "I paid using UPI."),
                new ChatMessageDto("assistant", res2.getReply())
        ));
        ChatResponse res3 = chatService.processChat(turn3);

        assertNotNull(res3);
        assertTrue(res3.isFollowUp());
        // Must give synthesized next steps without asking "Did you pay online or COD?" again!
        assertFalse(res3.getReply().toLowerCase().contains("is it cash on delivery"));
        assertTrue(res3.getReply().contains("1930"));
        assertTrue(res3.getReply().toLowerCase().contains("utr") || res3.getReply().toLowerCase().contains("dispute"));
    }

    @Test
    @DisplayName("Natural Language 12: 'What is the difference between an OTP and a UPI PIN?' - Clear definition and distinctions")
    void testNaturalLanguage12_DifferenceBetweenOtpAndUpiPin() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is the difference between an OTP and a UPI PIN?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("GENERAL_CYBER_SAFETY", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("difference between an otp and a upi pin"));
        assertTrue(response.getReply().toLowerCase().contains("otp"));
        assertTrue(response.getReply().toLowerCase().contains("upi pin"));
        assertTrue(response.getReply().toLowerCase().contains("always deducts money") || response.getReply().toLowerCase().contains("debits money"));
    }

    // ==========================================
    // PREDEFINED QUESTIONS REGRESSION TESTS
    // ==========================================

    @Test
    @DisplayName("Regression: Predefined question - '₹5,000 was debited from my account'")
    void testPredefined_UnauthorizedDebit() {
        ChatRequest request = new ChatRequest();
        request.setMessage("₹5,000 was debited from my account");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("UNKNOWN_DEBIT", response.getIntent());
        assertTrue(response.getReply().contains("5000"));
        assertTrue(response.getReply().contains("1930"));
    }

    @Test
    @DisplayName("Regression: Predefined question - 'What should I do if my phone is stolen?'")
    void testPredefined_StolenPhone() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What should I do if my phone is stolen?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("STOLEN_PHONE", response.getIntent());
        assertTrue(response.getReply().toLowerCase().contains("ceir") || response.getReply().toLowerCase().contains("sim"));
    }

    @Test
    @DisplayName("Regression: Predefined question - 'What should I do if I shared an OTP?'")
    void testPredefined_SharedOtp() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What should I do if I shared an OTP?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertEquals("OTP_DISCLOSURE", response.getIntent());
        assertTrue(response.getReply().contains("1930"));
    }

    @Test
    @DisplayName("Regression: Predefined question - 'How do I dispute a UPI payment?'")
    void testPredefined_DisputeUpiPayment() {
        ChatRequest request = new ChatRequest();
        request.setMessage("How do I dispute a UPI payment?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertTrue("UPI_FRAUD".equals(response.getIntent()) || "FRAUD_REPORTING".equals(response.getIntent()) || "GENERAL_CYBER_SAFETY".equals(response.getIntent()));
        assertTrue(response.getReply().contains("1930") || response.getReply().toLowerCase().contains("utr"));
    }

    @Test
    @DisplayName("Regression: Predefined question - 'How do I block my debit or credit card?'")
    void testPredefined_BlockDebitOrCreditCard() {
        ChatRequest request = new ChatRequest();
        request.setMessage("How do I block my debit or credit card?");

        ChatResponse response = chatService.processChat(request);

        assertNotNull(response);
        assertTrue(response.getReply().toLowerCase().contains("card") || response.getReply().toLowerCase().contains("lock"));
    }

    // ==========================================
    // AI PROVIDER STATUS & ERROR HANDLING TESTS
    // ==========================================

    @Test
    @DisplayName("Provider Test: Provider configured but API key missing produces verified notice")
    void testProvider_MissingApiKey() {
        ChatService service = new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                "gemini",
                "", // missing key
                "",
                "gemini-1.5-flash",
                5000
        );

        ChatRequest request = new ChatRequest();
        request.setMessage("What is phishing?");

        ChatResponse response = service.processChat(request);

        assertNotNull(response);
        assertFalse(response.isLiveAi());
        assertEquals("gemini_unconfigured", response.getProvider());
        assertTrue(response.getReply().contains("Operating with verified Prahar rule guidance"));
    }

    @Test
    @DisplayName("Provider Test: Live AI Provider Rate Limit (HTTP 429) triggers safe fallback")
    void testProvider_RateLimitExceeded() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        ChatService service = new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                "gemini",
                "valid-test-key",
                endpoint,
                "gemini-1.5-flash",
                5000
        );
        service.setCustomRestClient(mockClient);

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"Resource has been exhausted (e.g. check quota).\"}"));

        ChatRequest request = new ChatRequest();
        request.setMessage("What is phishing?");

        ChatResponse response = service.processChat(request);

        server.verify();
        assertNotNull(response);
        assertFalse(response.isLiveAi());
        assertEquals("gemini_fallback", response.getProvider());
        assertTrue(response.getReply().contains("External AI provider (gemini) unavailable"));
        assertTrue(response.getReply().toLowerCase().contains("phishing"));
    }

    @Test
    @DisplayName("Provider Test: Successful live AI response returns isLiveAi=true and clean reply")
    void testProvider_SuccessfulLiveResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient mockClient = builder.build();

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        ChatService service = new ChatService(
                urlScanRepository,
                userRepository,
                transactionService,
                fraudIncidentService,
                "gemini",
                "valid-test-key",
                endpoint,
                "gemini-1.5-flash",
                5000
        );
        service.setCustomRestClient(mockClient);

        String mockResponseBody = "{" +
                "\"choices\": [{" +
                "\"message\": {\"role\": \"assistant\", \"content\": \"Phishing is an impersonation attack designed to deceive users into disclosing sensitive credentials.\"}," +
                "\"finish_reason\": \"stop\"" +
                "}]}";

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        ChatRequest request = new ChatRequest();
        request.setMessage("What is phishing?");

        ChatResponse response = service.processChat(request);

        server.verify();
        assertNotNull(response);
        assertTrue(response.isLiveAi());
        assertEquals("gemini", response.getProvider());
        assertEquals("Phishing is an impersonation attack designed to deceive users into disclosing sensitive credentials.", response.getReply());
    }
}
