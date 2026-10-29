package com.phishshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.dto.request.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;

/**
 * Dedicated Google Gemini AI service communicating with Google's official Generative Language API.
 * Supports both official Google Gemini v1beta (:generateContent) and OpenAI-compatible proxy endpoints.
 * Never exposes API keys or internal credentials to logs, clients, or test output.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    public enum ProviderStatus {
        UNCONFIGURED,
        MISSING_CREDENTIALS,
        CONFIGURED_NOT_TESTED,
        PROVIDER_AVAILABLE,
        PROVIDER_ERROR
    }

    @Value("${app.ai.provider:none}")
    private String configuredProvider;

    @Value("${app.ai.api-key:${GEMINI_API_KEY:${GOOGLE_API_KEY:}}}")
    private String apiKey;

    @Value("${app.ai.model:${GEMINI_MODEL:gemini-1.5-flash}}")
    private String model;

    @Value("${app.ai.api-url:}")
    private String customApiUrl;

    @Value("${app.ai.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${app.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private volatile ProviderStatus status = ProviderStatus.CONFIGURED_NOT_TESTED;
    private volatile String lastErrorCategory = null;
    private volatile Long lastProbeTimestamp = null;
    private volatile boolean connectivityVerified = false;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient customRestClient;

    public interface ToolExecutor {
        String execute(String toolName, String argumentsJson);
    }

    public GeminiService() {}

    public GeminiService(String provider, String apiKey, String customApiUrl, String model, int timeoutMs) {
        this.configuredProvider = provider;
        this.apiKey = apiKey;
        this.customApiUrl = customApiUrl;
        this.model = model;
        this.timeoutMs = timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getConfiguredProvider() { return configuredProvider; }
    public String getCustomApiUrl() { return customApiUrl; }
    public String getModel() { return model; }

    public void setCustomRestClient(RestClient customRestClient) {
        this.customRestClient = customRestClient;
    }

    public void setConfiguration(String provider, String apiKey, String customApiUrl, String model, int timeoutMs) {
        this.configuredProvider = provider;
        this.apiKey = apiKey;
        this.customApiUrl = customApiUrl;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.status = ProviderStatus.CONFIGURED_NOT_TESTED;
        this.lastErrorCategory = null;
        this.connectivityVerified = false;
    }

    public boolean isConfigured() {
        boolean hasKey = apiKey != null && !apiKey.trim().isEmpty();
        String prov = resolveProviderName();
        return !"none".equalsIgnoreCase(prov) && hasKey;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public String resolveProviderName() {
        String prov = (configuredProvider != null) ? configuredProvider.trim().toLowerCase() : "none";
        boolean hasKey = (apiKey != null && !apiKey.trim().isEmpty());
        if ("none".equals(prov) && hasKey) {
            return "gemini";
        }
        return prov;
    }

    public String getResolvedModel() {
        String m = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-1.5-flash";
        // Strip any leading "models/" if present for consistent model identifier reporting
        return m.startsWith("models/") ? m.substring(7) : m;
    }

    /**
     * Resolves the target API URL.
     * Uses official Google Gemini v1beta endpoint by default, or the custom API URL if provided.
     */
    public String resolveApiEndpoint() {
        if (customApiUrl != null && !customApiUrl.trim().isEmpty()) {
            return customApiUrl.trim();
        }
        String m = getResolvedModel();
        return "https://generativelanguage.googleapis.com/v1beta/models/" + m + ":generateContent";
    }

    private boolean isOpenAiCompatibleEndpoint(String endpoint) {
        return endpoint != null && endpoint.contains("/openai/");
    }

    /**
     * Executes generation request using the official Google Gemini API (or OpenAI proxy if configured).
     */
    public String generateContent(String systemInstruction,
                                  List<ChatMessageDto> conversationHistory,
                                  String userMessage,
                                  List<Map<String, Object>> tools,
                                  ToolExecutor toolExecutor,
                                  int maxTokens) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Gemini provider is not configured or missing API credentials.");
        }

        String endpoint = resolveApiEndpoint();
        String activeModel = getResolvedModel();
        boolean isOpenAi = isOpenAiCompatibleEndpoint(endpoint);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient client = (this.customRestClient != null) ? this.customRestClient :
                RestClient.builder().requestFactory(factory).build();

        try {
            String result;
            if (isOpenAi) {
                result = executeOpenAiCompatibleFlow(client, endpoint, activeModel, systemInstruction,
                        conversationHistory, userMessage, tools, toolExecutor, maxTokens);
            } else {
                result = executeNativeGeminiFlow(client, endpoint, activeModel, systemInstruction,
                        conversationHistory, userMessage, tools, toolExecutor, maxTokens);
            }
            this.status = ProviderStatus.PROVIDER_AVAILABLE;
            this.lastErrorCategory = null;
            this.connectivityVerified = true;
            this.lastProbeTimestamp = System.currentTimeMillis();
            return result;
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            String msg;
            if (code == 401 || code == 403) {
                msg = "Invalid or unauthorized API key for Google Gemini (HTTP " + code + ")";
            } else if (code == 429) {
                msg = "Rate limit or quota exceeded for Google Gemini (HTTP 429)";
            } else if (code == 404) {
                msg = "Specified model '" + activeModel + "' not accessible on Google Gemini (HTTP 404)";
            } else {
                msg = "Google Gemini API returned HTTP " + code;
            }
            this.status = ProviderStatus.PROVIDER_ERROR;
            this.lastErrorCategory = sanitizeError(msg);
            this.connectivityVerified = false;
            this.lastProbeTimestamp = System.currentTimeMillis();
            throw new RuntimeException(msg);
        } catch (Exception e) {
            this.status = ProviderStatus.PROVIDER_ERROR;
            this.connectivityVerified = false;
            this.lastProbeTimestamp = System.currentTimeMillis();
            if (e.getCause() instanceof SocketTimeoutException || e instanceof SocketTimeoutException) {
                String msg = "Connection to Google Gemini timed out after " + timeoutMs + "ms";
                this.lastErrorCategory = sanitizeError(msg);
                throw new RuntimeException(msg);
            }
            this.lastErrorCategory = sanitizeError(e.getMessage());
            throw e;
        }
    }

    /**
     * Official Native Google Gemini v1beta Protocol
     */
    private String executeNativeGeminiFlow(RestClient client,
                                          String endpoint,
                                          String modelName,
                                          String systemInstruction,
                                          List<ChatMessageDto> history,
                                          String userMessage,
                                          List<Map<String, Object>> tools,
                                          ToolExecutor toolExecutor,
                                          int maxTokens) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();

        // 1. System instruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            payload.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        // 2. Bounded conversation turns
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 8);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto turn = history.get(i);
                if (turn.getContent() != null && !turn.getContent().isBlank()) {
                    String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "model" : "user";
                    contents.add(Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", turn.getContent()))
                    ));
                }
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));
        payload.put("contents", contents);

        // 3. Native function declarations
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> functionDeclarations = new ArrayList<>();
            for (Map<String, Object> t : tools) {
                Object fnObj = t.get("function");
                if (fnObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fn = (Map<String, Object>) fnObj;
                    functionDeclarations.add(fn);
                }
            }
            if (!functionDeclarations.isEmpty()) {
                payload.put("tools", List.of(Map.of("function_declarations", functionDeclarations)));
            }
        }

        // 4. Generation configuration
        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("temperature", 0.2);
        genConfig.put("maxOutputTokens", Math.max(100, maxTokens));
        payload.put("generationConfig", genConfig);

        String responseBody = client.post()
                .uri(endpoint)
                .header("x-goog-api-key", apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidate = root.path("candidates").path(0);
        JsonNode contentNode = candidate.path("content");
        JsonNode partsNode = contentNode.path("parts");

        // Check for native function call
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                if (part.has("functionCall") && toolExecutor != null) {
                    JsonNode fnCall = part.path("functionCall");
                    String fnName = fnCall.path("name").asText();
                    String argsJson = fnCall.path("args").toString();

                    String toolResult = toolExecutor.execute(fnName, argsJson);

                    // Append assistant tool call and function response turns
                    contents.add(Map.of(
                            "role", "model",
                            "parts", List.of(Map.of("functionCall", fnCall))
                    ));
                    contents.add(Map.of(
                            "role", "function",
                            "parts", List.of(Map.of(
                                    "functionResponse", Map.of(
                                            "name", fnName,
                                            "response", Map.of("output", toolResult)
                                    )
                            ))
                    ));

                    // Second turn to generate synthesized explanation
                    payload.put("contents", contents);
                    String secondResp = client.post()
                            .uri(endpoint)
                            .header("x-goog-api-key", apiKey.trim())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                    JsonNode secondRoot = objectMapper.readTree(secondResp);
                    return extractTextFromCandidate(secondRoot.path("candidates").path(0));
                }
            }
        }

        return extractTextFromCandidate(candidate);
    }

    private String extractTextFromCandidate(JsonNode candidate) {
        JsonNode partsNode = candidate.path("content").path("parts");
        if (partsNode.isArray() && !partsNode.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : partsNode) {
                if (part.has("text")) {
                    sb.append(part.path("text").asText());
                }
            }
            if (!sb.isEmpty()) return sb.toString().trim();
        }
        return "";
    }

    /**
     * OpenAI-Compatible Endpoint Support
     */
    private String executeOpenAiCompatibleFlow(RestClient client,
                                               String endpoint,
                                               String modelName,
                                               String systemInstruction,
                                               List<ChatMessageDto> history,
                                               String userMessage,
                                               List<Map<String, Object>> tools,
                                               ToolExecutor toolExecutor,
                                               int maxTokens) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemInstruction));
        }

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 8);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto turn = history.get(i);
                if (turn.getContent() != null && !turn.getContent().isBlank()) {
                    String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "assistant" : "user";
                    messages.add(Map.of("role", role, "content", turn.getContent()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);
        payload.put("messages", messages);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", 0.2);
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
        }

        String responseBody = client.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choiceMsg = root.path("choices").path(0).path("message");
        JsonNode toolCallsNode = choiceMsg.path("tool_calls");

        if (toolCallsNode.isArray() && !toolCallsNode.isEmpty() && toolExecutor != null) {
            List<Map<String, Object>> toolCallList = new ArrayList<>();
            for (JsonNode tc : toolCallsNode) {
                Map<String, Object> tcMap = new LinkedHashMap<>();
                tcMap.put("id", tc.path("id").asText());
                tcMap.put("type", "function");
                tcMap.put("function", Map.of(
                        "name", tc.path("function").path("name").asText(),
                        "arguments", tc.path("function").path("arguments").asText()
                ));
                toolCallList.add(tcMap);
            }

            messages.add(Map.of("role", "assistant", "tool_calls", toolCallList));

            for (JsonNode tc : toolCallsNode) {
                String callId = tc.path("id").asText();
                String fnName = tc.path("function").path("name").asText();
                String argsJson = tc.path("function").path("arguments").asText();
                String toolResult = toolExecutor.execute(fnName, argsJson);

                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", callId,
                        "content", toolResult
                ));
            }

            Map<String, Object> secondPayload = new LinkedHashMap<>();
            secondPayload.put("model", modelName);
            secondPayload.put("messages", messages);
            secondPayload.put("max_tokens", maxTokens);
            secondPayload.put("temperature", 0.2);

            String secondResp = client.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(secondPayload)
                    .retrieve()
                    .body(String.class);

            JsonNode secondRoot = objectMapper.readTree(secondResp);
            return secondRoot.path("choices").path(0).path("message").path("content").asText();
        }

        return choiceMsg.path("content").asText();
    }

    /**
     * Executes an actual minimal generation request ("ping") to verify end-to-end connectivity.
     */
    public synchronized boolean probeConnectivity() {
        if (!isConfigured()) {
            this.status = ProviderStatus.UNCONFIGURED;
            this.connectivityVerified = false;
            return false;
        }

        try {
            generateContent("You are a connectivity test probe. Reply with PONG.",
                    Collections.emptyList(),
                    "ping",
                    Collections.emptyList(),
                    null,
                    10);
            this.status = ProviderStatus.PROVIDER_AVAILABLE;
            this.lastErrorCategory = null;
            this.connectivityVerified = true;
            this.lastProbeTimestamp = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            this.status = ProviderStatus.PROVIDER_ERROR;
            this.lastErrorCategory = sanitizeError(e.getMessage());
            this.connectivityVerified = false;
            this.lastProbeTimestamp = System.currentTimeMillis();
            return false;
        }
    }

    public Map<String, Object> getDiagnostics(boolean probe) {
        Map<String, Object> diag = new LinkedHashMap<>();
        String prov = resolveProviderName();
        boolean hasKey = (apiKey != null && !apiKey.trim().isEmpty());
        boolean isNone = "none".equalsIgnoreCase(prov);

        diag.put("provider", isNone ? "none" : prov);
        diag.put("configured", isConfigured());
        diag.put("hasApiKey", hasKey);
        diag.put("model", getResolvedModel());
        diag.put("endpoint", maskEndpoint(resolveApiEndpoint()));
        diag.put("timeoutMs", timeoutMs);
        diag.put("fallbackEnabled", fallbackEnabled);
        diag.put("activeMode", isConfigured() ? "GEN_AI_PROVIDER" : "RULE_BASED_SAFETY_ENGINE");

        if (isNone) {
            status = ProviderStatus.UNCONFIGURED;
            diag.put("status", ProviderStatus.UNCONFIGURED.name());
            diag.put("connectivityVerified", false);
            diag.put("configurationGuide", "To enable live Google Gemini AI, set GEMINI_API_KEY and AI_PROVIDER=gemini.");
        } else if (!hasKey) {
            status = ProviderStatus.MISSING_CREDENTIALS;
            diag.put("status", ProviderStatus.MISSING_CREDENTIALS.name());
            diag.put("connectivityVerified", false);
            diag.put("configurationGuide", "Provider '" + prov + "' selected, but GEMINI_API_KEY is missing.");
        } else {
            if (probe) {
                probeConnectivity();
            }
            diag.put("status", status.name());
            diag.put("connectivityVerified", connectivityVerified);
            if (lastErrorCategory != null) {
                diag.put("lastError", lastErrorCategory);
                diag.put("lastErrorCategory", lastErrorCategory);
            }
            if (lastProbeTimestamp != null) {
                diag.put("lastCheckedTimestamp", lastProbeTimestamp);
            }
        }

        return diag;
    }

    private String maskEndpoint(String endpoint) {
        if (endpoint == null) return "none";
        // Ensure no API keys in URL query params are visible
        return endpoint.replaceAll("key=[^&]+", "key=REDACTED");
    }

    public static String sanitizeError(String raw) {
        if (raw == null) return "Unknown provider error";
        // Scrub any secrets, bearer tokens, or keys
        String scrubbed = raw.replaceAll("(?i)(?:key|token|bearer|secret)[ =:]+([a-zA-Z0-9_\\-]{6,})", "[REDACTED]");
        if (scrubbed.length() > 200) {
            return scrubbed.substring(0, 197) + "...";
        }
        return scrubbed;
    }
}
