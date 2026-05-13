package com.rifas.repository;

import com.rifas.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RolRepository — Acceso a la tabla 'roles'.
 *
 * Usado principalmente en el registro de nuevos usuarios
 * para asignarles el rol 'participante' por defecto.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}
