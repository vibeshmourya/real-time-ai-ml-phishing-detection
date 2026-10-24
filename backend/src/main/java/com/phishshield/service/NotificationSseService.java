package com.phishshield.service;

import com.phishshield.dto.response.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Events (SSE) client subscriptions with strict user isolation,
 * resource bounding, keep-alive heartbeats, and graceful disconnection cleanup.
 */
@Service
public class NotificationSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes
    private static final int MAX_EMITTERS_PER_USER = 5;

    // Concurrent map keyed strictly by authenticated userId
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * Subscribes an authenticated user to their real-time notification event stream.
     *
     * @param userId authenticated user ID
     * @return SseEmitter bound to the user
     */
    public SseEmitter subscribe(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null for SSE subscription");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        List<SseEmitter> emitters = userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // Bound active emitters per user to avoid memory exhaustion
        if (emitters.size() >= MAX_EMITTERS_PER_USER) {
            SseEmitter oldest = emitters.remove(0);
            try {
                oldest.complete();
            } catch (Exception ignored) {}
        }

        emitters.add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = userEmitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    userEmitters.remove(userId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send initial connect handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Connected to Prahar real-time security alert stream."));
        } catch (IOException e) {
            cleanup.run();
        }

        return emitter;
    }

    /**
     * Dispatches a notification to all active SSE subscribers for a specific user.
     * Execution failure never rolls back the persistent database transaction.
     *
     * @param userId recipient user ID
     * @param notification notification data transfer object
     */
    public void sendToUser(Long userId, NotificationDto notification) {
        if (userId == null || notification == null) return;

        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("NOTIFICATION")
                        .id(String.valueOf(notification.getId()))
                        .data(notification));
            } catch (Exception e) {
                log.debug("Failed sending SSE to user {}: {}", userId, e.getMessage());
                emitters.remove(emitter);
            }
        }

        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }

    /**
     * Heartbeat keep-alive ping every 25 seconds to keep connections active through proxies.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (userEmitters.isEmpty()) return;

        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("PING")
                            .data("keep-alive"));
                } catch (Exception e) {
                    emitters.remove(emitter);
                }
            }
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        });
    }

    public int getActiveSubscriberCount(Long userId) {
        List<SseEmitter> list = userEmitters.get(userId);
        return list != null ? list.size() : 0;
    }
}
