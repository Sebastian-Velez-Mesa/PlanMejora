package com.nexuscore.api.security.jwt;

import com.nexuscore.api.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ================================================================
 *  MÓDULO: Security - Filtro de Autenticación JWT
 *  Activo 2: Validación de Token en cada petición HTTP
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Sin filtro de autenticación: cualquier petición HTTP llegaba
 *   directamente a los controladores sin validar identidad.
 *
 * [MITIGACIÓN APLICADA]:
 *   Este filtro intercepta CADA petición antes de llegar al controlador.
 *   Extrae el JWT del header "Authorization: Bearer <token>",
 *   valida su firma y carga el contexto de seguridad solo si es válido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header Authorization o no empieza con "Bearer ", pasar al siguiente filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token (eliminar el prefijo "Bearer ")
        final String jwt = authHeader.substring(7);
        String username = null;

        try {
            username = jwtUtils.extractUsername(jwt);
        } catch (Exception e) {
            log.warn("[JWT-FILTER] No se pudo extraer el usuario del token: {}", e.getMessage());
        }

        // Si hay un usuario y aún no está autenticado en el contexto de seguridad
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtils.isTokenValid(jwt, userDetails)) {
                // [MITIGACIÓN APLICADA]: Construir contexto de autenticación solo
                // después de validar firma y expiración del token.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("[JWT-FILTER] Autenticación exitosa para: {} | Ruta: {}",
                        username, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
