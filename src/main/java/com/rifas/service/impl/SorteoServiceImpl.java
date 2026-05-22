package com.rifas.service.impl;

import com.rifas.dto.response.SorteoResponse;
import com.rifas.entity.NumeroRifa;
import com.rifas.entity.Participacion;
import com.rifas.entity.Rifa;
import com.rifas.entity.Sorteo;
import com.rifas.entity.enums.EstadoNumero;
import com.rifas.entity.enums.EstadoRifa;
import com.rifas.repository.NumeroRifaRepository;
import com.rifas.repository.ParticipacionRepository;
import com.rifas.repository.RifaRepository;
import com.rifas.repository.SorteoRepository;
import com.rifas.service.SorteoService;
import com.rifas.util.SorteoAlgoritmo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SorteoServiceImpl implements SorteoService {

    private final RifaRepository          rifaRepository;
    private final NumeroRifaRepository     numeroRifaRepository;
    private final ParticipacionRepository  participacionRepository;
    private final SorteoRepository         sorteoRepository;
    private final SorteoAlgoritmo          sorteoAlgoritmo;

    // ─────────────────────────────────────────────────────────────
    //  EJECUTAR SORTEO
    // ─────────────────────────────────────────────────────────────

    /**
     * Flujo completo del sorteo:
     *
     *  1. Verificar que la rifa está CERRADA
     *  2. Verificar que no se haya sorteado antes
     *  3. Obtener números RESERVADOS (tienen participante)
     *  4. Verificar que hay al menos un participante
     *  5. Ejecutar el algoritmo aleatorio
     *  6. Buscar quién tiene el número ganador
     *  7. Guardar el resultado en tabla sorteos
     *  8. Cambiar estado de la rifa a SORTEADA
     *
     * @Transactional garantiza que si algo falla en el paso 7 u 8,
     * el sorteo no queda registrado a medias.
     */
    @Override
    @Transactional
    public SorteoResponse ejecutar(Integer rifaId) {

        // ── 1. Verificar estado de la rifa ────────────────────────
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new RuntimeException(
                        "Rifa no encontrada con id: " + rifaId));

        if (!EstadoRifa.CERRADA.equals(rifa.getEstado())) {
            throw new RuntimeException(
                "La rifa debe estar en estado CERRADA para ejecutar el sorteo. " +
                "Estado actual: " + rifa.getEstado());
        }

        // ── 2. Verificar que no se sorteó antes ───────────────────
        if (sorteoRepository.existsByRifaId(rifaId)) {
            throw new RuntimeException(
                "Esta rifa ya fue sorteada. " +
                "Consulta el resultado con GET /sorteos/" + rifaId);
        }

        // ── 3. Obtener números RESERVADOS ─────────────────────────
        List<NumeroRifa> numerosReservados = numeroRifaRepository
                .findByRifaIdAndEstadoOrderByNumero(
                        rifaId, EstadoNumero.RESERVADO);

        // ── 4. Verificar que hay participantes ────────────────────
        if (numerosReservados.isEmpty()) {
            throw new RuntimeException(
                "No hay números reservados en esta rifa. " +
                "No se puede ejecutar el sorteo sin participantes.");
        }

        log.info("Iniciando sorteo para rifa '{}' con {} números reservados",
                rifa.getNombre(), numerosReservados.size());

        // ── 5. Ejecutar el algoritmo ──────────────────────────────
        SorteoAlgoritmo.ResultadoSorteo resultado =
                sorteoAlgoritmo.ejecutar(numerosReservados);

        // ── 6. Buscar el participante ganador ─────────────────────
        // El número ganador está reservado → tiene exactamente una participación
        Participacion participacionGanadora = participacionRepository
                .findByRifaIdAndNumero(rifaId, resultado.numeroGanador().getNumero())
                .orElseThrow(() -> new RuntimeException(
                        "Error interno: no se encontró el participante del número ganador"));

        // ── 7. Guardar el resultado ───────────────────────────────
        Sorteo sorteo = Sorteo.builder()
                .rifa(rifa)
                .numeroGanador(resultado.numeroGanador())
                .usuarioGanador(participacionGanadora.getUsuario())
                .algoritmo(resultado.algoritmo())
                .semilla(resultado.semilla())
                .build();

        Sorteo sorteoGuardado = sorteoRepository.save(sorteo);

        // ── 8. Cambiar estado de la rifa a SORTEADA ───────────────
        rifa.marcarSorteada();
        rifaRepository.save(rifa);

        log.info("Sorteo completado: rifa='{}', número ganador={}, ganador='{}'",
                rifa.getNombre(),
                resultado.numeroGanador().getNumero(),
                participacionGanadora.getUsuario().getNombreCompleto());

        return mapToResponse(sorteoGuardado, participacionGanadora);
    }

    // ─────────────────────────────────────────────────────────────
    //  OBTENER RESULTADO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SorteoResponse obtenerResultado(Integer rifaId) {

        Sorteo sorteo = sorteoRepository.findByRifaIdWithDetails(rifaId)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró sorteo para la rifa con id: " + rifaId +
                        ". ¿Ya fue ejecutado?"));

        Participacion participacion = participacionRepository
                .findByRifaIdAndNumero(rifaId, sorteo.getNumeroGanador().getNumero())
                .orElseThrow(() -> new RuntimeException(
                        "Error interno: no se encontró la participación ganadora"));

        return mapToResponse(sorteo, participacion);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTODO PRIVADO DE APOYO
    // ─────────────────────────────────────────────────────────────

    private SorteoResponse mapToResponse(Sorteo sorteo, Participacion participacion) {
        return SorteoResponse.builder()
                .id(sorteo.getId())
                .rifaId(sorteo.getRifa().getId())
                .rifaNombre(sorteo.getRifa().getNombre())
                .numeroGanador(sorteo.getNumeroGanador().getNumero())
                .usuarioGanadorId(participacion.getUsuario().getId())
                .usuarioGanadorNombre(participacion.getUsuario().getNombreCompleto())
                .usuarioGanadorEmail(participacion.getUsuario().getEmail())
                .usuarioGanadorTelefono(participacion.getUsuario().getTelefono())
                .algoritmo(sorteo.getAlgoritmo())
                .semilla(sorteo.getSemilla())
                .ejecutadoEn(sorteo.getEjecutadoEn())
                .build();
    }
}
