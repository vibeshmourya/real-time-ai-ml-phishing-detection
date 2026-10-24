package com.phishshield.dto.response;

public class MLServiceHealthDto {

    private String status;           // "UP", "DOWN", "MODEL_UNAVAILABLE"
    private String service;          // "prahar-ml-service"
    private boolean modelLoaded;
    private String modelVersion;
    private String modelName;
    private Long latencyMs;
    private String serviceUrl;
    private String error;

    public MLServiceHealthDto() {}

    public static MLServiceHealthDto down(String serviceUrl, String error) {
        MLServiceHealthDto dto = new MLServiceHealthDto();
        dto.setStatus("DOWN");
        dto.setService("prahar-ml-service");
        dto.setModelLoaded(false);
        dto.setServiceUrl(serviceUrl);
        dto.setError(error);
        return dto;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public boolean isModelLoaded() { return modelLoaded; }
    public void setModelLoaded(boolean modelLoaded) { this.modelLoaded = modelLoaded; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
