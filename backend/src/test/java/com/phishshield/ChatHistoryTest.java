package com.phishshield;

import com.phishshield.domain.Role;
import com.phishshield.domain.RoleName;
import com.phishshield.domain.User;
import com.phishshield.dto.response.ChatConversationDto;
import com.phishshield.dto.response.ChatMessageItemDto;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.ChatConversationRepository;
import com.phishshield.repository.ChatMessageRepository;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatHistoryTest {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("User role").build()));

        userA = userRepository.findByUsername("history_user_a")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("history_user_a")
                        .email("user_a@prahar.internal")
                        .passwordHash("hashed")
                        .role(userRole)
                        .build()));

        userB = userRepository.findByUsername("history_user_b")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("history_user_b")
                        .email("user_b@prahar.internal")
                        .passwordHash("hashed")
                        .role(userRole)
                        .build()));
    }

    @Test
    @DisplayName("Redaction: Scrubs card numbers, OTPs, PINs, and Bearer tokens before saving")
    void testSensitiveDataRedaction() {
        String sensitiveInput = "My card is 4532 0123 4567 8910 and my OTP is 492810. Bearer eyJhbGciOi...";
        String redacted = chatHistoryService.redactSensitiveData(sensitiveInput);

        assertFalse(redacted.contains("4532 0123 4567 8910"), "Card number must be redacted");
        assertTrue(redacted.contains("[REDACTED_CARD_NUMBER]"), "Card placeholder must be inserted");

        assertFalse(redacted.contains("492810"), "OTP number must be redacted");
        assertTrue(redacted.contains("[REDACTED_SECRET]"), "OTP placeholder must be inserted");

        assertFalse(redacted.contains("eyJhbGciOi"), "Bearer token must be redacted");
        assertTrue(redacted.contains("Bearer [REDACTED]"), "Bearer placeholder must be inserted");
    }

    @Test
    @DisplayName("Authorization & Isolation: User B cannot access or modify User A's conversation")
    void testConversationOwnershipIsolation() {
        // User A creates conversation
        ChatConversationDto convA = chatHistoryService.createConversation(userA.getId(), "User A Private Fraud Inquiry");
        assertNotNull(convA.getId());

        chatHistoryService.appendMessage(
                convA.getId(),
                userA.getId(),
                "user",
                "I was debited Rs. 5000",
                "prahar_nlp",
                false,
                "UNKNOWN_DEBIT"
        );

        // User B attempts to read User A's conversation -> must be denied with ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            chatHistoryService.getConversationWithMessages(convA.getId(), userB.getId(), PageRequest.of(0, 10));
        });

        // User B attempts to append a message to User A's conversation -> must be denied
        assertThrows(ResourceNotFoundException.class, () -> {
            chatHistoryService.appendMessage(
                    convA.getId(),
                    userB.getId(),
                    "user",
                    "Unauthorized message injection",
                    null,
                    false,
                    null
            );
        });

        // User B attempts to delete User A's conversation -> must be denied
        assertThrows(ResourceNotFoundException.class, () -> {
            chatHistoryService.deleteConversation(convA.getId(), userB.getId());
        });

        // Verify conversation still exists and is untouched
        ChatConversationDto verified = chatHistoryService.getConversationWithMessages(convA.getId(), userA.getId(), PageRequest.of(0, 10));
        assertEquals(1, verified.getMessageCount());
    }

    @Test
    @DisplayName("Pagination & History: Messages and conversations support pagination")
    void testConversationPagination() {
        ChatConversationDto conv = chatHistoryService.createConversation(userA.getId(), "Thread with multiple messages");

        for (int i = 1; i <= 5; i++) {
            chatHistoryService.appendMessage(
                    conv.getId(),
                    userA.getId(),
                    "user",
                    "Message number " + i,
                    null,
                    false,
                    "GENERAL_CYBER_SAFETY"
            );
        }

        // Fetch page 0 with size 2
        ChatConversationDto page0 = chatHistoryService.getConversationWithMessages(conv.getId(), userA.getId(), PageRequest.of(0, 2));
        assertEquals(2, page0.getMessages().size());
        assertEquals("Message number 1", page0.getMessages().get(0).getContent());
        assertEquals(5, page0.getMessageCount());

        // Fetch page 1 with size 2
        ChatConversationDto page1 = chatHistoryService.getConversationWithMessages(conv.getId(), userA.getId(), PageRequest.of(1, 2));
        assertEquals(2, page1.getMessages().size());
        assertEquals("Message number 3", page1.getMessages().get(0).getContent());
    }

    @Test
    @DisplayName("Deletion: Deleting a conversation cascades and deletes all associated messages")
    void testCascadeDeletion() {
        ChatConversationDto conv = chatHistoryService.createConversation(userA.getId(), "Temporary Threat Check");

        chatHistoryService.appendMessage(conv.getId(), userA.getId(), "user", "Question 1", null, false, null);
        chatHistoryService.appendMessage(conv.getId(), userA.getId(), "assistant", "Answer 1", "gemini", true, null);

        assertEquals(2, messageRepository.countByConversation(conversationRepository.findById(conv.getId()).get()));

        chatHistoryService.deleteConversation(conv.getId(), userA.getId());

        // Verify conversation is gone
        assertFalse(conversationRepository.findById(conv.getId()).isPresent());
        // Verify messages were cascaded
        assertEquals(0, messageRepository.count());
    }
}
