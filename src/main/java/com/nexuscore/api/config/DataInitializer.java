package com.nexuscore.api.config;

import com.nexuscore.api.auth.entity.AppUser;
import com.nexuscore.api.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ================================================================
 *  INICIALIZADOR DE DATOS - DataInitializer
 *  Crea usuarios de prueba al arrancar la aplicación.
 * ================================================================
 *
 * [MITIGACIÓN APLICADA]:
 *   Las contraseñas de los usuarios de prueba se hashean con BCrypt
 *   antes de almacenarse. Nunca se almacenan en texto plano.
 *
 *   Usuarios creados:
 *     - admin / Admin@123! → ROLE_ADMIN  (acceso total)
 *     - user  / User@123!  → ROLE_USER   (acceso limitado)
 *
 *   Estos usuarios son SOLO para el entorno educativo/demo.
 *   En producción, el registro se hace por /api/auth/register.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            crearUsuarioAdmin();
            crearUsuarioNormal();
            log.info("[SISTEMA] Usuarios de demostración creados exitosamente.");
            log.info("[SISTEMA] Admin: admin / Admin@123!");
            log.info("[SISTEMA] User:  user  / User@123!");
        }
    }

    private void crearUsuarioAdmin() {
        AppUser admin = AppUser.builder()
                .username("admin")
                // [MITIGACIÓN APLICADA]: Contraseña hasheada con BCrypt strength=12
                .password(passwordEncoder.encode("Admin@123!"))
                .role("ROLE_ADMIN")
                .activo(true)
                .build();
        userRepository.save(admin);
    }

    private void crearUsuarioNormal() {
        AppUser user = AppUser.builder()
                .username("user")
                .password(passwordEncoder.encode("User@123!"))
                .role("ROLE_USER")
                .activo(true)
                .build();
        userRepository.save(user);
    }
}
