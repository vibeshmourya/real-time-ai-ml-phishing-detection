package com.phishshield;

import com.phishshield.domain.Role;
import com.phishshield.domain.RoleName;
import com.phishshield.domain.User;
import com.phishshield.security.JwtTokenProvider;
import com.phishshield.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 3600000;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract user id")
    void testGenerateAndValidateToken() {
        Role role = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
        User user = User.builder()
                .id(42L)
                .username("testuser")
                .email("testuser@example.com")
                .passwordHash("hashed")
                .role(role)
                .isActive(true)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = tokenProvider.generateToken(auth);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromJwt(token));
    }

    @Test
    @DisplayName("Should reject malformed or altered JWT token")
    void testRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
    }
}
