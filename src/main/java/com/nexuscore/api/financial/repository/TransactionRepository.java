package com.nexuscore.api.financial.repository;

import com.nexuscore.api.financial.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio JPA para transacciones financieras.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Lista transacciones por tipo (INGRESO o EGRESO).
     */
    List<Transaction> findByTipo(String tipo);

    /**
     * Lista transacciones creadas por un usuario específico.
     * Útil para auditorías de acceso.
     */
    List<Transaction> findByCreadoPor(String creadoPor);

    /**
     * Calcula el total de ingresos registrados.
     */
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM Transaction t WHERE t.tipo = 'INGRESO'")
    BigDecimal sumTotalIngresos();

    /**
     * Calcula el total de egresos registrados.
     */
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM Transaction t WHERE t.tipo = 'EGRESO'")
    BigDecimal sumTotalEgresos();
}
