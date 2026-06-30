package com.nexuscore.api.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(new BCryptPasswordEncoder(12));

    @Test
    void shouldAcceptComplexPassword() {
        assertTrue(passwordService.validatePasswordPolicy("SecurePass1!"));
    }

    @Test
    void shouldRejectPasswordWithoutUppercaseLowercaseDigitAndSpecialCharacter() {
        assertFalse(passwordService.validatePasswordPolicy("weakpass"));
        assertFalse(passwordService.validatePasswordPolicy("WEAKPASS1"));
        assertFalse(passwordService.validatePasswordPolicy("Weakpass"));
    }
}
