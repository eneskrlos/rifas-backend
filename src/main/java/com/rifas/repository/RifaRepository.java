package com.rifas.repository;

import com.rifas.entity.Rifa;
import com.rifas.entity.enums.EstadoRifa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RifaRepository — Acceso a la tabla 'rifas'.
 */
@Repository
public interface RifaRepository extends JpaRepository<Rifa, Integer> {

    Page<Rifa> findByEstado(EstadoRifa estado, Pageable pageable);

    List<Rifa> findByEstado(EstadoRifa estado);

    /**
     * Carga la rifa con su combo y productos en una sola consulta.
     * Evita el problema N+1 al mostrar el detalle de una rifa.
     */
    @Query("""
        SELECT DISTINCT r FROM Rifa r
        LEFT JOIN FETCH r.combo c
        LEFT JOIN FETCH c.comboProductos cp
        LEFT JOIN FETCH cp.producto
        WHERE r.id = :id
    """)
    Optional<Rifa> findByIdWithCombo(@Param("id") Integer id);

    /**
     * Carga la rifa con sus números.
     * Usado para mostrar la grilla de números al participante.
     */
    @Query("""
        SELECT r FROM Rifa r
        LEFT JOIN FETCH r.numeros
        WHERE r.id = :id
    """)
    Optional<Rifa> findByIdWithNumeros(@Param("id") Integer id);

    boolean existsByNombreIgnoreCase(String nombre);
}
