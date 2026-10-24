package com.phishshield.threatintel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class VirusTotalProvider implements ThreatIntelligenceProvider {

    private static final Logger log = LoggerFactory.getLogger(VirusTotalProvider.class);
    private static final String DOMAIN_ENDPOINT = "https://www.virustotal.com/api/v3/domains/";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VirusTotalProvider(
            @Value("${app.threat-intel.virustotal-api-key:}") String apiKey,
            @Value("${app.threat-intel.timeout-ms:3000}") int timeoutMs,
            RestTemplateBuilder restTemplateBuilder) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public String getProviderName() {
        return "VirusTotal";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public ThreatIntelReport checkReputation(String url, String domain) {
        if (!isConfigured()) {
            return ThreatIntelReport.notConfigured(getProviderName());
        }

        if (domain == null || domain.isBlank()) {
            return ThreatIntelReport.failure(getProviderName(), "Invalid or missing domain.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apikey", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String requestUrl = DOMAIN_ENDPOINT + domain.toLowerCase();
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode stats = root.path("data").path("attributes").path("last_analysis_stats");
                int maliciousCount = stats.path("malicious").asInt(0);
                int suspiciousCount = stats.path("suspicious").asInt(0);
                int totalMalicious = maliciousCount + suspiciousCount;

                if (totalMalicious >= 2) {
                    int confidence = Math.min(99, 50 + (totalMalicious * 5));
                    String details = "VirusTotal flagged domain as malicious by " + totalMalicious + " security vendors.";
                    return ThreatIntelReport.malicious(getProviderName(), "SECURITY_VENDOR_DETECTION", confidence, details, false);
                } else {
                    return ThreatIntelReport.clean(getProviderName(), "VirusTotal reports 0-1 detections across security vendors.", false);
                }
            }
            return ThreatIntelReport.clean(getProviderName(), "VirusTotal returned clean report.", false);

        } catch (ResourceAccessException e) {
            log.warn("VirusTotal query timed out or connection failed: {}", e.getMessage());
            return ThreatIntelReport.failure(getProviderName(), "Request timed out or connection failed.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("VirusTotal rate limit / quota exceeded");
                return ThreatIntelReport.failure(getProviderName(), "Provider quota or rate limit exceeded.");
            }
            log.warn("VirusTotal returned HTTP {}: {}", e.getStatusCode(), e.getMessage());
            return ThreatIntelReport.failure(getProviderName(), "HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.warn("Error querying VirusTotal: {}", e.getMessage());
            return ThreatIntelReport.failure(getProviderName(), e.getMessage());
        }
    }
}
