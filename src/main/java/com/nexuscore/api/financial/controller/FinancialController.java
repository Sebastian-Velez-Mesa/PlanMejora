package com.nexuscore.api.financial.controller;

import com.nexuscore.api.financial.entity.Transaction;
import com.nexuscore.api.financial.service.FinancialService;
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
 *  MÓDULO: Financial - Controlador REST de Transacciones
 *  Activo 4: Módulo Financiero y Contable
 *  Ruta base: /api/financial
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   Sin control de acceso: cualquier usuario podía registrar transacciones,
 *   consultar balances y ver el historial completo. Sin logs de auditoría,
 *   era imposible detectar transacciones fraudulentas.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. GET permite ADMIN y USER (lectura controlada).
 *   2. POST solo ADMIN puede registrar transacciones.
 *   3. FinancialService registra cada operación en log inmutable.
 */
@Slf4j
@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    /**
     * GET /api/financial/historial
     * Retorna todas las transacciones registradas.
     *
     * [MITIGACIÓN APLICADA]: Accesible para ADMIN y USER,
     * pero cada acceso queda registrado en el log de auditoría.
     */
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Transaction>> getHistorial(Authentication auth) {
        List<Transaction> historial = financialService.obtenerHistorial(auth.getName());
        return ResponseEntity.ok(historial);
    }

    /**
     * GET /api/financial/balance
     * Genera el balance de ingresos vs egresos.
     */
    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(Authentication auth) {
        Map<String, BigDecimal> balance = financialService.generarBalance(auth.getName());
        return ResponseEntity.ok(balance);
    }

    /**
     * GET /api/financial/historial/{tipo}
     * Filtra transacciones por tipo: INGRESO o EGRESO.
     */
    @GetMapping("/historial/{tipo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Transaction>> getByTipo(
            @PathVariable String tipo, Authentication auth) {

        if (!tipo.equalsIgnoreCase("INGRESO") && !tipo.equalsIgnoreCase("EGRESO")) {
            return ResponseEntity.badRequest().build();
        }
        List<Transaction> transacciones = financialService.obtenerPorTipo(tipo, auth.getName());
        return ResponseEntity.ok(transacciones);
    }

    /**
     * POST /api/financial/transaccion
     * Registra una nueva transacción financiera.
     *
     * [MITIGACIÓN APLICADA]: Solo ADMIN puede crear transacciones.
     * El servicio registra la operación en el log inmutable.
     */
    @PostMapping("/transaccion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registrarTransaccion(
            @Valid @RequestBody Transaction transaction,
            Authentication auth) {

        if (transaction.getMonto() == null || transaction.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El monto debe ser mayor a cero."));
        }

        Transaction saved = financialService.registrarTransaccion(transaction, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
