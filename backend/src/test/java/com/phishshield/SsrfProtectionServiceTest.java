package com.phishshield;

import com.phishshield.exception.SSRFSecurityException;
import com.phishshield.service.SSRFProtectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SsrfProtectionServiceTest {

    private SSRFProtectionService ssrfService;

    @BeforeEach
    void setUp() {
        ssrfService = new SSRFProtectionService();
        ReflectionTestUtils.setField(ssrfService, "ssrfProtectionEnabled", true);
        ReflectionTestUtils.setField(ssrfService, "allowPrivateIps", false);
    }

    @Test
    @DisplayName("Should block loopback IPv4 address 127.0.0.1")
    void testBlockLoopbackIp() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("http://127.0.0.1:8080/admin");
        });
    }

    @Test
    @DisplayName("Should block localhost domain")
    void testBlockLocalhost() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("http://localhost:3000");
        });
    }

    @Test
    @DisplayName("Should block AWS metadata IP 169.254.169.254")
    void testBlockCloudMetadata() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("http://169.254.169.254/latest/meta-data/");
        });
    }

    @Test
    @DisplayName("Should block disallowed file:// protocol")
    void testBlockDisallowedScheme() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("file:///etc/passwd");
        });
    }

    @Test
    @DisplayName("Should block credentials embedded in URL")
    void testBlockEmbeddedCredentials() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("http://admin:secret@example.com");
        });
    }

    @Test
    @DisplayName("Should block non-standard prohibited ports")
    void testBlockNonStandardPorts() {
        assertThrows(SSRFSecurityException.class, () -> {
            ssrfService.validateUrl("http://example.com:22");
        });
    }

    @Test
    @DisplayName("Should allow valid public URLs on standard web ports")
    void testAllowValidPublicUrl() {
        assertDoesNotThrow(() -> {
            ssrfService.validateUrl("https://example.com/login");
        });
    }
}
