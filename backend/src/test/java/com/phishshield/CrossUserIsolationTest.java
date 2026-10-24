package com.phishshield;

import com.phishshield.domain.*;
import com.phishshield.dto.request.FraudIncidentRequest;
import com.phishshield.dto.request.TransactionRequest;
import com.phishshield.dto.response.FraudIncidentDto;
import com.phishshield.dto.response.TransactionDto;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.repository.FraudIncidentRepository;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.service.FraudIncidentService;
import com.phishshield.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CrossUserIsolationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private FraudIncidentService fraudIncidentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @Autowired
    private FraudIncidentRepository incidentRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("User role").build()));

        userA = userRepository.save(User.builder()
                .username("user_alpha")
                .email("alpha@prahar.security")
                .passwordHash("hashed_pwd_alpha")
                .fullName("User Alpha")
                .role(userRole)
                .isActive(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("user_beta")
                .email("beta@prahar.security")
                .passwordHash("hashed_pwd_beta")
                .fullName("User Beta")
                .role(userRole)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Verify User B cannot access or list transactions created by User A")
    void testTransactionCrossUserIsolation() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("3500.00"));
        req.setDirection(TransactionDirection.EXPENSE);
        req.setTransactionType(TransactionType.UPI);
        req.setCategory(TransactionCategory.SHOPPING);
        req.setMerchant("Amazon India");
        req.setTransactionReference("UPI-TXN-ALPHA-99");
        req.setTransactionDateTime(LocalDateTime.now());

        TransactionDto created = transactionService.createManualTransaction(req, userA.getId());
        assertNotNull(created.getId());

        // User A can see their own transaction
        var userATxns = transactionService.getUserTransactions(userA.getId(), PageRequest.of(0, 10), null, null, null);
        assertEquals(1, userATxns.getTotalElements());
        assertEquals("Amazon India", userATxns.getContent().get(0).getMerchant());

        // User B has zero transactions and cannot see User A's transaction
        var userBTxns = transactionService.getUserTransactions(userB.getId(), PageRequest.of(0, 10), null, null, null);
        assertEquals(0, userBTxns.getTotalElements());

        // User B cannot verify or update User A's transaction (must throw ResourceNotFoundException)
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.verifyTransaction(created.getId(), userB.getId());
        });

        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.updateCategory(created.getId(), TransactionCategory.FOOD, userB.getId());
        });
    }

    @Test
    @DisplayName("Verify User B cannot access or update incident reports created by User A")
    void testIncidentCrossUserIsolation() {
        FraudIncidentRequest req = new FraudIncidentRequest();
        req.setTitle("Suspicious UPI debit");
        req.setIncidentType(IncidentType.UPI_FRAUD);
        req.setAmountInvolved(new BigDecimal("7500.00"));
        req.setBankOrProviderContacted("State Bank of India");
        req.setComplaintReference("2026-NCRP-881923");
        req.setEvidenceNotes("Fraudulent collect request approved on GPay");

        FraudIncidentDto created = fraudIncidentService.createIncident(req, userA.getId());
        assertNotNull(created.getId());

        // User A sees their own incident
        var userAIncidents = fraudIncidentService.getUserIncidents(userA.getId(), PageRequest.of(0, 10), null);
        assertEquals(1, userAIncidents.getTotalElements());

        // User B sees zero incidents
        var userBIncidents = fraudIncidentService.getUserIncidents(userB.getId(), PageRequest.of(0, 10), null);
        assertEquals(0, userBIncidents.getTotalElements());

        // User B cannot update User A's incident
        FraudIncidentRequest updateAttempt = new FraudIncidentRequest();
        updateAttempt.setStatus(IncidentStatus.RESOLVED);
        assertThrows(ResourceNotFoundException.class, () -> {
            fraudIncidentService.updateIncident(created.getId(), updateAttempt, userB.getId());
        });
    }
}

