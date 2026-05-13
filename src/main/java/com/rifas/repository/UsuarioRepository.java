package com.rifas.repository;

import com.rifas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UsuarioRepository — Acceso a la tabla 'usuarios'.
 *
 * Spring Data JPA genera automáticamente el SQL de cada método
 * basándose en el nombre del método. No necesitamos escribir SQL.
 *
 * findByEmail(email)    → SELECT * FROM usuarios WHERE email = ?
 * findByTelefono(tel)   → SELECT * FROM usuarios WHERE telefono = ?
 * existsByEmail(email)  → SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTelefono(String telefono);

    Optional<Usuario> findByEmailOrTelefono(String email, String telefono);

    boolean existsByEmail(String email);

    boolean existsByTelefono(String telefono);
}
