package com.phishshield;

import com.phishshield.dto.request.ChatMessageDto;
import com.phishshield.dto.request.ChatRequest;
import com.phishshield.dto.response.ChatResponse;
import com.phishshield.nlp.NlpIntent;
import com.phishshield.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(null, null, null, null);
        ReflectionTestUtils.setField(chatService, "aiProvider", "none");
        ReflectionTestUtils.setField(chatService, "aiApiKey", "");
    }

    @Test
    @DisplayName("Conversation 1: Multi-turn Unknown Debit -> UPI -> OTP disclosure escalation")
    void testMultiTurnDebitToUpiToOtp() {
        // Turn 1: User reports debit
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("₹5,000 was debited from my account");

        ChatResponse res1 = chatService.processChat(turn1);
        assertEquals("UNKNOWN_DEBIT", res1.getIntent());
        assertTrue(res1.getReply().contains("5000"));
        assertTrue(res1.getReply().toLowerCase().contains("payment method") || res1.getReply().toLowerCase().contains("channel"));

        // Turn 2: User specifies channel as "UPI"
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("UPI");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "₹5,000 was debited from my account"),
                new ChatMessageDto("assistant", res1.getReply())
        ));

        ChatResponse res2 = chatService.processChat(turn2);
        assertEquals("UPI_FRAUD", res2.getIntent());
        assertTrue(res2.getReply().contains("UTR") || res2.getReply().contains("UPI"));

        // Turn 3: User follow-up: "What if I shared the OTP too?"
        ChatRequest turn3 = new ChatRequest();
        turn3.setMessage("What if I shared the OTP too?");
        turn3.setConversationHistory(List.of(
                new ChatMessageDto("user", "₹5,000 was debited from my account"),
                new ChatMessageDto("assistant", res1.getReply()),
                new ChatMessageDto("user", "UPI"),
                new ChatMessageDto("assistant", res2.getReply())
        ));

        ChatResponse res3 = chatService.processChat(turn3);
        assertEquals("OTP_DISCLOSURE", res3.getIntent());
        assertTrue(res3.getReply().contains("1930"));
        assertTrue(res3.getReply().toLowerCase().contains("freeze"));
        assertNotNull(res3.getActionChecklist());
        assertFalse(res3.getActionChecklist().isEmpty());
    }

    @Test
    @DisplayName("Conversation 2: Phishing explanation -> Credential disclosure follow-up")
    void testMultiTurnPhishingToCredDisclosure() {
        // Turn 1: Explain scan
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("Why was this link flagged?");
        turn1.setScanUrl("http://secure-login-hdfcbank.account-verify.tk");
        turn1.setScanRiskScore(86);
        turn1.setScanRiskLevel("HIGH_RISK");
        turn1.setScanThreatCategory("CREDENTIAL_HARVESTING");
        turn1.setScanExplanation("Mimics official HDFC Bank login pages on a high-risk free domain.");

        ChatResponse res1 = chatService.processChat(turn1);
        assertEquals("EXPLAIN_SCAN", res1.getIntent());
        assertTrue(res1.getReply().contains("secure-login-hdfcbank.account-verify.tk"));
        assertTrue(res1.getReply().contains("86 / 100"));

        // Turn 2: User follow-up: I clicked it and typed my password
        ChatRequest turn2 = new ChatRequest();
        turn2.setMessage("I clicked the link and entered my password");
        turn2.setConversationHistory(List.of(
                new ChatMessageDto("user", "Why was this link flagged?"),
                new ChatMessageDto("assistant", res1.getReply())
        ));

        ChatResponse res2 = chatService.processChat(turn2);
        assertEquals("OTP_DISCLOSURE", res2.getIntent());
        assertTrue(res2.getReply().toLowerCase().contains("freeze"));
    }

    @Test
    @DisplayName("Conversation 3: Stolen Phone incident & follow-up")
    void testMultiTurnStolenPhone() {
        ChatRequest turn1 = new ChatRequest();
        turn1.setMessage("My mobile phone was stolen on the bus");

        ChatResponse res1 = chatService.processChat(turn1);
        assertEquals("STOLEN_PHONE", res1.getIntent());
        assertTrue(res1.getReply().contains("CEIR") || res1.getReply().contains("ceir.gov.in"));
        assertTrue(res1.getReply().contains("SIM"));
    }

    @Test
    @DisplayName("Conversation 4: Unrelated questions gracefully handled without fabrication")
    void testUnrelatedQueryHandling() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is the capital of France?");

        ChatResponse response = chatService.processChat(request);
        assertEquals(NlpIntent.UNRELATED_QUERY.name(), response.getIntent());
        assertTrue(response.getReply().contains("specialized in cybersecurity"));
        assertFalse(response.isLiveAi());
    }

    @Test
    @DisplayName("Conversation 5: Ambiguous input with focused clarification")
    void testAmbiguousQueryHandling() {
        ChatRequest request = new ChatRequest();
        request.setMessage("help");

        ChatResponse response = chatService.processChat(request);
        assertEquals("AMBIGUOUS_QUERY", response.getIntent());
        assertTrue(response.getReply().contains("Prahar AI Assistant"));
        assertNotNull(response.getFollowUpSuggestions());
        assertFalse(response.getFollowUpSuggestions().isEmpty());
    }

    @Test
    @DisplayName("Conversation 6: Redaction of sensitive credit/debit card numbers in memory")
    void testSensitiveCardNumberRedaction() {
        ChatRequest request = new ChatRequest();
        request.setMessage("My card 4532 1234 5678 9012 was charged without an OTP");

        ChatResponse response = chatService.processChat(request);
        assertNotNull(response);
        // Ensure raw 16-digit card is NOT printed back in the response
        assertFalse(response.getReply().contains("4532 1234 5678 9012"));
    }

    @Test
    @DisplayName("Test AI Provider Status reporting without leaking credentials")
    void testAiProviderStatusReporting() {
        Map<String, Object> status = chatService.getAiProviderStatus();
        assertNotNull(status);
        assertEquals("none", status.get("provider"));
        assertEquals(false, status.get("configured"));
        assertEquals("RULE_BASED_SAFETY_ENGINE", status.get("activeMode"));
        assertNotNull(status.get("configurationGuide"));
        // Confirm no raw api key field exists in output
        assertNull(status.get("apiKey"));
    }
}
