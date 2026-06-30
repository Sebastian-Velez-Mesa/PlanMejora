package com.nexuscore.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta tras autenticación exitosa.
 * Contiene el token JWT y los datos básicos del usuario.
 *
 * [MITIGACIÓN APLICADA]: NUNCA incluir el hash de contraseña ni datos
 * sensibles internos en la respuesta. Solo se devuelve el token JWT.
 */
@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private String mensaje;
}
