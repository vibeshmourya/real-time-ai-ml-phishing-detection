package com.phishshield.util;

import com.phishshield.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) auth.getPrincipal());
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentUserPrincipal().map(UserPrincipal::getUsername);
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "UNKNOWN";
    }
}
