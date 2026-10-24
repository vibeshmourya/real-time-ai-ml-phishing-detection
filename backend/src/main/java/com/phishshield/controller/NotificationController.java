package com.phishshield.controller;

import com.phishshield.dto.response.NotificationDto;
import com.phishshield.dto.response.NotificationPreferenceDto;
import com.phishshield.dto.response.UnreadCountDto;
import com.phishshield.exception.UnauthorizedException;
import com.phishshield.service.NotificationService;
import com.phishshield.service.NotificationSseService;
import com.phishshield.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Real-Time Notifications", description = "Server-sent events, in-app notification centre, and user preferences")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final NotificationSseService sseService;

    public NotificationController(NotificationService notificationService,
                                  NotificationSseService sseService) {
        this.notificationService = notificationService;
        this.sseService = sseService;
    }

    private Long getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required to access notifications."));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user notifications", description = "Paginated list of alerts for the authenticated user, optionally filtered by read/unread status.")
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Returns the number of unread alerts for the authenticated user.")
    public ResponseEntity<UnreadCountDto> getUnreadCount() {
        Long userId = getAuthenticatedUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new UnreadCountDto(count));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read, enforcing user ownership.")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        NotificationDto updated = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications of the authenticated user as read.")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Long userId = getAuthenticatedUserId();
        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read", "updatedCount", updatedCount));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete notification", description = "Deletes a specific notification with ownership validation.")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification preferences", description = "Retrieves in-app, email, browser, and severity preferences.")
    public ResponseEntity<NotificationPreferenceDto> getPreferences() {
        Long userId = getAuthenticatedUserId();
        NotificationPreferenceDto prefs = notificationService.getUserPreferences(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update notification preferences", description = "Updates notification delivery and severity settings.")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(@RequestBody NotificationPreferenceDto request) {
        Long userId = getAuthenticatedUserId();
        NotificationPreferenceDto updated = notificationService.updateUserPreferences(userId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subscribe to real-time notification stream", description = "Connects an SSE stream for real-time alert pushes.")
    public SseEmitter subscribeToStream() {
        Long userId = getAuthenticatedUserId();
        log.info("Client subscribed to SSE notification stream for user {}", userId);
        return sseService.subscribe(userId);
    }
}
