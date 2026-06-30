package com.nexuscore.api.auth.controller;

import com.nexuscore.api.auth.dto.AuthRequest;
import com.nexuscore.api.auth.dto.AuthResponse;
import com.nexuscore.api.auth.entity.AppUser;
import com.nexuscore.api.auth.repository.UserRepository;
import com.nexuscore.api.auth.service.LoginAttemptService;
import com.nexuscore.api.auth.service.PasswordService;
import com.nexuscore.api.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================================
 *  MÓDULO: Auth - Controlador de Autenticación y Registro
 *  Activo 2: Gestión de Credenciales
 *  Ruta base: /api/auth
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El sistema original no tenía endpoint de autenticación formal.
 *   Las credenciales viajaban por HTTP sin cifrar (texto plano visible
 *   con Wireshark). No había generación de tokens, ni control de intentos
 *   fallidos, ni logs de acceso.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. AuthenticationManager de Spring Security valida credenciales.
 *   2. Si la autenticación es exitosa, se genera un JWT firmado HS512.
 *   3. La contraseña se hashea con BCrypt antes de guardar.
 *   4. Log de intentos de login (exitosos y fallidos) para auditoría.
 *   5. En caso de falla, respuesta genérica (no revela si existe el usuario).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final LoginAttemptService loginAttemptService;

    /**
     * POST /api/auth/login
     * Endpoint público de inicio de sesión.
     *
     * Flujo:
     *   1. Recibe {username, password}
     *   2. Spring Security valida contra BCrypt hash almacenado
     *   3. Si válido → genera JWT y lo retorna
     *   4. Si inválido → respuesta 401 genérica
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {

        if (loginAttemptService.isBlocked(authRequest.getUsername())) {
            log.warn("[SEGURIDAD] Cuenta temporalmente bloqueada por múltiples intentos fallidos: {}.",
                    authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("error", "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."));
        }

        try {
            // [MITIGACIÓN APLICADA]: AuthenticationManager usa BCrypt internamente
            // para comparar la contraseña con el hash almacenado.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateToken(userDetails);
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            loginAttemptService.loginSucceeded(authRequest.getUsername());
            log.info("[AUDITORÍA] Login EXITOSO para usuario: '{}'.", authRequest.getUsername());

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .username(userDetails.getUsername())
                    .role(role)
                    .mensaje("Autenticación exitosa. Token válido por 1 hora.")
                    .build());

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(authRequest.getUsername());
            // [MITIGACIÓN APLICADA]: Log de intento fallido para detectar
            // ataques de fuerza bruta. Respuesta genérica al cliente.
            log.warn("[SEGURIDAD] Login FALLIDO para usuario: '{}'. Intento #{}.",
                    authRequest.getUsername(), loginAttemptService.getAttempts(authRequest.getUsername()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas."));
        }
    }

    /**
     * POST /api/auth/register
     * Registro de nuevo usuario en el sistema.
     *
     * [MITIGACIÓN APLICADA]:
     *   - Validación de política de contraseñas antes de guardar.
     *   - Hash BCrypt aplicado ANTES de persistir en la BD.
     *   - Nunca se almacena la contraseña en texto plano.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest authRequest) {

        // Verificar si el usuario ya existe
        if (userRepository.existsByUsername(authRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El nombre de usuario ya está en uso."));
        }

        // [MITIGACIÓN APLICADA]: Validar política de contraseñas.
        if (!passwordService.validatePasswordPolicy(authRequest.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "La contraseña no cumple la política de seguridad: " +
                            "mínimo 8 caracteres, una mayúscula, un número y un carácter especial."));
        }

        // [MITIGACIÓN APLICADA]: Hash BCrypt antes de persistir.
        String hashedPassword = passwordService.hashPassword(authRequest.getPassword());

        AppUser newUser = AppUser.builder()
                .username(authRequest.getUsername())
                .password(hashedPassword)    // Solo se guarda el HASH, nunca el texto plano
                .role("ROLE_USER")           // Por defecto, rol de usuario básico
                .activo(true)
                .build();

        userRepository.save(newUser);
        log.info("[AUDITORÍA] Nuevo usuario registrado: '{}'.", authRequest.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado exitosamente."));
    }
}
