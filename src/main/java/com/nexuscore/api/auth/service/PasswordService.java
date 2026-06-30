package com.nexuscore.api.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ================================================================
 *  MÓDULO: Auth - Servicio de Gestión de Contraseñas
 *  Activo 2: Repositorio de Credenciales de Usuarios
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Las contraseñas del ERP original se almacenaban en texto plano
 *   o con hash MD5 sin salt, lo que las hace vulnerables a:
 *   - Ataques de rainbow tables (tablas precalculadas).
 *   - Fuerza bruta acelerada por GPUs (MD5 = ~10 billones hash/seg).
 *   - Si la base de datos es robada, TODAS las contraseñas quedan expuestas.
 *
 * [MITIGACIÓN APLICADA]:
 *   Se usa BCryptPasswordEncoder con cost factor 12.
 *   BCrypt aplica un salt aleatorio de 128 bits por contraseña,
 *   lo que hace que cada hash sea único aunque las contraseñas sean iguales.
 *   El cost factor 12 implica 2^12 = 4096 iteraciones, haciendo que
 *   la fuerza bruta sea computacionalmente inviable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Genera un hash BCrypt seguro para la contraseña dada.
     *
     * Ejemplo de resultado:
     *   Input:  "MiClave123"
     *   Output: "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     *
     * Cada llamada genera un hash DIFERENTE por el salt aleatorio integrado.
     *
     * @param rawPassword Contraseña en texto plano (NUNCA almacenar esto)
     * @return Hash BCrypt seguro listo para almacenar en base de datos
     */
    public String hashPassword(String rawPassword) {
        // [VULNERABILIDAD ORIGINAL]: return DigestUtils.md5Hex(rawPassword);
        // [MITIGACIÓN APLICADA]: BCrypt con salt automático y cost factor 12.
        String hashedPassword = passwordEncoder.encode(rawPassword);
        log.info("[SEGURIDAD] Contraseña procesada con BCrypt (cost=12). Hash generado exitosamente.");
        return hashedPassword;
    }

    /**
     * Verifica que una contraseña en texto plano coincide con su hash BCrypt.
     *
     * BCrypt extrae el salt del propio hash almacenado, por lo que
     * la comparación es siempre segura y en tiempo constante
     * (previene timing attacks).
     *
     * @param rawPassword     Contraseña que el usuario intenta usar
     * @param hashedPassword  Hash almacenado en la base de datos
     * @return true si la contraseña es válida
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);

        if (matches) {
            log.info("[SEGURIDAD] Verificación de contraseña exitosa.");
        } else {
            // [MITIGACIÓN APLICADA]: Log de intento fallido sin revelar detalles.
            // No se expone si el usuario existe o si la contraseña es incorrecta.
            log.warn("[SEGURIDAD] Intento de verificación de contraseña FALLIDO.");
        }

        return matches;
    }

    /**
     * Valida que una contraseña cumple con la política de seguridad del ERP.
     *
     * Política mínima:
     *   - Al menos 8 caracteres
     *   - Al menos una letra mayúscula
     *   - Al menos un número
     *   - Al menos un carácter especial
     *
     * [VULNERABILIDAD ORIGINAL]: Sin validación de complejidad de contraseñas.
     * [MITIGACIÓN APLICADA]: Política explícita aplicada antes de hashing.
     *
     * @param rawPassword Contraseña a validar
     * @return true si cumple la política de seguridad
     */
    public boolean validatePasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            log.warn("[POLÍTICA] Contraseña rechazada: longitud insuficiente (mín. 8 caracteres).");
            return false;
        }

        boolean hasUpperCase  = rawPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase  = rawPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit      = rawPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial    = rawPassword.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{}|;:,.<>?".indexOf(c) >= 0);

        boolean valid = hasUpperCase && hasLowerCase && hasDigit && hasSpecial;

        if (!valid) {
            log.warn("[POLÍTICA] Contraseña rechazada: no cumple requisitos de complejidad.");
        } else {
            log.info("[POLÍTICA] Contraseña aprobada por política de seguridad.");
        }

        return valid;
    }
}
