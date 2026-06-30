package com.nexuscore.api.salary.repository;

import com.nexuscore.api.salary.entity.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repositorio JPA para registros de nómina.
 */
@Repository
public interface SalaryRepository extends JpaRepository<SalaryRecord, Long> {

    Optional<SalaryRecord> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    /**
     * Calcula la nómina total de la empresa (suma de todos los salarios base).
     */
    @Query("SELECT COALESCE(SUM(s.salarioBase), 0) FROM SalaryRecord s")
    BigDecimal sumNominaTotal();
}
