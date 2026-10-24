package com.phishshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.dto.response.MLPredictionResult;
import com.phishshield.dto.response.MLServiceHealthDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class MLServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MLServiceClient.class);

    private RestTemplate restTemplate;
    private final String mlServiceUrl;
    private final long timeoutMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MLServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.ml-service.url:http://localhost:8000}") String mlServiceUrl,
            @Value("${app.ml-service.timeout-ms:3000}") long timeoutMs) {
        this.mlServiceUrl = mlServiceUrl != null ? mlServiceUrl.replaceAll("/+$", "") : "http://localhost:8000";
        this.timeoutMs = timeoutMs;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    /**
     * Setter for injecting mocked RestTemplate during unit and integration tests.
     */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getMlServiceUrl() {
        return mlServiceUrl;
    }

    /**
     * Checks operational health and model loading status of the FastAPI service.
     */
    public MLServiceHealthDto checkHealth() {
        long start = System.currentTimeMillis();
        String endpoint = mlServiceUrl + "/health";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            long latency = System.currentTimeMillis() - start;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                MLServiceHealthDto dto = new MLServiceHealthDto();
                dto.setStatus(root.path("status").asText("UP"));
                dto.setService(root.path("service").asText("prahar-ml-service"));
                dto.setModelLoaded(root.path("model_loaded").asBoolean(false));
                dto.setModelVersion(root.path("model_version").isNull() ? null : root.path("model_version").asText());
                dto.setModelName(root.path("model_name").isNull() ? null : root.path("model_name").asText());
                dto.setLatencyMs(latency);
                dto.setServiceUrl(mlServiceUrl);
                return dto;
            } else {
                return MLServiceHealthDto.down(mlServiceUrl, "HTTP " + response.getStatusCode().value() + " received from health check");
            }
        } catch (ResourceAccessException e) {
            log.debug("ML Service health probe timed out or connection refused: {}", e.getMessage());
            return MLServiceHealthDto.down(mlServiceUrl, "Service unreachable or timed out (" + e.getMessage() + ")");
        } catch (Exception e) {
            log.debug("ML Service health probe failed: {}", e.getMessage());
            return MLServiceHealthDto.down(mlServiceUrl, e.getMessage());
        }
    }

    /**
     * Executes real-time ML prediction with schema validation and error attribution.
     *
     * @param rawUrl target URL to inspect
     * @return typed MLPredictionResult with availability flag and failure reason if unavailable
     */
    public MLPredictionResult predictUrlTyped(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return MLPredictionResult.failure("EMPTY_URL: Target URL cannot be empty");
        }

        String sanitizedUrl = rawUrl.trim();
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "http://" + sanitizedUrl;
        }

        String endpoint = mlServiceUrl + "/predict";
        long start = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestPayload = Collections.singletonMap("url", sanitizedUrl);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePredictionJson(response.getBody(), sanitizedUrl, System.currentTimeMillis() - start);
            } else {
                return MLPredictionResult.failure("UNEXPECTED_STATUS: HTTP " + response.getStatusCode().value());
            }

        } catch (HttpServerErrorException.ServiceUnavailable e) {
            // FastAPI returns 503 when model artifact is missing or not loaded
            String errorBody = e.getResponseBodyAsString();
            log.warn("FastAPI ML service reports MODEL_UNAVAILABLE: {}", errorBody);
            return MLPredictionResult.failure("MODEL_UNAVAILABLE: ML model artifact is not loaded in microservice.");

        } catch (HttpClientErrorException e) {
            log.warn("FastAPI returned client validation error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return MLPredictionResult.failure("CLIENT_ERROR_" + e.getStatusCode().value() + ": " + e.getStatusText());

        } catch (ResourceAccessException e) {
            log.debug("ML Service connection unreachable or read timed out after {}ms: {}", timeoutMs, e.getMessage());
            return MLPredictionResult.failure("SERVICE_UNAVAILABLE: Could not connect to ML service at " + mlServiceUrl + " (" + e.getMessage() + ")");

        } catch (Exception e) {
            log.warn("Unexpected failure communicating with ML service: {}", e.getMessage());
            return MLPredictionResult.failure("INFERENCE_ERROR: " + e.getMessage());
        }
    }

    /**
     * Backward-compatible predictUrl method returning Map or null.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> predictUrl(String url) {
        MLPredictionResult typed = predictUrlTyped(url);
        if (typed.isAvailable()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("success", true);
            map.put("url", typed.getUrl());
            map.put("prediction", typed.getPrediction());
            map.put("risk_level", typed.getRiskLevel());
            map.put("risk_score", typed.getRiskScore());
            map.put("ml_probability", typed.getMlProbability());
            map.put("confidence", typed.getConfidence());
            map.put("threat_category", typed.getThreatCategory());
            map.put("indicators", typed.getIndicators());
            map.put("explanation", typed.getExplanations());
            map.put("model", Map.of(
                    "name", typed.getModelName() != null ? typed.getModelName() : "PhishShield-ML",
                    "version", typed.getModelVersion() != null ? typed.getModelVersion() : "1.0.0"
            ));
            map.put("execution_time_ms", typed.getExecutionTimeMs());
            return map;
        }
        return null;
    }

    private MLPredictionResult parsePredictionJson(String json, String url, long fallbackLatencyMs) {
        try {
            JsonNode root = objectMapper.readTree(json);
            MLPredictionResult result = new MLPredictionResult();
            result.setAvailable(true);
            result.setUrl(root.path("url").asText(url));
            result.setPrediction(root.path("prediction").asText("UNKNOWN"));
            result.setRiskLevel(root.path("risk_level").asText("SAFE"));
            result.setRiskScore(root.path("risk_score").asInt(0));
            result.setMlProbability(root.path("ml_probability").asDouble(0.0));
            result.setConfidence(root.path("confidence").asDouble(0.5));
            result.setThreatCategory(root.path("threat_category").asText("BENIGN"));

            // Parse explanations list
            List<String> explanations = new ArrayList<>();
            JsonNode expNode = root.path("explanation");
            if (expNode.isArray()) {
                for (JsonNode item : expNode) {
                    explanations.add(item.asText());
                }
            }
            result.setExplanations(explanations);

            // Parse model metadata
            JsonNode modelNode = root.path("model");
            result.setModelName(modelNode.path("name").asText("Logistic Regression"));
            result.setModelVersion(modelNode.path("version").asText("1.0.0"));

            // Execution latency
            double execTime = root.path("execution_time_ms").asDouble(fallbackLatencyMs);
            result.setExecutionTimeMs(execTime);

            return result;
        } catch (Exception e) {
            log.error("Failed to parse ML microservice prediction payload: {}", e.getMessage());
            return MLPredictionResult.failure("MALFORMED_RESPONSE: " + e.getMessage());
        }
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
