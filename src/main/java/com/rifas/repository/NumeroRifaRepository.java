package com.rifas.repository;

import com.rifas.entity.NumeroRifa;
import com.rifas.entity.enums.EstadoNumero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NumeroRifaRepository — Acceso a la tabla 'numeros_rifa'.
 */
@Repository
public interface NumeroRifaRepository extends JpaRepository<NumeroRifa, Integer> {

    List<NumeroRifa> findByRifaIdOrderByNumero(Integer rifaId);

    List<NumeroRifa> findByRifaIdAndEstadoOrderByNumero(
            Integer rifaId, EstadoNumero estado);

    Optional<NumeroRifa> findByRifaIdAndNumero(Integer rifaId, Integer numero);

    long countByRifaIdAndEstado(Integer rifaId, EstadoNumero estado);

    boolean existsByRifaIdAndNumero(Integer rifaId, Integer numero);

    /**
     * Obtiene todos los números disponibles de una rifa
     * bloqueando los registros para evitar condición de carrera.
     *
     * ¿Qué es una condición de carrera aquí?
     * Si dos personas eligen el mismo número al mismo tiempo,
     * sin FOR UPDATE ambas podrían ver el número como DISPONIBLE
     * y ambas intentarían reservarlo. Con FOR UPDATE, la segunda
     * transacción espera a que la primera termine antes de leer.
     */
    @Query(value = """
        SELECT * FROM numeros_rifa
        WHERE rifa_id = :rifaId
        AND estado = 'DISPONIBLE'
        ORDER BY numero
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<NumeroRifa> findDisponiblesConLock(@Param("rifaId") Integer rifaId);
}
