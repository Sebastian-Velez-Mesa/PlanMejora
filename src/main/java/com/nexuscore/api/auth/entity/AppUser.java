package com.nexuscore.api.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

/**
 * ================================================================
 *  MÓDULO: Auth - Entidad de Usuario del Sistema
 *  Activo 2: Repositorio de Credenciales
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Contraseñas almacenadas en texto plano o con MD5.
 *   Sin separación de roles ni principio de mínimo privilegio.
 *
 * [MITIGACIÓN APLICADA]:
 *   - Campo "password" almacena HASH BCrypt, nunca texto plano.
 *   - Campo "role" define el nivel de acceso (ROLE_ADMIN / ROLE_USER).
 *   - Campo "activo" permite deshabilitar cuentas sin eliminarlas
 *     (útil para mantener historial de auditoría).
 */
@Entity
@Table(name = "usuarios_sistema")
@Audited
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * [MITIGACIÓN APLICADA]: Este campo SIEMPRE contiene un hash BCrypt.
     * NUNCA se almacena ni se devuelve la contraseña en texto plano.
     * Ejemplo de valor: "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p..."
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Rol del usuario en el sistema.
     * Valores válidos: ROLE_ADMIN, ROLE_USER
     *
     * [MITIGACIÓN APLICADA]: Principio de mínimo privilegio.
     * Los usuarios con ROLE_USER no pueden acceder a datos de clientes
     * ni de nómina (controlado en SecurityConfig).
     */
    @Column(nullable = false, length = 20)
    private String role;

    /**
     * Indica si la cuenta está activa.
     * [MITIGACIÓN APLICADA]: Permite bloquear cuentas comprometidas
     * sin eliminar el historial de auditoría del usuario.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
