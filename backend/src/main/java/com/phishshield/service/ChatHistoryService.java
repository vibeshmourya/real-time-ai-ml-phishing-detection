package com.phishshield.service;

import com.phishshield.domain.ChatConversation;
import com.phishshield.domain.ChatMessage;
import com.phishshield.domain.User;
import com.phishshield.dto.response.ChatConversationDto;
import com.phishshield.dto.response.ChatMessageItemDto;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.ChatConversationRepository;
import com.phishshield.repository.ChatMessageRepository;
import com.phishshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

    private static final Pattern SENSITIVE_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern SENSITIVE_OTP_PATTERN = Pattern.compile("(?i)\\b(?:otp|pin|cvv|code|password)\\s*(?:is|was|[:=])?\\s*(\\d{4,8})\\b");
    private static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("(?i)Bearer\\s+[a-zA-Z0-9_.-]+");
    private static final Pattern API_KEY_PARAM_PATTERN = Pattern.compile("(?i)(?:key|secret|token)=[a-zA-Z0-9_.-]+");

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatHistoryService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Redacts payment card numbers, OTPs, PINs, and auth secrets before persisting to DB.
     */
    public String redactSensitiveData(String text) {
        return com.phishshield.util.SensitiveDataScrubber.scrub(text);
    }

    @Transactional
    public ChatConversationDto createConversation(Long userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        String sanitizedTitle = title != null && !title.isBlank() ? title.trim() : "New Conversation";
        if (sanitizedTitle.length() > 150) {
            sanitizedTitle = sanitizedTitle.substring(0, 147) + "...";
        }

        ChatConversation conv = new ChatConversation(user, sanitizedTitle);
        ChatConversation saved = conversationRepository.save(conv);
        return new ChatConversationDto(saved.getId(), saved.getTitle(), saved.getIsPinned(), saved.getCreatedAt(), saved.getUpdatedAt(), 0);
    }

    @Transactional(readOnly = true)
    public Page<ChatConversationDto> getUserConversations(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        return conversationRepository.findByUserOrderByUpdatedAtDesc(user, pageable)
                .map(conv -> new ChatConversationDto(
                        conv.getId(),
                        conv.getTitle(),
                        conv.getIsPinned(),
                        conv.getCreatedAt(),
                        conv.getUpdatedAt(),
                        messageRepository.countByConversation(conv)
                ));
    }

    @Transactional(readOnly = true)
    public ChatConversationDto getConversationWithMessages(Long conversationId, Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        ChatConversation conv = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or access denied for id: " + conversationId));

        Page<ChatMessage> messagePage = messageRepository.findByConversationOrderByCreatedAtAsc(conv, pageable);
        List<ChatMessageItemDto> messageDtos = messagePage.getContent().stream()
                .map(m -> new ChatMessageItemDto(
                        m.getId(),
                        m.getRole(),
                        m.getContent(),
                        m.getProvider(),
                        m.getIsLiveAi(),
                        m.getIntent(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());

        ChatConversationDto dto = new ChatConversationDto(
                conv.getId(),
                conv.getTitle(),
                conv.getIsPinned(),
                conv.getCreatedAt(),
                conv.getUpdatedAt(),
                messagePage.getTotalElements()
        );
        dto.setMessages(messageDtos);
        return dto;
    }

    @Transactional
    public ChatMessageItemDto appendMessage(Long conversationId, Long userId, String role, String content, String provider, boolean isLiveAi, String intent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        ChatConversation conv = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or access denied for id: " + conversationId));

        String sanitizedContent = redactSensitiveData(content);

        // Cap maximum history per conversation to 100 messages
        long count = messageRepository.countByConversation(conv);
        if (count >= 100) {
            log.warn("Conversation {} reached maximum capacity of 100 messages.", conversationId);
            throw new BadRequestException("This conversation thread has reached its maximum message capacity (100). Please start a new conversation.");
        }

        // If title is default and this is first user message, update title
        if ("user".equalsIgnoreCase(role) && "New Conversation".equals(conv.getTitle())) {
            String snippet = sanitizedContent.replaceAll("\\r?\\n", " ");
            if (snippet.length() > 40) snippet = snippet.substring(0, 37) + "...";
            conv.setTitle(snippet);
        }

        ChatMessage message = new ChatMessage(conv, role, sanitizedContent, provider, isLiveAi, intent);
        ChatMessage saved = messageRepository.save(message);

        return new ChatMessageItemDto(
                saved.getId(),
                saved.getRole(),
                saved.getContent(),
                saved.getProvider(),
                saved.getIsLiveAi(),
                saved.getIntent(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found for id: " + userId));

        ChatConversation conv = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or access denied for id: " + conversationId));

        conversationRepository.delete(conv);
        log.info("Deleted chat conversation {} for user {}", conversationId, userId);
    }
}
