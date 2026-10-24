package com.phishshield.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class UrlNormalizationUtil {

    private UrlNormalizationUtil() {
        // Utility class
    }

    /**
     * Normalizes a given URL for consistent hashing and storage.
     */
    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "";
        }

        String cleaned = rawUrl.trim();
        // Add default protocol if absent for parsing
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://" + cleaned;
        }

        try {
            URI uri = new URI(cleaned);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";

            // Strip trailing slash if path is just "/"
            if ("/".equals(path)) {
                path = "";
            }

            // Remove www. prefix for canonical domain matching
            String normalizedHost = host.startsWith("www.") ? host.substring(4) : host;

            return normalizedHost + path + query;
        } catch (URISyntaxException e) {
            // Fallback string manipulation
            String stripped = rawUrl.toLowerCase(Locale.ROOT)
                    .replaceFirst("^https?://", "")
                    .replaceFirst("^www\\.", "");
            if (stripped.endsWith("/")) {
                stripped = stripped.substring(0, stripped.length() - 1);
            }
            return stripped;
        }
    }

    /**
     * Extracts the host domain name from a URL.
     */
    public static String extractDomain(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "";
        }

        String cleaned = rawUrl.trim();
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://" + cleaned;
        }

        try {
            URI uri = new URI(cleaned);
            String host = uri.getHost();
            if (host != null) {
                return host.toLowerCase(Locale.ROOT);
            }
        } catch (URISyntaxException ignored) {
            // Fallback below
        }

        // Fallback string extraction
        String domain = rawUrl.toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "");
        int slashIdx = domain.indexOf('/');
        if (slashIdx != -1) {
            domain = domain.substring(0, slashIdx);
        }
        int colonIdx = domain.indexOf(':');
        if (colonIdx != -1) {
            domain = domain.substring(0, colonIdx);
        }
        try {
            return java.net.IDN.toASCII(domain);
        } catch (Exception e) {
            return domain;
        }
    }
}
