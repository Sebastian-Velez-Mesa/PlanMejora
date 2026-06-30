package com.nexuscore.api.customer.repository;

import com.nexuscore.api.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Customer.
 * Spring Data genera automáticamente las implementaciones CRUD.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Busca un cliente por cédula o NIT.
     * Útil para validar duplicados antes de guardar.
     */
    Optional<Customer> findByCedulaNit(String cedulaNit);

    /**
     * Busca un cliente por correo electrónico.
     */
    Optional<Customer> findByCorreo(String correo);

    /**
     * Verifica si ya existe un cliente con esa cédula/NIT.
     */
    boolean existsByCedulaNit(String cedulaNit);
}
