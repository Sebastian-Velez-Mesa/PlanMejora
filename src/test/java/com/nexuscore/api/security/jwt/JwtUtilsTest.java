package com.nexuscore.api.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    void generateTokenShouldWorkWithShortSecrets() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "short-secret");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L);

        UserDetails userDetails = new User("admin", "password", Collections.emptyList());

        String token = assertDoesNotThrow(() -> jwtUtils.generateToken(userDetails));

        assertNotNull(token);
        assertTrue(token.length() > 20);
        assertTrue(jwtUtils.isTokenValid(token, userDetails));
    }
}
