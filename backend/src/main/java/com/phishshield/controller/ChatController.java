package com.phishshield.controller;

import com.phishshield.dto.request.ChatRequest;
import com.phishshield.dto.request.CreateConversationRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.ChatConversationDto;
import com.phishshield.dto.response.ChatResponse;
import com.phishshield.exception.BadRequestException;
import com.phishshield.service.ChatHistoryService;
import com.phishshield.service.ChatService;
import com.phishshield.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI Chat Assistant", description = "Interactive conversational cyber safety assistant for threat guidance, scan explanation, and persistent history")
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;

    public ChatController(ChatService chatService, ChatHistoryService chatHistoryService) {
        this.chatService = chatService;
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping
    @Operation(summary = "Ask Prahar AI Assistant a question or get explanation for a scan (Public & Authenticated)")
    public ResponseEntity<ApiResponse<ChatResponse>> askPrahar(@Valid @RequestBody ChatRequest request) {
        Optional<Long> userIdOpt = SecurityUtils.getCurrentUserId();

        Long conversationId = request.getConversationId();

        // If authenticated and no conversationId specified, create a new conversation thread
        if (userIdOpt.isPresent() && conversationId == null) {
            ChatConversationDto created = chatHistoryService.createConversation(userIdOpt.get(), "New Conversation");
            conversationId = created.getId();
        }

        // Persist user turn if authenticated
        if (userIdOpt.isPresent() && conversationId != null) {
            chatHistoryService.appendMessage(
                    conversationId,
                    userIdOpt.get(),
                    "user",
                    request.getMessage(),
                    null,
                    false,
                    null
            );
        }

        ChatResponse response = chatService.processChat(request);

        // Persist assistant reply if authenticated
        if (userIdOpt.isPresent() && conversationId != null) {
            chatHistoryService.appendMessage(
                    conversationId,
                    userIdOpt.get(),
                    "assistant",
                    response.getReply(),
                    response.getProvider(),
                    response.isLiveAi(),
                    response.getIntent()
            );
            response.setConversationId(conversationId);
        }

        return ResponseEntity.ok(ApiResponse.success("Response generated successfully", response));
    }

    @PostMapping("/message")
    @Operation(summary = "Alias for askPrahar (/api/chat/message)")
    public ResponseEntity<ApiResponse<ChatResponse>> askPraharAlias(@Valid @RequestBody ChatRequest request) {
        return askPrahar(request);
    }

    @GetMapping("/status")
    @Operation(summary = "Safe health and provider configuration check without exposing credentials (Public)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @RequestParam(value = "probe", required = false, defaultValue = "false") boolean probe) {
        Map<String, Object> status = chatService.getAiProviderStatus(probe);
        return ResponseEntity.ok(ApiResponse.success("Chatbot service status", status));
    }

    // --- Authenticated Conversation History Endpoints ---

    @GetMapping("/conversations")
    @Operation(summary = "List all persistent chat conversations for the current authenticated user (Authenticated)")
    public ResponseEntity<ApiResponse<Page<ChatConversationDto>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Authentication required to access conversation history"));

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ChatConversationDto> conversations = chatHistoryService.getUserConversations(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Create a new conversation thread for the authenticated user (Authenticated)")
    public ResponseEntity<ApiResponse<ChatConversationDto>> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Authentication required to create a conversation"));

        ChatConversationDto conv = chatHistoryService.createConversation(userId, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success("Conversation created successfully", conv));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get a conversation and its paginated messages (Owner only)")
    public ResponseEntity<ApiResponse<ChatConversationDto>> getConversation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Authentication required to view this conversation"));

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        ChatConversationDto conv = chatHistoryService.getConversationWithMessages(id, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully", conv));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete a conversation and its messages (Owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Authentication required to delete this conversation"));

        chatHistoryService.deleteConversation(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }
}
