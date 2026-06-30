package com.nexuscore.api.auth.service;

import com.nexuscore.api.auth.entity.AppUser;
import com.nexuscore.api.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ================================================================
 *  MÓDULO: Auth - Servicio de Carga de Detalles de Usuario
 *  Activo 2: Integración con Spring Security
 * ================================================================
 *
 * [MITIGACIÓN APLICADA]:
 *   Spring Security invoca este servicio para cargar el usuario
 *   durante cada proceso de autenticación.
 *   Si el usuario no existe, lanza UsernameNotFoundException
 *   con un mensaje genérico (no revela si el usuario existe o no).
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                // [MITIGACIÓN APLICADA]: Mensaje genérico para no revelar
                // si el usuario existe (previene enumeración de usuarios).
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        // Verificar que la cuenta esté activa
        if (!user.getActivo()) {
            throw new UsernameNotFoundException("Cuenta deshabilitada");
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
