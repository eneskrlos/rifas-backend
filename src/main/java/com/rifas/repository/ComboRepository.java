package com.rifas.repository;

import com.rifas.entity.Combo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ComboRepository — Acceso a la tabla 'combos'.
 */
@Repository
public interface ComboRepository extends JpaRepository<Combo, Integer> {

    Page<Combo> findByActivoTrue(Pageable pageable);

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Carga el combo con sus productos en una sola consulta.
     * Sin este JOIN FETCH, acceder a combo.getComboProductos()
     * dispararía N consultas adicionales (problema N+1).
     *
     * ¿Qué es el problema N+1?
     * Si tienes 10 combos y cada uno tiene productos, sin JOIN FETCH
     * Hibernate haría 1 consulta para los combos + 10 consultas para
     * los productos de cada uno = 11 consultas. Con JOIN FETCH: 1 sola.
     */
    @Query("""
        SELECT DISTINCT c FROM Combo c
        LEFT JOIN FETCH c.comboProductos cp
        LEFT JOIN FETCH cp.producto
        WHERE c.id = :id
    """)
    Optional<Combo> findByIdWithProductos(Integer id);

    /**
     * Combos activos que NO tienen una rifa asociada activa.
     * Útil para mostrar qué combos están disponibles para crear
     * una nueva rifa.
     */
    @Query("""
        SELECT c FROM Combo c
        WHERE c.activo = true
        AND c.id NOT IN (
            SELECT r.combo.id FROM Rifa r
            WHERE r.estado IN ('ACTIVA', 'CERRADA', 'SORTEADA')
            AND r.combo IS NOT NULL
        )
    """)
    List<Combo> findCombosDisponiblesParaRifa();
}
