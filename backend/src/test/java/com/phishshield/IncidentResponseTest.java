package com.phishshield;

import com.phishshield.domain.*;
import com.phishshield.dto.request.FraudIncidentRequest;
import com.phishshield.dto.response.ComplaintDraftResponse;
import com.phishshield.dto.response.FraudIncidentDto;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.FraudIncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IncidentResponseTest {

    @Autowired
    private FraudIncidentService incidentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private URLScanRepository urlScanRepository;

    private User citizenA;
    private User citizenB;
    private User adminUser;
    private URLScan testScan;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("User role").build()));

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).description("Admin role").build()));

        citizenA = userRepository.findByUsername("incident_user_a")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("incident_user_a")
                        .email("user_a@prahar.security")
                        .passwordHash("hashed")
                        .fullName("Citizen A")
                        .role(userRole)
                        .isActive(true)
                        .build()));

        citizenB = userRepository.findByUsername("incident_user_b")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("incident_user_b")
                        .email("user_b@prahar.security")
                        .passwordHash("hashed")
                        .fullName("Citizen B")
                        .role(userRole)
                        .isActive(true)
                        .build()));

        adminUser = userRepository.findByUsername("incident_admin")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("incident_admin")
                        .email("admin@prahar.security")
                        .passwordHash("hashed")
                        .fullName("Admin Officer")
                        .role(adminRole)
                        .isActive(true)
                        .build()));

        testScan = urlScanRepository.save(URLScan.builder()
                .user(citizenA)
                .url("https://fraud-portal-hdfc.net/verify.php")
                .normalizedUrl("https://fraud-portal-hdfc.net/verify.php")
                .domain("fraud-portal-hdfc.net")
                .scanType(ScanType.URL)
                .riskScore(95)
                .riskLevel(RiskLevel.HIGH_RISK)
                .threatCategory(ThreatCategory.BANKING_PHISHING)
                .status("COMPLETED")
                .build());
    }

    @Test
    @DisplayName("Incident Creation: Creates incident linked to existing scan and generates timestamped timeline")
    void testCreateIncidentLinkedToScan() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setIncidentType(IncidentType.PHISHING_CLICKED);
        req.setTitle("Suspicious Bank SMS Link Clicked");
        req.setDescription("I received a text message and clicked the link.");
        req.setSourceScanId(testScan.getId());
        req.setAmountInvolved(new BigDecimal("15000"));

        FraudIncidentDto incident = incidentService.createIncident(req, citizenA.getId());

        assertNotNull(incident.getId());
        assertEquals(citizenA.getId(), incidentService.getIncidentById(incident.getId(), citizenA.getId()).getId() != null ? citizenA.getId() : null);
        assertEquals(testScan.getId(), incident.getSourceScanId(), "Source scan ID must be linked");
        assertEquals(IncidentSeverity.HIGH, incident.getSeverity(), "Amount >= 10k should yield HIGH severity");
        assertNotNull(incident.getTimelineJson());
        assertTrue(incident.getTimelineJson().contains("INCIDENT_CREATED"));
        assertTrue(incident.getTimelineJson().contains(testScan.getId().toString()), "Timeline must record linked scan ID");
    }

    @Test
    @DisplayName("PII & Secret Redaction: Scrubs credit cards, OTPs, PINs, and Bearer tokens before saving incident")
    void testSensitiveDataRedactionInIncident() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setIncidentType(IncidentType.UNAUTHORIZED_DEBIT);
        req.setTitle("Unauthorized Card Debit");
        req.setDescription("My debit card is 5241 8901 2345 6789 and the fraudster asked for OTP is 894102. Auth Bearer eyJhbGciOi...");
        req.setEvidenceNotes("Suspect claimed PIN is 1234 was needed.");
        req.setAmountInvolved(new BigDecimal("75000"));

        FraudIncidentDto incident = incidentService.createIncident(req, citizenA.getId());

        assertFalse(incident.getDescription().contains("5241 8901 2345 6789"), "Card number must be redacted");
        assertTrue(incident.getDescription().contains("[REDACTED_CARD_NUMBER]"));

        assertFalse(incident.getDescription().contains("894102"), "OTP must be redacted");
        assertTrue(incident.getDescription().contains("[REDACTED_SECRET]"));

        assertFalse(incident.getDescription().contains("eyJhbGciOi"), "Bearer token must be redacted");

        assertFalse(incident.getEvidenceNotes().contains("1234"), "PIN must be redacted");
        assertTrue(incident.getEvidenceNotes().contains("[REDACTED_SECRET]"));
        assertEquals(IncidentSeverity.CRITICAL, incident.getSeverity(), "Amount >= 50,000 must yield CRITICAL severity");
    }

    @Test
    @DisplayName("User Isolation (IDOR Defense): User B cannot read, update, or delete User A's incident")
    void testUserIsolationPreventsIdor() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setIncidentType(IncidentType.UPI_FRAUD);
        req.setTitle("Citizen A Private Fraud Event");
        req.setDescription("Private details");

        FraudIncidentDto created = incidentService.createIncident(req, citizenA.getId());

        // Citizen B attempting to view Citizen A's incident must be rejected
        assertThrows(ResourceNotFoundException.class, () ->
                incidentService.getIncidentById(created.getId(), citizenB.getId())
        );

        // Citizen B attempting to update Citizen A's incident must be rejected
        FraudIncidentRequest updateReq = new FraudIncidentRequest();
        updateReq.setTitle("Tampered Title");
        assertThrows(ResourceNotFoundException.class, () ->
                incidentService.updateIncident(created.getId(), updateReq, citizenB.getId())
        );

        // Citizen B attempting to delete Citizen A's incident must be rejected
        assertThrows(ResourceNotFoundException.class, () ->
                incidentService.deleteIncident(created.getId(), citizenB.getId())
        );
    }

    @Test
    @DisplayName("Admin Oversight: Security administrator can view all incidents and triage status with audit notes")
    void testAdminOversightAndStatusTriage() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setIncidentType(IncidentType.SIM_SWAP);
        req.setTitle("Emergency SIM Swap Detected");
        req.setDescription("Phone disconnected suddenly.");

        FraudIncidentDto created = incidentService.createIncident(req, citizenA.getId());

        // Admin lists all incidents
        Page<FraudIncidentDto> allIncidents = incidentService.getAllIncidentsAdmin(PageRequest.of(0, 10), null, null);
        assertTrue(allIncidents.getTotalElements() >= 1);

        // Admin triages incident to INVESTIGATING
        FraudIncidentDto triaged = incidentService.adminUpdateStatus(
                created.getId(),
                IncidentStatus.SUBMITTED_EXTERNALLY,
                IncidentSeverity.CRITICAL,
                "Cyber cell notified; lien requested on target wallet",
                adminUser.getUsername()
        );

        assertEquals(IncidentStatus.SUBMITTED_EXTERNALLY, triaged.getStatus());
        assertTrue(triaged.getTimelineJson().contains("ADMIN_TRIAGE_UPDATE"));
        assertTrue(triaged.getTimelineJson().contains("Cyber cell notified"));
    }

    @Test
    @DisplayName("Complaint Draft: Generates official cybercrime complaint without making false automation claims")
    void testComplaintDraftGeneration() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setIncidentType(IncidentType.UNAUTHORIZED_DEBIT);
        req.setTitle("Unauthorized Withdrawal from Savings Account");
        req.setDescription("Fraudulent transaction of INR 20000 observed on statement.");
        req.setAmountInvolved(new BigDecimal("20000"));
        req.setBankOrProviderContacted("HDFC Bank");
        req.setRelatedTransactionIds("UTR-20261023-998811");

        FraudIncidentDto created = incidentService.createIncident(req, citizenA.getId());
        ComplaintDraftResponse draft = incidentService.generateComplaintDraft(created.getId(), citizenA.getId());

        assertNotNull(draft.getComplaintDraftText());
        assertTrue(draft.getComplaintDraftText().contains("UTR-20261023-998811"));
        assertTrue(draft.getComplaintDraftText().contains("HDFC Bank"));
        assertTrue(draft.getGuidanceSteps().stream().anyMatch(s -> s.contains("1930")));
        assertTrue(draft.getGuidanceSteps().stream().anyMatch(s -> s.contains("Prahar is an advisory guidance tool")));
    }
}
