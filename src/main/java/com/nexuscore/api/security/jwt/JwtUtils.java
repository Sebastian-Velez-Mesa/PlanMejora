package com.nexuscore.api.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ================================================================
 *  MÓDULO: Security - Utilidad de JSON Web Tokens (JWT)
 *  Activo 2: Gestión de Credenciales y Sesiones
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El sistema original usaba cookies de sesión sin expiración y sin firma.
 *   Esto permitía: ataques de session hijacking, tokens que nunca expiran,
 *   y falsificación de identidad al modificar el ID de sesión.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. JWT firmado con HMAC-SHA512 (algoritmo HS512) – imposible de falsificar
 *      sin conocer la clave secreta de 512 bits.
 *   2. Expiración configurable (por defecto 1 hora).
 *   3. El token es stateless: el servidor no almacena sesiones.
 *   4. Cualquier modificación del payload invalida la firma.
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${nexuscore.jwt.secret}")
    private String jwtSecret;

    @Value("${nexuscore.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Genera un token JWT firmado para el usuario autenticado.
     *
     * El token contiene:
     *   - subject: nombre de usuario
     *   - roles: lista de roles del usuario
     *   - iat (issued at): momento de emisión
     *   - exp (expiration): momento de expiración (1 hora)
     *
     * @param userDetails Detalles del usuario autenticado
     * @return Token JWT firmado (String)
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", userDetails.getAuthorities().toString());

        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                // [MITIGACIÓN APLICADA]: Expiración de 1 hora para limitar
                // la ventana de uso si un token es interceptado.
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                // [MITIGACIÓN APLICADA]: Firma HS512 con clave de 512 bits.
                // Un atacante sin la clave no puede generar tokens válidos.
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        log.info("[JWT] Token generado para usuario: {}", userDetails.getUsername());
        return token;
    }

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifica que el token sea válido para el usuario dado.
     * Valida: firma, expiración y que el subject coincida.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            // [MITIGACIÓN APLICADA]: Log de token inválido sin revelar detalles
            // internos al cliente (evita information disclosure).
            log.warn("[JWT] Token inválido o manipulado detectado: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Deriva la clave de firma HMAC-SHA512 desde el secreto configurado.
     * La clave mínima para HS512 es 512 bits (64 bytes).
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
