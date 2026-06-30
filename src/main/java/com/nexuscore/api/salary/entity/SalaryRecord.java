package com.nexuscore.api.salary.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 *  MÓDULO: Salary - Entidad de Nómina y Salarios
 *  Activo 5: Módulo de Salarios y Nómina
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Los datos de nómina (salario base, cuenta bancaria) se almacenaban
 *   sin cifrado y eran accesibles sin autenticación. La cuenta bancaria
 *   en texto plano en la BD representa un riesgo crítico de exposición
 *   de datos financieros personales de los empleados.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Endpoint protegido exclusivamente con hasRole('ADMIN').
 *   2. La "cuentaBancaria" en producción debería almacenarse cifrada
 *      con AES-256 (columna cifrada en BD o a nivel de aplicación).
 *   3. BigDecimal para valores salariales (precisión exacta).
 *   4. Campo de auditoría "fechaRegistro" inmutable.
 *   5. Validación estricta del número de cuenta bancaria.
 */
@Entity
@Table(name = "nomina_empleados")
@Audited
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del empleado.
     */
    @NotBlank(message = "El nombre del empleado es obligatorio")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String empleado;

    /**
     * Número de cédula del empleado.
     */
    @NotBlank(message = "La cédula del empleado es obligatoria")
    @Pattern(regexp = "^[0-9]{6,15}$", message = "Cédula inválida")
    @Column(nullable = false, unique = true, length = 15)
    private String cedula;

    /**
     * Salario base mensual del empleado.
     * [MITIGACIÓN APLICADA]: BigDecimal para evitar errores de precisión
     * en cálculos de nómina (descuentos, aportes parafiscales, etc.).
     */
    @NotNull(message = "El salario base es obligatorio")
    @DecimalMin(value = "1300000.00", message = "El salario no puede ser inferior al mínimo legal")
    @Column(nullable = false, precision = 12, scale = 2, name = "salario_base")
    private BigDecimal salarioBase;

    /**
     * Número de cuenta bancaria del empleado (dato altamente sensible).
     *
     * [VULNERABILIDAD ORIGINAL]: Almacenado en texto plano.
     * [MITIGACIÓN APLICADA]: En producción cifrar con AES-256 antes de persistir.
     *   Implementación recomendada: @Convert(converter = CryptoConverter.class)
     *   donde CryptoConverter cifra/descifra usando una clave externa (KMS).
     *
     * Por ahora se valida el formato (solo dígitos, 10-20 caracteres).
     */
    @NotBlank(message = "La cuenta bancaria es obligatoria")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "Cuenta bancaria debe tener entre 10 y 20 dígitos")
    @Column(nullable = false, length = 25, name = "cuenta_bancaria")
    private String cuentaBancaria;

    /**
     * Cargo o posición del empleado en la empresa.
     */
    @Column(length = 80)
    private String cargo;

    /**
     * Fecha de registro de la nómina. Inmutable tras la creación.
     */
    @Column(nullable = false, updatable = false, name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}
