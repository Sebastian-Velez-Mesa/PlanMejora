package com.nexuscore.api.financial.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 *  MÓDULO: Financial - Entidad de Transacción Financiera
 *  Activo 4: Módulo Financiero y Contable
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El historial de transacciones no tenía logs de auditoría.
 *   Los registros podían ser modificados o eliminados sin trazabilidad.
 *   No había separación entre quien crea la transacción y quien la aprueba.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Campos "fechaCreacion" y "creadoPor" son inmutables (updatable=false).
 *   2. El logger en FinancialService registra cada operación con timestamp.
 *   3. Tipo de transacción (INGRESO/EGRESO) categoriza el flujo de dinero.
 *   4. BigDecimal para valores monetarios (evita errores de precisión de double).
 */
@Entity
@Table(name = "transacciones_financieras")
@Audited
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String referencia;

    /**
     * [MITIGACIÓN APLICADA]: BigDecimal para valores monetarios.
     * Usar float/double para dinero causa errores de redondeo.
     * Ej: 0.1 + 0.2 con double = 0.30000000000000004
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private String tipo; // INGRESO | EGRESO

    @Column(length = 200)
    private String descripcion;

    /**
     * [MITIGACIÓN APLICADA]: Fecha de creación inmutable.
     * updatable=false garantiza que este campo NO puede ser modificado
     * después de la inserción, creando un registro de auditoría confiable.
     */
    @Column(nullable = false, updatable = false, name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    /**
     * [MITIGACIÓN APLICADA]: Registro de quién creó la transacción.
     * Inmutable después de la creación (no-repudio).
     */
    @Column(nullable = false, updatable = false, name = "creado_por", length = 50)
    private String creadoPor;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
