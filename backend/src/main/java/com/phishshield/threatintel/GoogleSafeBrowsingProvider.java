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
import java.util.*;

@Component
public class GoogleSafeBrowsingProvider implements ThreatIntelligenceProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleSafeBrowsingProvider.class);
    private static final String API_ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleSafeBrowsingProvider(
            @Value("${app.threat-intel.google-api-key:}") String apiKey,
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
        return "GoogleSafeBrowsing";
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

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            Map<String, String> clientInfo = new LinkedHashMap<>();
            clientInfo.put("clientId", "prahar-security");
            clientInfo.put("clientVersion", "1.0.0");
            requestBody.put("client", clientInfo);

            Map<String, Object> threatInfo = new LinkedHashMap<>();
            threatInfo.put("threatTypes", Arrays.asList("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"));
            threatInfo.put("platformTypes", Collections.singletonList("ANY_PLATFORM"));
            threatInfo.put("threatEntryTypes", Collections.singletonList("URL"));
            threatInfo.put("threatEntries", Collections.singletonList(Collections.singletonMap("url", url)));
            requestBody.put("threatInfo", threatInfo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String requestUrl = API_ENDPOINT + apiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode matches = root.get("matches");
                if (matches != null && matches.isArray() && matches.size() > 0) {
                    JsonNode firstMatch = matches.get(0);
                    String threatType = firstMatch.path("threatType").asText("MALICIOUS");
                    String platformType = firstMatch.path("platformType").asText("ALL");
                    String details = "Flagged by Google Safe Browsing as " + threatType + " (" + platformType + ").";
                    return ThreatIntelReport.malicious(getProviderName(), threatType, 95, details, false);
                } else {
                    return ThreatIntelReport.clean(getProviderName(), "No active threat matches found in Google Safe Browsing lists.", false);
                }
            }
            return ThreatIntelReport.clean(getProviderName(), "Google Safe Browsing returned empty matches.", false);

        } catch (ResourceAccessException e) {
            log.warn("Google Safe Browsing query timed out or connection failed: {}", e.getMessage());
            return ThreatIntelReport.failure(getProviderName(), "Request timed out or connection failed.");
        } catch (RestClientResponseException e) {
            log.warn("Google Safe Browsing returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ThreatIntelReport.failure(getProviderName(), "HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.warn("Error querying Google Safe Browsing: {}", e.getMessage());
            return ThreatIntelReport.failure(getProviderName(), e.getMessage());
        }
    }
}
