package com.nexuscore.api.auth.repository;

import com.nexuscore.api.auth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad AppUser (usuarios del sistema).
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     * Utilizado por Spring Security para cargar el usuario durante la autenticación.
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Verifica si existe un usuario con el nombre dado.
     * Utilizado para prevenir registros duplicados.
     */
    boolean existsByUsername(String username);
}
