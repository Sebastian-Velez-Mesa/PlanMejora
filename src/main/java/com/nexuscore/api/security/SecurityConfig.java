package com.nexuscore.api.security;

import com.nexuscore.api.auth.service.UserDetailsServiceImpl;
import com.nexuscore.api.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ================================================================
 *  MÓDULO: Security - Configuración Central de Spring Security
 *  Activo: TODOS los módulos del ERP (Activos 1-5 de la guía)
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El sistema ERP original operaba sobre HTTP plano (sin TLS), sin
 *   ningún mecanismo de autenticación ni autorización. Cualquier
 *   usuario podía acceder a rutas de datos de clientes, nómina y
 *   módulo financiero sin credenciales. Faltaba separación de roles.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Todas las rutas sensibles requieren autenticación JWT.
 *   2. Las rutas de /customers y /salaries exigen rol ADMIN.
 *   3. Se eliminan las sesiones HTTP (SessionCreationPolicy.STATELESS)
 *      para prevenir ataques de session fixation y CSRF clásico.
 *   4. Se usa BCrypt con strength=12 para hashing de contraseñas.
 *   5. CSRF deshabilitado en APIs REST stateless (mitigado por JWT).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    /**
     * Cadena de filtros de seguridad HTTP.
     * Define qué rutas están protegidas y bajo qué condiciones.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // [MITIGACIÓN APLICADA]: CSRF deshabilitado en APIs REST stateless.
            // La protección se delega al token JWT con firma HMAC-SHA512.
            .csrf(AbstractHttpConfigurer::disable)

            // [MITIGACIÓN APLICADA]: Sin estado de sesión HTTP.
            // Previene session fixation y reduce superficie de ataque.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> {
                // Rutas públicas: login y registro
                auth.requestMatchers("/api/auth/**").permitAll();

                // Consola H2 solo en desarrollo (en producción se elimina)
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }

                // [MITIGACIÓN APLICADA]: Datos de Clientes (Activo 1 - PII)
                // Solo administradores pueden leer y escribir datos personales.
                auth.requestMatchers("/api/customers/**").hasRole("ADMIN");

                // [MITIGACIÓN APLICADA]: Salarios y Nómina (Activo 5)
                // Información financiera de empleados: acceso exclusivo ADMIN.
                auth.requestMatchers("/api/salaries/**").hasRole("ADMIN");

                // [MITIGACIÓN APLICADA]: Módulo Financiero (Activo 4)
                // Lectura para ADMIN y USER; escritura solo ADMIN.
                auth.requestMatchers(HttpMethod.GET, "/api/financial/**").hasAnyRole("ADMIN", "USER");
                auth.requestMatchers(HttpMethod.POST, "/api/financial/**").hasRole("ADMIN");

                // [MITIGACIÓN APLICADA]: Lógica de negocio (Activo 3)
                // Cualquier usuario autenticado puede consultar.
                auth.requestMatchers("/api/core/**").authenticated();

                // Todo lo demás requiere autenticación
                auth.anyRequest().authenticated();
            })

            // Proveedor de autenticación personalizado con BCrypt
            .authenticationProvider(authenticationProvider())

            // [MITIGACIÓN APLICADA]: Filtro JWT se ejecuta ANTES de la
            // autenticación estándar de usuario/contraseña.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Necesario para la consola H2 en iframe (solo desarrollo)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * [MITIGACIÓN APLICADA]: BCryptPasswordEncoder con strength=12.
     * BCrypt aplica un salt aleatorio por contraseña automáticamente,
     * resistente a ataques de fuerza bruta y rainbow tables.
     *
     * NUNCA usar MD5, SHA1 o SHA256 sin salt para contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Proveedor de autenticación que vincula el servicio de usuarios
     * con el encoder de contraseñas BCrypt.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el AuthenticationManager para ser inyectado en el
     * controlador de autenticación (login endpoint).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
