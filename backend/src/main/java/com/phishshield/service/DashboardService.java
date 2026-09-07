package com.phishshield.service;

import com.phishshield.domain.ReportStatus;
import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.User;
import com.phishshield.dto.response.*;
import com.phishshield.exception.BadRequestException;
import com.phishshield.repository.SecurityAlertRepository;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserReportRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final URLScanRepository urlScanRepository;
    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final URLScanService urlScanService;
    private final UserReportService userReportService;

    public DashboardService(URLScanRepository urlScanRepository,
                            UserReportRepository userReportRepository,
                            UserRepository userRepository,
                            SecurityAlertRepository securityAlertRepository,
                            URLScanService urlScanService,
                            UserReportService userReportService) {
        this.urlScanRepository = urlScanRepository;
        this.userReportRepository = userReportRepository;
        this.userRepository = userRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.urlScanService = urlScanService;
        this.userReportService = userReportService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getUserDashboardStats() {
        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new BadRequestException("Authentication required to access dashboard metrics"));

        long totalScans = urlScanRepository.countByUser(user);
        long safeScans = urlScanRepository.countByUserAndRiskLevel(user, RiskLevel.SAFE);
        long suspiciousScans = urlScanRepository.countByUserAndRiskLevel(user, RiskLevel.SUSPICIOUS);
        long highRiskScans = urlScanRepository.countByUserAndRiskLevel(user, RiskLevel.HIGH_RISK);

        Map<String, Long> riskMap = new HashMap<>();
        riskMap.put("SAFE", safeScans);
        riskMap.put("SUSPICIOUS", suspiciousScans);
        riskMap.put("HIGH_RISK", highRiskScans);

        List<URLScanResponse> recentScans = urlScanRepository.findTop10ByUserOrderByCreatedAtDesc(user).stream()
                .map(scan -> urlScanService.mapToResponse(scan, null))
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalScans(totalScans)
                .safeScans(safeScans)
                .suspiciousScans(suspiciousScans)
                .highRiskScans(highRiskScans)
                .reportsSubmitted(userReportRepository.count())
                .scansByRiskLevel(riskMap)
                .recentScans(recentScans)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getAdminDashboardStats() {
        long totalUsers = userRepository.count();
        long totalScans = urlScanRepository.count();
        long highRisk = urlScanRepository.countByRiskLevel(RiskLevel.HIGH_RISK);
        long suspicious = urlScanRepository.countByRiskLevel(RiskLevel.SUSPICIOUS);
        long safe = urlScanRepository.countByRiskLevel(RiskLevel.SAFE);
        long pendingReports = userReportRepository.countByStatus(ReportStatus.PENDING);
        long totalReports = userReportRepository.count();
        long activeAlerts = securityAlertRepository.findByIsActiveTrueOrderByCreatedAtDesc().size();

        Map<String, Long> catDist = new HashMap<>();
        List<Object[]> catRows = urlScanRepository.countGroupedByThreatCategory();
        for (Object[] row : catRows) {
            if (row[0] != null && row[1] != null) {
                catDist.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> riskDist = new HashMap<>();
        riskDist.put("SAFE", safe);
        riskDist.put("SUSPICIOUS", suspicious);
        riskDist.put("HIGH_RISK", highRisk);

        List<URLScanResponse> recentScans = urlScanRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(scan -> urlScanService.mapToResponse(scan, null))
                .collect(Collectors.toList());

        List<UserReportResponse> recentReports = userReportRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(userReportService::mapToResponse)
                .collect(Collectors.toList());

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalScans(totalScans)
                .highRiskDetections(highRisk)
                .suspiciousDetections(suspicious)
                .safeDetections(safe)
                .pendingReports(pendingReports)
                .totalReports(totalReports)
                .activeAlerts(activeAlerts)
                .threatCategoryDistribution(catDist)
                .riskLevelDistribution(riskDist)
                .recentScans(recentScans)
                .recentReports(recentReports)
                .build();
    }
}
