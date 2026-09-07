package com.phishshield.service;

import com.phishshield.domain.SecurityAlert;
import com.phishshield.domain.User;
import com.phishshield.dto.request.CreateSecurityAlertRequest;
import com.phishshield.dto.response.SecurityAlertResponse;
import com.phishshield.repository.SecurityAlertRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecurityAlertService {

    private final SecurityAlertRepository securityAlertRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SecurityAlertService(SecurityAlertRepository securityAlertRepository,
                                UserRepository userRepository,
                                AuditLogService auditLogService) {
        this.securityAlertRepository = securityAlertRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlertResponse> getActiveAlerts() {
        return securityAlertRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SecurityAlertResponse createAlert(CreateSecurityAlertRequest request) {
        SecurityAlert alert = SecurityAlert.builder()
                .title(request.getTitle())
                .alertType(request.getAlertType())
                .severity(request.getSeverity())
                .message(request.getMessage())
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : "ALL")
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .build();

        SecurityAlert saved = securityAlertRepository.save(alert);

        User admin = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);

        auditLogService.logAction(admin, "CREATE_SECURITY_ALERT", "SECURITY_ALERT", saved.getId().toString(),
                "Broadcasted security alert: " + saved.getTitle());

        return mapToResponse(saved);
    }

    public SecurityAlertResponse mapToResponse(SecurityAlert alert) {
        return SecurityAlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .targetAudience(alert.getTargetAudience())
                .isActive(alert.getIsActive())
                .createdAt(alert.getCreatedAt())
                .expiresAt(alert.getExpiresAt())
                .build();
    }
}
