package com.nexuscore.api.financial.service;

import com.nexuscore.api.financial.entity.Transaction;
import com.nexuscore.api.financial.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  MÓDULO: Financial - Servicio de Lógica Financiera y Contable
 *  Activo 4: Historial de Transacciones y Facturación
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El módulo financiero original no tenía ningún sistema de logging.
 *   Las transacciones se podían crear, modificar o eliminar sin dejar
 *   rastro alguno. Esto viola el principio de NO REPUDIO en seguridad
 *   informática y hace imposible auditar fraudes o errores contables.
 *   Adicionalmente, los errores generaban stack traces completos visibles
 *   al usuario final (information disclosure).
 *
 * [MITIGACIÓN APLICADA]:
 *   1. @Slf4j (SLF4J + Logback): Logger formal que registra cada
 *      operación financiera con timestamp, usuario, monto y referencia.
 *   2. Logs escritos en archivo físico (logs/nexuscore-audit.log)
 *      que actúa como registro INMUTABLE de operaciones.
 *   3. @Transactional garantiza atomicidad: si falla algún paso,
 *      se hace rollback completo (no quedan estados intermedios).
 *   4. Try-catch con logs de ERROR sin exponer stack trace al cliente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final TransactionRepository transactionRepository;

    /**
     * Registra una nueva transacción financiera.
     *
     * [MITIGACIÓN APLICADA]:
     *   - Log ANTES y DESPUÉS de la operación (audit trail completo).
     *   - El username del creador queda registrado de forma inmutable.
     *   - @Transactional garantiza que el registro es atómico.
     *
     * @param transaction Transacción a registrar
     * @param username    Usuario que ejecuta la operación (del JWT)
     * @return Transacción guardada con ID generado
     */
    @Transactional
    public Transaction registrarTransaccion(Transaction transaction, String username) {
        // [MITIGACIÓN APLICADA]: Log previo a la operación (pre-audit).
        log.info("[FINANCIERO] INICIO registro de transacción. Usuario: '{}' | Tipo: {} | Monto: ${}",
                username, transaction.getTipo(), transaction.getMonto());

        transaction.setCreadoPor(username);

        try {
            Transaction saved = transactionRepository.save(transaction);

            // [MITIGACIÓN APLICADA]: Log posterior con ID asignado (post-audit).
            // Este log queda en el archivo físico como registro inmutable.
            log.info("[FINANCIERO] Transacción REGISTRADA. ID: {} | Ref: {} | Tipo: {} | Monto: ${} | Por: '{}'",
                    saved.getId(),
                    saved.getReferencia(),
                    saved.getTipo(),
                    saved.getMonto(),
                    saved.getCreadoPor());

            return saved;

        } catch (Exception e) {
            // [MITIGACIÓN APLICADA]: Log de ERROR con detalles internos
            // (solo visible en logs del servidor, NO en la respuesta al cliente).
            log.error("[FINANCIERO] ERROR al registrar transacción. Usuario: '{}' | Causa: {}",
                    username, e.getMessage());
            throw new RuntimeException("No se pudo procesar la transacción. Contacte al administrador.");
        }
    }

    /**
     * Obtiene el historial completo de transacciones.
     *
     * [MITIGACIÓN APLICADA]: Log de acceso al historial financiero.
     * Cada consulta queda registrada con quién la realizó y cuándo.
     */
    public List<Transaction> obtenerHistorial(String username) {
        log.info("[FINANCIERO] Usuario '{}' consultó historial completo de transacciones.", username);
        return transactionRepository.findAll();
    }

    /**
     * Genera un balance financiero (total ingresos vs egresos).
     *
     * [MITIGACIÓN APLICADA]:
     *   - Log de generación de balance para detectar accesos no autorizados.
     *   - Uso de BigDecimal para cálculos monetarios exactos (sin errores de float).
     *
     * @param username Usuario que solicita el balance
     * @return Mapa con totales de ingresos, egresos y balance neto
     */
    public Map<String, BigDecimal> generarBalance(String username) {
        log.info("[FINANCIERO] Usuario '{}' solicitó reporte de balance financiero.", username);

        BigDecimal totalIngresos = transactionRepository.sumTotalIngresos();
        BigDecimal totalEgresos  = transactionRepository.sumTotalEgresos();
        BigDecimal balanceNeto   = totalIngresos.subtract(totalEgresos);

        log.info("[FINANCIERO] Balance generado. Ingresos: ${} | Egresos: ${} | Neto: ${}",
                totalIngresos, totalEgresos, balanceNeto);

        Map<String, BigDecimal> balance = new HashMap<>();
        balance.put("totalIngresos", totalIngresos);
        balance.put("totalEgresos", totalEgresos);
        balance.put("balanceNeto", balanceNeto);

        return balance;
    }

    /**
     * Obtiene transacciones filtradas por tipo.
     *
     * @param tipo     INGRESO o EGRESO
     * @param username Usuario que realiza la consulta
     */
    public List<Transaction> obtenerPorTipo(String tipo, String username) {
        log.info("[FINANCIERO] Usuario '{}' filtró transacciones por tipo: {}.", username, tipo);
        return transactionRepository.findByTipo(tipo.toUpperCase());
    }
}
