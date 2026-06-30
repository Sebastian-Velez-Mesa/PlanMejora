package com.nexuscore.api.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de gestión de intentos fallidos de inicio de sesión.
 *
 * [MEJORA DE SEGURIDAD]: Bloquea cuentas temporalmente después de varios
 * intentos fallidos para mitigar ataques de fuerza bruta.
 */
@Slf4j
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attempts.remove(username);
        log.info("[SEGURIDAD] Reiniciados intentos fallidos de inicio de sesión para usuario: {}.", username);
    }

    public void loginFailed(String username) {
        int currentAttempts = attempts.getOrDefault(username, 0) + 1;
        attempts.put(username, currentAttempts);
        log.warn("[SEGURIDAD] Intento fallido #{} para usuario: {}.", currentAttempts, username);
    }

    public boolean isBlocked(String username) {
        return attempts.getOrDefault(username, 0) >= MAX_ATTEMPTS;
    }

    public int getAttempts(String username) {
        return attempts.getOrDefault(username, 0);
    }
}
