package com.nexuscore.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la petición de login.
 * Solo contiene los campos necesarios para la autenticación.
 *
 * [MITIGACIÓN APLICADA]: Usar DTOs en lugar de entidades en los
 * endpoints públicos evita exponer campos internos (como el hash
 * de contraseña o el ID interno) en las respuestas de la API.
 */
@Data
public class AuthRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100)
    private String password;
}
