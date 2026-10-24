package com.phishshield.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiting filter protecting sensitive endpoints:
 * 1. Authentication brute-force prevention on /api/auth/login (15 req/min)
 * 2. Scanning abuse prevention on /api/scans/** (60 req/min)
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final boolean enabled;
    private final int authLimitPerMin;
    private final int scanLimitPerMin;

    private final Map<String, WindowCounter> requestWindows = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.auth-limit-per-min:15}") int authLimitPerMin,
            @Value("${app.security.rate-limit.scan-limit-per-min:60}") int scanLimitPerMin) {
        this.enabled = enabled;
        this.authLimitPerMin = authLimitPerMin;
        this.scanLimitPerMin = scanLimitPerMin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);

        // 1. Throttle /api/auth/login against brute force
        if ("/api/auth/login".equalsIgnoreCase(path) && "POST".equalsIgnoreCase(method)) {
            if (isRateLimited(clientIp + ":AUTH", authLimitPerMin)) {
                log.warn("Rate limit exceeded for IP {} on /api/auth/login", clientIp);
                respondRateLimited(response, "Too many authentication attempts. Please wait 60 seconds before retrying.");
                return;
            }
        }

        // 2. Throttle POST /api/scans/** against abuse and DoS
        if (path.startsWith("/api/scans/") && "POST".equalsIgnoreCase(method)) {
            if (isRateLimited(clientIp + ":SCAN", scanLimitPerMin)) {
                log.warn("Rate limit exceeded for IP {} on scanning endpoints", clientIp);
                respondRateLimited(response, "Scan rate limit exceeded. Please wait 60 seconds before submitting additional scans.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key, int maxRequestsPerMinute) {
        long currentMinuteEpoch = System.currentTimeMillis() / 60000;
        String windowKey = key + ":" + currentMinuteEpoch;

        WindowCounter counter = requestWindows.compute(windowKey, (k, existing) -> {
            if (existing == null) {
                return new WindowCounter();
            }
            existing.increment();
            return existing;
        });

        // Periodic cleanup of stale windows
        if (requestWindows.size() > 5000) {
            requestWindows.entrySet().removeIf(entry -> {
                String[] parts = entry.getKey().split(":");
                try {
                    long windowEpoch = Long.parseLong(parts[parts.length - 1]);
                    return windowEpoch < currentMinuteEpoch;
                } catch (Exception e) {
                    return true;
                }
            });
        }

        return counter.getCount() > maxRequestsPerMinute;
    }

    private void respondRateLimited(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        String json = String.format("{\"success\":false,\"message\":\"%s\",\"status\":429}", message);
        response.getWriter().write(json);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    public void clearWindows() {
        requestWindows.clear();
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(1);

        void increment() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }
    }
}
