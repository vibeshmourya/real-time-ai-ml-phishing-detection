package com.phishshield;

import com.phishshield.domain.*;
import com.phishshield.dto.response.NotificationDto;
import com.phishshield.dto.response.NotificationPreferenceDto;
import com.phishshield.dto.response.URLScanResponse;
import com.phishshield.exception.ForbiddenException;
import com.phishshield.repository.NotificationPreferenceRepository;
import com.phishshield.repository.NotificationRepository;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        Role citizenRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).build()));

        user1 = userRepository.findByUsername("notify_user1").orElseGet(() -> {
            User u = new User();
            u.setUsername("notify_user1");
            u.setEmail("notify1@prahar.gov.in");
            u.setPasswordHash("hashedpassword123");
            u.setRole(citizenRole);
            return userRepository.save(u);
        });

        user2 = userRepository.findByUsername("notify_user2").orElseGet(() -> {
            User u = new User();
            u.setUsername("notify_user2");
            u.setEmail("notify2@prahar.gov.in");
            u.setPasswordHash("hashedpassword123");
            u.setRole(citizenRole);
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("createScanNotification: Persists notification and respects idempotency")
    void testCreateScanNotification_Integration() {
        URLScanResponse scan = URLScanResponse.builder()
                .scanId(9001L)
                .url("https://malicious-bank-login.biz")
                .domain("malicious-bank-login.biz")
                .riskScore(92)
                .riskLevel(RiskLevel.HIGH_RISK)
                .scanType(ScanType.URL)
                .threatCategory(ThreatCategory.BANKING_PHISHING)
                .build();

        NotificationDto dto = notificationService.createScanNotification(user1, scan);

        assertThat(dto).isNotNull();
        assertThat(dto.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
        assertThat(dto.getNotificationType()).isEqualTo(NotificationType.SCAN_ALERT);
        assertThat(dto.getTitle()).contains("Critical Phishing Alert Flagged");

        // Test idempotency: sending again with same scan ID should not create duplicate
        NotificationDto duplicate = notificationService.createScanNotification(user1, scan);
        assertThat(duplicate).isNull();

        long count = notificationRepository.countByUserIdAndIsReadFalse(user1.getId());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("markAsRead: Enforces user ownership and updates read status")
    void testMarkAsRead_OwnershipEnforced() {
        URLScanResponse scan = URLScanResponse.builder()
                .scanId(9002L)
                .url("https://suspicious-gift-card.top")
                .domain("suspicious-gift-card.top")
                .riskScore(72)
                .riskLevel(RiskLevel.HIGH_RISK)
                .scanType(ScanType.URL)
                .threatCategory(ThreatCategory.CREDENTIAL_THEFT)
                .build();

        NotificationDto created = notificationService.createScanNotification(user1, scan);
        assertThat(created).isNotNull();

        // user2 attempting to mark user1's notification as read must fail
        assertThatThrownBy(() -> notificationService.markAsRead(user2.getId(), created.getId()))
                .isInstanceOf(ForbiddenException.class);

        // user1 marking own notification as read succeeds
        NotificationDto updated = notificationService.markAsRead(user1.getId(), created.getId());
        assertThat(updated.getIsRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Preferences: Filter alerts below minimum severity")
    void testNotificationPreferenceFiltering() {
        // Set user preferences to CRITICAL only
        NotificationPreferenceDto prefDto = new NotificationPreferenceDto(
                true, false, false, NotificationSeverity.CRITICAL, true, true, true
        );
        notificationService.updateUserPreferences(user1.getId(), prefDto);

        // Scan with risk score 60 (MEDIUM severity) should be filtered out
        URLScanResponse mediumScan = URLScanResponse.builder()
                .scanId(9003L)
                .url("https://questionable-domain.xyz")
                .domain("questionable-domain.xyz")
                .riskScore(60)
                .riskLevel(RiskLevel.SUSPICIOUS)
                .scanType(ScanType.URL)
                .threatCategory(ThreatCategory.SHOPPING_SCAM)
                .build();

        NotificationDto filtered = notificationService.createScanNotification(user1, mediumScan);
        assertThat(filtered).isNull();
    }

    @Test
    @DisplayName("markAllAsRead and Pagination: Correctly handles bulk operations")
    void testBulkMarkAllAsReadAndPagination() {
        URLScanResponse scanA = URLScanResponse.builder()
                .scanId(9004L)
                .url("https://phish1.com")
                .riskScore(88)
                .riskLevel(RiskLevel.HIGH_RISK)
                .threatCategory(ThreatCategory.BANKING_PHISHING)
                .build();

        URLScanResponse scanB = URLScanResponse.builder()
                .scanId(9005L)
                .url("https://phish2.com")
                .riskScore(89)
                .riskLevel(RiskLevel.HIGH_RISK)
                .threatCategory(ThreatCategory.CREDENTIAL_THEFT)
                .build();

        notificationService.createScanNotification(user1, scanA);
        notificationService.createScanNotification(user1, scanB);

        Page<NotificationDto> page = notificationService.getUserNotifications(user1.getId(), true, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);

        int updated = notificationService.markAllAsRead(user1.getId());
        assertThat(updated).isGreaterThanOrEqualTo(2);

        long unread = notificationService.getUnreadCount(user1.getId());
        assertThat(unread).isEqualTo(0);
    }
}
