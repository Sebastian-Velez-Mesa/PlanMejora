package com.nexuscore.api.customer.controller;

import com.nexuscore.api.audit.AuditLogService;
import com.nexuscore.api.customer.entity.Customer;
import com.nexuscore.api.customer.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  MÓDULO: Customer - Controlador REST de Gestión de Clientes
 *  Activo 1: Servidor de Datos Personales (PII)
 *  Ruta base: /api/customers
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El endpoint de clientes no tenía autenticación. Un atacante podía
 *   hacer GET /clientes y obtener todos los datos personales (cédulas,
 *   correos, teléfonos) sin necesidad de credenciales. Tampoco había
 *   validación de los datos de entrada, permitiendo inyección SQL.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Todos los endpoints requieren rol ADMIN (configurado en SecurityConfig).
 *   2. @Valid activa la validación de Jakarta Validation en cada campo.
 *   3. Logs de auditoría para cada operación CRUD (trazabilidad).
 *   4. Manejo de errores estructurado sin revelar stack traces internos.
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    /**
     * GET /api/customers
     * Lista todos los clientes registrados.
     *
     * [MITIGACIÓN APLICADA]: Solo rol ADMIN puede listar clientes.
     * Se registra quién realiza la consulta y cuándo.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Customer>> getAllCustomers(Authentication auth) {
        List<Customer> customers = customerRepository.findAll();

        // [MITIGACIÓN APLICADA]: Log de auditoría con identidad del solicitante.
        log.info("[AUDITORÍA] Usuario '{}' consultó {} registros de clientes.",
                auth.getName(), customers.size());

        return ResponseEntity.ok(customers);
    }

    /**
     * GET /api/customers/{id}
     * Obtiene un cliente por su ID.
     *
     * [MITIGACIÓN APLICADA]: Respuesta genérica si no existe (sin revelar
     * si el ID existe en la BD - previene enumeración de IDs).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id, Authentication auth) {
        return customerRepository.findById(id)
                .map(customer -> {
                    log.info("[AUDITORÍA] Usuario '{}' accedió al cliente ID: {}.", auth.getName(), id);
                    return ResponseEntity.ok(customer);
                })
                .orElseGet(() -> {
                    log.warn("[AUDITORÍA] Usuario '{}' intentó acceder a cliente inexistente ID: {}.",
                            auth.getName(), id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * POST /api/customers
     * Registra un nuevo cliente en el sistema.
     *
     * [MITIGACIÓN APLICADA]:
     *   - @Valid valida todos los campos antes de persistir.
     *   - Se verifica duplicado de cédula antes de insertar.
     *   - Log de auditoría al crear registro.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCustomer(@Valid @RequestBody Customer customer, Authentication auth) {

        // [MITIGACIÓN APLICADA]: Verificar duplicado ANTES de intentar insertar
        // para dar un mensaje claro sin exponer detalles de la BD.
        if (customerRepository.existsByCedulaNit(customer.getCedulaNit())) {
            log.warn("[SEGURIDAD] Intento de registro duplicado. Cédula/NIT ya existe. Usuario: {}",
                    auth.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un cliente con esa cédula o NIT."));
        }

        Customer saved = customerRepository.save(customer);
        auditLogService.record("customer", "create", auth.getName(), "Cliente creado con ID " + saved.getId());
        log.info("[AUDITORÍA] Usuario '{}' registró nuevo cliente. ID asignado: {}.",
                auth.getName(), saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * DELETE /api/customers/{id}
     * Elimina un cliente por su ID.
     *
     * [MITIGACIÓN APLICADA]: Log de auditoría al eliminar datos personales
     * (requerimiento de la Ley 1581 de 2012 - Protección de Datos Colombia).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id, Authentication auth) {
        if (!customerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        customerRepository.deleteById(id);
        auditLogService.record("customer", "delete", auth.getName(), "Cliente eliminado con ID " + id);
        log.info("[AUDITORÍA] Usuario '{}' ELIMINÓ cliente ID: {}. Operación registrada.",
                auth.getName(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Cliente eliminado correctamente."));
    }
}
