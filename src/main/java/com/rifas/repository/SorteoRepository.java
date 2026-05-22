package com.rifas.repository;

import com.rifas.entity.Sorteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SorteoRepository — Acceso a la tabla 'sorteos'.
 *
 * Una rifa solo puede tener UN sorteo (relación 1:1).
 * El UNIQUE en la BD garantiza esto a nivel de datos.
 */
@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, Integer> {

    /**
     * Busca el sorteo de una rifa con todos sus datos relacionados.
     * Usado para mostrar el resultado completo del sorteo.
     */
    @Query("""
        SELECT s FROM Sorteo s
        JOIN FETCH s.rifa
        JOIN FETCH s.numeroGanador
        JOIN FETCH s.usuarioGanador
        WHERE s.rifa.id = :rifaId
    """)
    Optional<Sorteo> findByRifaIdWithDetails(@Param("rifaId") Integer rifaId);

    boolean existsByRifaId(Integer rifaId);
}
