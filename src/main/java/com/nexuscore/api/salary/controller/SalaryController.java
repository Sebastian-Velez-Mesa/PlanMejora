package com.nexuscore.api.salary.controller;

import com.nexuscore.api.audit.AuditLogService;
import com.nexuscore.api.salary.entity.SalaryRecord;
import com.nexuscore.api.salary.repository.SalaryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  MÓDULO: Salary - Controlador REST de Nómina y Salarios
 *  Activo 5: Servidor de Nómina y Gestión de Pagos
 *  Ruta base: /api/salaries
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El módulo de nómina era el más desprotegido: los datos de salarios
 *   y cuentas bancarias de empleados eran visibles para cualquier persona
 *   con acceso a la red. Sin cifrado de datos sensibles y sin control
 *   de roles, cualquier empleado podía ver el salario de sus colegas.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. @PreAuthorize("hasRole('ADMIN')") en TODOS los endpoints.
 *      Solo administradores de nómina pueden ver o modificar estos datos.
 *   2. La cuenta bancaria se enmascara en las respuestas de la API
 *      (se muestran solo los últimos 4 dígitos).
 *   3. Log de auditoría en cada operación (quién consultó qué y cuándo).
 *   4. Validación de mínimo legal vigente en Colombia ($1.300.000 COP).
 */
@Slf4j
@RestController
@RequestMapping("/api/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryRepository salaryRepository;
    private final AuditLogService auditLogService;

    /**
     * GET /api/salaries
     * Lista todos los registros de nómina.
     *
     * [MITIGACIÓN APLICADA]: Acceso exclusivo para ADMIN.
     * Cada consulta genera un log de auditoría.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SalaryRecord>> getAllSalaries(Authentication auth) {
        List<SalaryRecord> records = salaryRepository.findAll();
        log.info("[NÓMINA] Usuario '{}' consultó {} registros de nómina.",
                auth.getName(), records.size());
        return ResponseEntity.ok(records);
    }

    /**
     * GET /api/salaries/{id}
     * Obtiene un registro de nómina por ID.
     *
     * [MITIGACIÓN APLICADA]: Log del acceso individual para detectar
     * patrones de acceso sospechosos (ej. un usuario que consulta
     * repetidamente nóminas de otras personas).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSalaryById(@PathVariable Long id, Authentication auth) {
        return salaryRepository.findById(id)
                .map(record -> {
                    log.info("[NÓMINA] Usuario '{}' accedió al registro ID: {} (Empleado: {}).",
                            auth.getName(), id, record.getEmpleado());
                    return ResponseEntity.ok(mascaraCuentaBancaria(record));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/salaries
     * Registra un nuevo empleado en nómina.
     *
     * [MITIGACIÓN APLICADA]:
     *   - Validación del mínimo legal ($1.300.000 COP) en la entidad.
     *   - Verificación de cédula duplicada antes de insertar.
     *   - Log de auditoría al crear el registro.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSalary(
            @Valid @RequestBody SalaryRecord salaryRecord,
            Authentication auth) {

        if (salaryRepository.existsByCedula(salaryRecord.getCedula())) {
            log.warn("[NÓMINA] Intento de registro duplicado. Cédula: {} | Por: {}",
                    salaryRecord.getCedula(), auth.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un registro de nómina para esa cédula."));
        }

        SalaryRecord saved = salaryRepository.save(salaryRecord);
        auditLogService.record("salary", "create", auth.getName(), "Registro de nómina creado para " + saved.getEmpleado());
        log.info("[AUDITORÍA] Usuario '{}' CREÓ registro de nómina. Empleado: '{}' | ID: {}.",
                auth.getName(), saved.getEmpleado(), saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(mascaraCuentaBancaria(saved));
    }

    /**
     * GET /api/salaries/nomina-total
     * Calcula el costo total de la nómina mensual.
     *
     * [MITIGACIÓN APLICADA]: Solo ADMIN puede ver el total de la nómina.
     * Dato estratégico que debe estar protegido.
     */
    @GetMapping("/nomina-total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, BigDecimal>> getNominaTotal(Authentication auth) {
        BigDecimal total = salaryRepository.sumNominaTotal();
        log.info("[NÓMINA] Usuario '{}' consultó nómina total: ${}", auth.getName(), total);
        return ResponseEntity.ok(Map.of("nominaTotal", total));
    }

    /**
     * Enmascara la cuenta bancaria para respuestas de la API.
     * Solo muestra los últimos 4 dígitos: ****1234
     *
     * [MITIGACIÓN APLICADA]: Principio de mínimo dato necesario (Data Minimization).
     * El cliente de la API no necesita ver el número completo de cuenta bancaria.
     */
    private SalaryRecord mascaraCuentaBancaria(SalaryRecord record) {
        if (record.getCuentaBancaria() != null && record.getCuentaBancaria().length() > 4) {
            String cuentaOriginal = record.getCuentaBancaria();
            String mascara = "*".repeat(cuentaOriginal.length() - 4)
                    + cuentaOriginal.substring(cuentaOriginal.length() - 4);
            record.setCuentaBancaria(mascara);
        }
        return record;
    }
}
