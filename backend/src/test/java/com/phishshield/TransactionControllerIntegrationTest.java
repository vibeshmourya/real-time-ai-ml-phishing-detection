package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.domain.*;
import com.phishshield.dto.request.ImportConfirmRequest;
import com.phishshield.dto.request.TransactionRequest;
import com.phishshield.dto.response.StatementPreviewRowDto;
import com.phishshield.repository.FinancialTransactionRepository;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser1;
    private User testUser2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("User").build()));

        testUser1 = userRepository.findByUsername("test_user_alpha").orElseGet(() -> {
            User u = User.builder()
                    .username("test_user_alpha")
                    .email("user1@prahar.test")
                    .passwordHash(passwordEncoder.encode("Test@123456"))
                    .fullName("User Alpha")
                    .role(userRole)
                    .isActive(true)
                    .build();
            return userRepository.save(u);
        });

        testUser2 = userRepository.findByUsername("test_user_beta").orElseGet(() -> {
            User u = User.builder()
                    .username("test_user_beta")
                    .email("user2@prahar.test")
                    .passwordHash(passwordEncoder.encode("Test@123456"))
                    .fullName("User Beta")
                    .role(userRole)
                    .isActive(true)
                    .build();
            return userRepository.save(u);
        });

        com.phishshield.security.UserPrincipal p1 = com.phishshield.security.UserPrincipal.create(testUser1);
        user1Token = tokenProvider.generateToken(new UsernamePasswordAuthenticationToken(p1, null, p1.getAuthorities()));

        com.phishshield.security.UserPrincipal p2 = com.phishshield.security.UserPrincipal.create(testUser2);
        user2Token = tokenProvider.generateToken(new UsernamePasswordAuthenticationToken(p2, null, p2.getAuthorities()));
    }

    @Test
    @DisplayName("INTEG-01: Unauthenticated request to /api/transactions returns 401 Unauthorized")
    void testUnauthenticatedAccessFails() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("INTEG-02: Authenticated user can fetch their personal transaction summary and baseline")
    void testAuthenticatedUserSummary() throws Exception {
        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baselineStatusMessage", notNullValue()));
    }

    @Test
    @DisplayName("INTEG-03: Preview import parses CSV and returns detected columns and preview rows")
    void testPreviewImportCsv() throws Exception {
        String csvContent = "Date,Description,Debit,Credit,Reference\n" +
                "2026-10-15,Swiggy Meal,350.00,,UPI481029\n" +
                "2026-10-16,Salary Deposit,,45000.00,SAL99281\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/transactions/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRowsFound").value(2))
                .andExpect(jsonPath("$.data.validRowsCount").value(2))
                .andExpect(jsonPath("$.data.previewRows", hasSize(2)));
    }

    @Test
    @DisplayName("INTEG-04: Confirm import commits rows and suppresses duplicate imports")
    void testConfirmImportAndDeduplication() throws Exception {
        StatementPreviewRowDto row = new StatementPreviewRowDto();
        row.setRowNumber(1);
        row.setDate(LocalDateTime.of(2026, 10, 15, 10, 0));
        row.setDescription("Coffee Day Cafe");
        row.setAmount(new BigDecimal("220.00"));
        row.setDirection("EXPENSE");
        row.setReference("REF482910");
        row.setValid(true);

        ImportConfirmRequest confirmReq = new ImportConfirmRequest();
        confirmReq.setRows(List.of(row));
        confirmReq.setFileName("statement.csv");

        // First commit
        mockMvc.perform(post("/api/transactions/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq))
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.duplicateSkippedCount").value(0));

        // Re-committing identical record skips as duplicate
        mockMvc.perform(post("/api/transactions/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq))
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(0))
                .andExpect(jsonPath("$.data.duplicateSkippedCount").value(1));
    }

    @Test
    @DisplayName("INTEG-05: Backwards-compatible aliases (/upload-csv, /import/csv, /manual) work seamlessly")
    void testBackwardCompatibleAliases() throws Exception {
        // 1. /upload-csv alias
        String csv = "Date,Description,Debit,Credit\n2026-10-18,Grocery Store,850.00,\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/transactions/upload-csv")
                        .file(file)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(1));

        // 2. /manual alias
        TransactionRequest manualReq = new TransactionRequest();
        manualReq.setAmount(new BigDecimal("120.00"));
        manualReq.setDirection(TransactionDirection.EXPENSE);
        manualReq.setMerchant("Chai Point");

        mockMvc.perform(post("/api/transactions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualReq))
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant").value("Chai Point"));
    }

    @Test
    @DisplayName("INTEG-06: Strict user data isolation prevents User B from seeing or verifying User A's transactions")
    void testCrossUserDataIsolation() throws Exception {
        // User A records a transaction
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("950.00"));
        req.setDirection(TransactionDirection.EXPENSE);
        req.setMerchant("Bookstore Alpha");

        String responseStr = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long txnId = objectMapper.readTree(responseStr).get("data").get("id").asLong();

        // User B attempts to verify User A's transaction -> 404 ResourceNotFound
        mockMvc.perform(patch("/api/transactions/" + txnId + "/verify")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());

        // User B's transaction history does not contain User A's transaction
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.merchant == 'Bookstore Alpha')]", empty()));
    }
}
