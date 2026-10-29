package com.phishshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.dto.request.LoginRequest;
import com.phishshield.security.RateLimitingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRateLimiterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.clearWindows();
    }

    @Test
    @DisplayName("Brute Force Defense: Rapid authentication attempts trigger HTTP 429 Too Many Requests")
    void testAuthRateLimiterThrottling() throws Exception {
        LoginRequest req = new LoginRequest("testuser", "WrongPass123!");
        String json = objectMapper.writeValueAsString(req);

        // Guard against minute rollover during execution
        long msIntoMinute = System.currentTimeMillis() % 60000;
        if (msIntoMinute > 57000) {
            Thread.sleep(60000 - msIntoMinute + 100);
        }

        // Send 15 requests (allowed threshold)
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .header("X-Forwarded-For", "198.51.100.45"));
        }

        // 16th request must be throttled with 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-Forwarded-For", "198.51.100.45"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
