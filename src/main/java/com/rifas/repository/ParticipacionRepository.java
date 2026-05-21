package com.rifas.repository;

import com.rifas.entity.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ParticipacionRepository — Acceso a la tabla 'participaciones'.
 */
@Repository
public interface ParticipacionRepository extends JpaRepository<Participacion, Integer> {

    // Todas las participaciones de un usuario en una rifa
    List<Participacion> findByUsuarioIdAndRifaId(Integer usuarioId, Integer rifaId);

    // Cuántos números tiene un usuario en una rifa
    long countByUsuarioIdAndRifaId(Integer usuarioId, Integer rifaId);

    // Todas las participaciones de una rifa (para el admin)
    List<Participacion> findByRifaId(Integer rifaId);

    // Todas las participaciones de un usuario (historial)
    List<Participacion> findByUsuarioIdOrderByAsignadoEnDesc(Integer usuarioId);

    // Verificar si un número ya fue tomado
    boolean existsByNumeroRifaId(Integer numeroRifaId);

    /**
     * Busca la participación de un número específico en una rifa.
     * Usado en el sorteo para saber quién ganó.
     */
    @Query("""
        SELECT p FROM Participacion p
        JOIN FETCH p.usuario
        JOIN FETCH p.numeroRifa
        WHERE p.rifa.id    = :rifaId
        AND p.numeroRifa.numero = :numero
    """)
    java.util.Optional<Participacion> findByRifaIdAndNumero(
            @Param("rifaId") Integer rifaId,
            @Param("numero") Integer numero);
}
