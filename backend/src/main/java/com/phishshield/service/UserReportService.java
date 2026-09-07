package com.phishshield.service;

import com.phishshield.domain.ReportStatus;
import com.phishshield.domain.URLScan;
import com.phishshield.domain.User;
import com.phishshield.domain.UserReport;
import com.phishshield.dto.request.ReportStatusUpdateRequest;
import com.phishshield.dto.request.UserReportRequest;
import com.phishshield.dto.response.UserReportResponse;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserReportRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SecurityUtils;
import com.phishshield.util.UrlNormalizationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;
    private final URLScanRepository urlScanRepository;
    private final AuditLogService auditLogService;

    public UserReportService(UserReportRepository userReportRepository,
                             UserRepository userRepository,
                             URLScanRepository urlScanRepository,
                             AuditLogService auditLogService) {
        this.userReportRepository = userReportRepository;
        this.userRepository = userRepository;
        this.urlScanRepository = urlScanRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserReportResponse submitReport(UserReportRequest request) {
        User currentUser = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);

        URLScan associatedScan = null;
        if (request.getScanId() != null) {
            associatedScan = urlScanRepository.findById(request.getScanId()).orElse(null);
        }

        String normalizedUrl = UrlNormalizationUtil.normalizeUrl(request.getUrl());

        UserReport report = UserReport.builder()
                .user(currentUser)
                .url(request.getUrl())
                .normalizedUrl(normalizedUrl)
                .threatCategory(request.getThreatCategory() != null ? request.getThreatCategory() : com.phishshield.domain.ThreatCategory.UNKNOWN)
                .description(request.getDescription())
                .reporterEmail(request.getReporterEmail() != null ? request.getReporterEmail() : (currentUser != null ? currentUser.getEmail() : null))
                .scan(associatedScan)
                .status(ReportStatus.PENDING)
                .build();

        UserReport saved = userReportRepository.save(report);

        auditLogService.logAction(currentUser, "SUBMIT_REPORT", "USER_REPORT", saved.getId().toString(),
                "Threat report filed for: " + normalizedUrl);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserReportResponse> getUserReports(Pageable pageable) {
        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new BadRequestException("Authentication required"));

        return userReportRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return userReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::mapToResponse);
        }
        return userReportRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public UserReportResponse updateReportStatus(Long id, ReportStatusUpdateRequest request) {
        UserReport report = userReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        report.setStatus(request.getStatus());
        if (request.getAdminNotes() != null) {
            report.setAdminNotes(request.getAdminNotes());
        }

        UserReport updated = userReportRepository.save(report);

        User admin = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);

        auditLogService.logAction(admin, "UPDATE_REPORT_STATUS", "USER_REPORT", id.toString(),
                "Report status updated to: " + request.getStatus());

        return mapToResponse(updated);
    }

    public UserReportResponse mapToResponse(UserReport report) {
        return UserReportResponse.builder()
                .id(report.getId())
                .url(report.getUrl())
                .normalizedUrl(report.getNormalizedUrl())
                .threatCategory(report.getThreatCategory())
                .description(report.getDescription())
                .reporterEmail(report.getReporterEmail())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .scanId(report.getScan() != null ? report.getScan().getId() : null)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
