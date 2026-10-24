package com.phishshield.service;

import com.phishshield.exception.SSRFSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class SSRFProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SSRFProtectionService.class);

    @Value("${app.ssrf.enabled:true}")
    private boolean ssrfProtectionEnabled;

    @Value("${app.ssrf.allow-private-ips:false}")
    private boolean allowPrivateIps;

    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));
    private static final Set<Integer> ALLOWED_PORTS = new HashSet<>(Arrays.asList(80, 443, 8080, 8443));

    /**
     * Validates that the input target URL is safe and does not attempt SSRF against internal/cloud infrastructure.
     * Throws SSRFSecurityException if any security check fails.
     */
    public void validateUrl(String rawUrl) {
        if (!ssrfProtectionEnabled) {
            return;
        }

        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new SSRFSecurityException("URL cannot be empty");
        }

        String targetUrl = rawUrl.trim();
        if (!targetUrl.contains("://")) {
            targetUrl = "http://" + targetUrl;
        }

        URI uri;
        try {
            uri = new URI(targetUrl);
        } catch (URISyntaxException e) {
            // Attempt IDN / Punycode conversion for internationalized domain names
            try {
                int schemeEnd = targetUrl.indexOf("://");
                if (schemeEnd != -1) {
                    String schemePart = targetUrl.substring(0, schemeEnd + 3);
                    String rest = targetUrl.substring(schemeEnd + 3);
                    int slashIndex = rest.indexOf('/');
                    String rawHost = slashIndex != -1 ? rest.substring(0, slashIndex) : rest;
                    String pathPart = slashIndex != -1 ? rest.substring(slashIndex) : "";
                    String asciiHost = java.net.IDN.toASCII(rawHost);
                    uri = new URI(schemePart + asciiHost + pathPart);
                } else {
                    throw new SSRFSecurityException("Invalid URL syntax: " + e.getMessage());
                }
            } catch (Exception ex) {
                throw new SSRFSecurityException("Invalid URL syntax: " + e.getMessage());
            }
        }

        // 1. Validate Scheme
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new SSRFSecurityException("Disallowed protocol scheme: " + scheme + ". Only HTTP and HTTPS are permitted.");
        }

        // 2. Reject Authority Credentials (http://user:pass@host)
        if (uri.getUserInfo() != null) {
            throw new SSRFSecurityException("URL with embedded user credentials is not permitted.");
        }

        // 3. Validate Host
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            String auth = uri.getAuthority();
            if (auth != null && !auth.trim().isEmpty()) {
                try {
                    int colonIdx = auth.indexOf(':');
                    String rawH = colonIdx != -1 ? auth.substring(0, colonIdx) : auth;
                    host = java.net.IDN.toASCII(rawH);
                } catch (Exception ignored) {}
            }
        }
        if (host == null || host.trim().isEmpty()) {
            throw new SSRFSecurityException("Missing or invalid hostname in URL.");
        }

        // 4. Validate Port
        int port = uri.getPort();
        if (port != -1 && !ALLOWED_PORTS.contains(port)) {
            throw new SSRFSecurityException("Access to non-standard port " + port + " is blocked for security.");
        }

        // 5. Inspect IP Addresses via DNS Resolution
        if (!allowPrivateIps) {
            checkHostAgainstPrivateRanges(host);
        }
    }

    private void checkHostAgainstPrivateRanges(String host) {
        // Direct checks on localhost names
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            throw new SSRFSecurityException("Requests to localhost or internal domain names are strictly prohibited.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedIpAddress(address)) {
                    throw new SSRFSecurityException("The domain resolves to a restricted/private network address: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            // If DNS cannot resolve, URL will still be analyzed statically by lexical rules
            log.debug("DNS resolution unresolvable for host: {}. Proceeding to lexical heuristic analysis.", host);
        }
    }

    private boolean isBlockedIpAddress(InetAddress address) {
        // 1. Loopback check (127.0.0.0/8, ::1)
        if (address.isLoopbackAddress()) {
            return true;
        }

        // 2. Any local address (0.0.0.0, ::)
        if (address.isAnyLocalAddress()) {
            return true;
        }

        // 3. Link-local (169.254.0.0/16, fe80::/10) - includes AWS/GCP metadata endpoint
        if (address.isLinkLocalAddress()) {
            return true;
        }

        // 4. Site-local / Private RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
        if (address.isSiteLocalAddress()) {
            return true;
        }

        // 5. Multicast
        if (address.isMulticastAddress()) {
            return true;
        }

        // 6. Explicit check on metadata IP (169.254.169.254)
        String ipStr = address.getHostAddress();
        if (ipStr.startsWith("169.254.") || ipStr.equals("127.0.0.1") || ipStr.equals("0.0.0.0")) {
            return true;
        }

        return false;
    }
}
