package com.phishshield.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class MLServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MLServiceClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.ml-service.url:http://localhost:8000}") String mlServiceUrl,
            @Value("${app.ml-service.timeout-ms:3000}") long timeoutMs) {
        this.mlServiceUrl = mlServiceUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    /**
     * Calls FastAPI ML microservice /predict endpoint
     * @param url target URL to predict
     * @return map with ML response or null on error
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> predictUrl(String url) {
        try {
            String endpoint = mlServiceUrl + "/predict";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestPayload = Collections.singletonMap("url", url);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.debug("ML Service inference unavailable (falling back to rule engine): {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fetches benchmark evaluations for models
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getModelEvaluations() {
        try {
            String endpoint = mlServiceUrl + "/model/evaluations";
            ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.debug("Could not fetch evaluations from ML service: {}", e.getMessage());
        }
        return new HashMap<>();
    }
}
