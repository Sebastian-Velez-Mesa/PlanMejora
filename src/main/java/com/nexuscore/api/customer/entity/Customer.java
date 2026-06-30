package com.nexuscore.api.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  MÓDULO: Customer - Entidad de Datos Personales de Clientes
 *  Activo 1: Servidor de Datos Personales (PII - Información Sensible)
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Los datos personales (cédula, correo, teléfono) se almacenaban
 *   sin ningún control de acceso, sin cifrado en reposo, y eran
 *   accesibles vía HTTP plano. Cualquier persona en la red local
 *   podía interceptar y leer la información con un sniffer.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Validación de formato en todos los campos (Jakarta Validation).
 *   2. La ruta del controlador está protegida con hasRole('ADMIN').
 *   3. En producción, la columna "cedula" debería cifrarse a nivel
 *      de base de datos (ej. PostgreSQL pgcrypto o cifrado de columna).
 *   4. Campo "createdAt" para trazabilidad de cuándo se registró el dato.
 */
@Entity
@Table(name = "clientes")
@Audited
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del cliente.
     * [MITIGACIÓN]: Validación de longitud para prevenir buffer overflow
     * y ataques de inyección en campos de texto.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Cédula o NIT del cliente (dato sensible - PII).
     * [MITIGACIÓN]: Patrón regex para aceptar solo valores numéricos válidos.
     * [PRODUCCIÓN]:  Considerar cifrado de columna con AES-256.
     */
    @NotBlank(message = "La cédula o NIT es obligatorio")
    @Pattern(regexp = "^[0-9]{6,15}$", message = "Cédula o NIT debe contener solo dígitos (6-15)")
    @Column(nullable = false, unique = true, length = 15, name = "cedula_nit")
    private String cedulaNit;

    /**
     * Correo electrónico del cliente.
     * [MITIGACIÓN]: Validación de formato de email para prevenir
     * datos corruptos y posibles ataques de enumeración.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    /**
     * Número de teléfono de contacto.
     * [MITIGACIÓN]: Solo dígitos y formato colombiano (+57 opcional).
     */
    @Pattern(regexp = "^(\\+57)?[0-9]{10}$",
             message = "Teléfono debe ser un número colombiano válido (10 dígitos)")
    @Column(length = 15)
    private String telefono;

    /**
     * Fecha y hora de registro del cliente.
     * [MITIGACIÓN APLICADA]: Trazabilidad de cuándo se ingresaron los datos.
     * Útil para auditorías de cumplimiento (Ley 1581 de 2012 - Habeas Data).
     */
    @Column(nullable = false, updatable = false, name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}
