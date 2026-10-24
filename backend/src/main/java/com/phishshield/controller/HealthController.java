package com.phishshield.controller;

import com.phishshield.dto.response.ApiResponse;
import com.phishshield.dto.response.MLServiceHealthDto;
import com.phishshield.service.ChatService;
import com.phishshield.service.MLServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "System & Health", description = "System entrypoint, directory, liveness, and readiness health check endpoints")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final MLServiceClient mlServiceClient;
    private final ChatService chatService;

    public HealthController(DataSource dataSource,
                            MLServiceClient mlServiceClient,
                            ChatService chatService) {
        this.dataSource = dataSource;
        this.mlServiceClient = mlServiceClient;
        this.chatService = chatService;
    }

    @GetMapping("/")
    @Operation(summary = "Platform Welcome & API Directory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rootEndpoint() {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("platform", "Prahar Real-Time AI/ML Cyber Defender");
        welcome.put("version", "2.0.0");
        welcome.put("status", "ONLINE");
        welcome.put("message", "Welcome to Prahar Cybersecurity API. Real-time Phishing Detection & Incident Safety.");

        Map<String, String> links = new HashMap<>();
        links.put("swagger_ui", "/swagger-ui/index.html");
        links.put("openapi_spec", "/v3/api-docs");
        links.put("health_check", "/api/health");
        links.put("readiness_check", "/api/health/readiness");
        links.put("scan_url", "/api/scans/url");
        links.put("scan_unified_image", "/api/scans/unified-image");
        links.put("auth_login", "/api/auth/login");
        links.put("active_alerts", "/api/alerts/active");
        links.put("notifications", "/api/notifications");

        welcome.put("endpoints", links);
        return ResponseEntity.ok(ApiResponse.success("Prahar Backend API is ready", welcome));
    }

    /**
     * Safe Liveness Probe (/api/health)
     * Verifies that the Spring Boot application context is alive and responding.
     * Does NOT execute heavy external queries or leak sensitive configurations.
     */
    @GetMapping("/api/health")
    @Operation(summary = "System liveness check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "prahar-backend");
        healthData.put("version", "2.0.0");
        healthData.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success("System is alive and operational", healthData));
    }

    /**
     * Safe Readiness Probe (/api/health/readiness)
     * Evaluates core dependencies:
     * 1. Database connectivity
     * 2. Python ML Microservice availability and model loading status
     * 3. AI Provider configuration state (without triggering paid API calls on every probe)
     */
    @GetMapping("/api/health/readiness")
    @Operation(summary = "Comprehensive system readiness check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readinessCheck(
            @RequestParam(defaultValue = "false") boolean probeAi) {

        Map<String, Object> details = new LinkedHashMap<>();
        boolean overallReady = true;

        // 1. Database Connectivity Probe
        Map<String, Object> dbStatus = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            dbStatus.put("status", valid ? "UP" : "DOWN");
            dbStatus.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            if (!valid) overallReady = false;
        } catch (Exception e) {
            log.warn("Database readiness check failed: {}", e.getMessage());
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", "Database connection could not be established");
            overallReady = false;
        }
        details.put("database", dbStatus);

        // 2. Python FastAPI ML Microservice Probe
        Map<String, Object> mlStatus = new LinkedHashMap<>();
        try {
            MLServiceHealthDto mlHealth = mlServiceClient.checkHealth();
            boolean isUp = "UP".equalsIgnoreCase(mlHealth.getStatus());
            mlStatus.put("status", mlHealth.getStatus());
            mlStatus.put("modelLoaded", mlHealth.isModelLoaded());
            mlStatus.put("modelVersion", mlHealth.getModelVersion());
            mlStatus.put("modelName", mlHealth.getModelName());
            if (!isUp) {
                overallReady = false;
            }
        } catch (Exception e) {
            mlStatus.put("status", "DEGRADED");
            mlStatus.put("modelLoaded", false);
            mlStatus.put("note", "Fallback heuristic rule engine active");
        }
        details.put("mlService", mlStatus);

        // 3. AI / Gemini Provider Status (Non-blocking, safe configuration inspection)
        try {
            Map<String, Object> aiStatus = chatService.getAiProviderStatus(probeAi);
            details.put("aiProvider", aiStatus);
        } catch (Exception e) {
            details.put("aiProvider", Map.of("status", "ERROR", "note", "Rule engine active fallback"));
        }

        details.put("overallStatus", overallReady ? "READY" : "NOT_READY");
        details.put("timestamp", System.currentTimeMillis());

        HttpStatus httpStatus = overallReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        ApiResponse<Map<String, Object>> responseBody = new ApiResponse<>(
                overallReady,
                overallReady ? "Prahar system is ready for traffic" : "Prahar dependencies not ready",
                details,
                java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(httpStatus).body(responseBody);
    }
}
