package com.phishshield.controller;

import com.phishshield.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "System & Health", description = "System entrypoint, directory, and health check endpoints")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Platform Welcome & API Directory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rootEndpoint() {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("platform", "PhishShield Real-Time AI/ML Cyber Defender");
        welcome.put("version", "1.0.0");
        welcome.put("status", "ONLINE");
        welcome.put("message", "Welcome to PhishShield Cybersecurity API. Use the links below to access documentation and endpoints.");

        Map<String, String> links = new HashMap<>();
        links.put("swagger_ui", "/swagger-ui/index.html");
        links.put("openapi_spec", "/v3/api-docs");
        links.put("health_check", "/api/health");
        links.put("scan_url", "/api/scans/url");
        links.put("auth_login", "/api/auth/login");
        links.put("active_alerts", "/api/alerts/active");
        links.put("prevention_guides", "/api/prevention/categories");

        welcome.put("endpoints", links);
        return ResponseEntity.ok(ApiResponse.success("PhishShield Backend API is ready", welcome));
    }

    @GetMapping("/api/health")
    @Operation(summary = "System health and status check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("platform", "PhishShield Real-Time Cyber Defender");
        healthData.put("version", "1.0.0");
        healthData.put("engine", "Hybrid Rule Engine + SSRF Shield Active");
        healthData.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success("System is healthy and operational", healthData));
    }
}
