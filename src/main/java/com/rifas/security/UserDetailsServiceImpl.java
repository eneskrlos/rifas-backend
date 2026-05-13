package com.rifas.security;

import com.rifas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl — El puente entre Spring Security y nuestra BD.
 *
 * ¿Por qué existe esta clase?
 * Spring Security no sabe nada de PostgreSQL ni de JPA.
 * Necesita que le digamos cómo cargar un usuario dado su username.
 * Esta clase implementa esa interfaz y le enseña a buscar en nuestra BD.
 *
 * ¿Cuándo se usa?
 * 1. Durante el login: Spring Security llama a loadUserByUsername()
 *    para obtener el usuario y verificar la contraseña.
 * 2. Durante cada request autenticada: el JwtAuthFilter llama aquí
 *    para cargar el usuario y entregárselo al contexto de seguridad.
 *
 * ¿Por qué buscamos por email O teléfono?
 * Porque el username en nuestro sistema puede ser cualquiera de los dos.
 * El JwtUtil guarda en el token sea cual sea el que el usuario registró.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga el usuario por su username (email o teléfono).
     *
     * @Transactional es necesario porque Usuario tiene relación
     * EAGER con Rol. Sin transacción activa, Hibernate no puede
     * cargar la relación y lanza LazyInitializationException.
     *
     * ¿Por qué devolvemos directamente el Usuario?
     * Porque Usuario implementa UserDetails (lo hicimos en la entidad).
     * No necesitamos clase adaptadora intermedia.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Intentamos primero por email, luego por teléfono
        return usuarioRepository.findByEmail(username)
                .or(() -> usuarioRepository.findByTelefono(username))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username
                ));
    }
}
