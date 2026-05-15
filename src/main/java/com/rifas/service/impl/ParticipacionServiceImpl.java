package com.rifas.service.impl;

import com.rifas.dto.request.ElegirNumeroRequest;
import com.rifas.dto.response.ParticipacionResponse;
import com.rifas.entity.NumeroRifa;
import com.rifas.entity.Participacion;
import com.rifas.entity.Rifa;
import com.rifas.entity.Usuario;
import com.rifas.entity.enums.EstadoNumero;
import com.rifas.entity.enums.EstadoRifa;
import com.rifas.repository.NumeroRifaRepository;
import com.rifas.repository.ParticipacionRepository;
import com.rifas.repository.RifaRepository;
import com.rifas.repository.UsuarioRepository;
import com.rifas.service.ParticipacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipacionServiceImpl implements ParticipacionService {

    private final ParticipacionRepository  participacionRepository;
    private final NumeroRifaRepository     numeroRifaRepository;
    private final RifaRepository           rifaRepository;
    private final UsuarioRepository        usuarioRepository;

    // ─────────────────────────────────────────────────────────────
    //  ELEGIR NÚMERO — El corazón del sistema
    // ─────────────────────────────────────────────────────────────

    /**
     * Flujo de elección de número:
     *
     *  1. Verificar que la rifa esté ACTIVA
     *  2. Buscar el número con FOR UPDATE (lock pesimista)
     *     para evitar condición de carrera
     *  3. Verificar que el número esté DISPONIBLE
     *  4. Verificar que el usuario no supere maxPorPersona
     *  5. Crear la participación
     *  6. El trigger de BD cambia el número a RESERVADO
     *
     * @Transactional es CRÍTICO aquí. Todo ocurre en una sola
     * transacción. Si algo falla en el paso 5, el número
     * vuelve a DISPONIBLE automáticamente (rollback).
     */
    @Override
    @Transactional
    public ParticipacionResponse elegirNumero(ElegirNumeroRequest request) {

        // ── 1. Verificar que la rifa existe y está ACTIVA ─────────
        Rifa rifa = rifaRepository.findById(request.getRifaId())
                .orElseThrow(() -> new RuntimeException(
                        "Rifa no encontrada con id: " + request.getRifaId()));

        if (!EstadoRifa.ACTIVA.equals(rifa.getEstado())) {
            throw new RuntimeException(
                "La rifa '" + rifa.getNombre() + "' no está activa. " +
                "Estado actual: " + rifa.getEstado());
        }

        // ── 2. Buscar el número con lock pesimista ────────────────
        // findDisponiblesConLock usa FOR UPDATE SKIP LOCKED.
        // Si dos usuarios piden el mismo número simultáneamente,
        // el segundo esperará hasta que la primera transacción termine.
        NumeroRifa numeroRifa = numeroRifaRepository
                .findByRifaIdAndNumero(request.getRifaId(), request.getNumero())
                .orElseThrow(() -> new RuntimeException(
                        "El número " + request.getNumero() +
                        " no existe en esta rifa"));

        // ── 3. Verificar que el número esté DISPONIBLE ────────────
        if (!EstadoNumero.DISPONIBLE.equals(numeroRifa.getEstado())) {
            throw new RuntimeException(
                "El número " + request.getNumero() +
                " ya fue reservado por otro participante");
        }

        // ── 4. Obtener usuario y verificar límite ─────────────────
        Usuario usuario = obtenerUsuarioActual();

        if (rifa.getMaxPorPersona() != null) {
            long cantidadActual = participacionRepository
                    .countByUsuarioIdAndRifaId(usuario.getId(), rifa.getId());

            if (cantidadActual >= rifa.getMaxPorPersona()) {
                throw new RuntimeException(
                    "Ya alcanzaste el límite de " + rifa.getMaxPorPersona() +
                    " número(s) por persona en esta rifa");
            }
        }

        // ── 5. Crear la participación ─────────────────────────────
        Participacion participacion = Participacion.builder()
                .usuario(usuario)
                .rifa(rifa)
                .numeroRifa(numeroRifa)
                .build();

        Participacion guardada = participacionRepository.save(participacion);

        // ── 6. El trigger trg_reservar_numero_al_participar ───────
        // cambia automáticamente el estado del número a RESERVADO
        // en la BD. No necesitamos hacerlo desde Java.

        log.info("Participación registrada: usuario={}, rifa='{}', número={}",
                usuario.getUsername(), rifa.getNombre(), request.getNumero());

        return mapToResponse(guardada);
    }

    // ─────────────────────────────────────────────────────────────
    //  HISTORIAL DEL USUARIO AUTENTICADO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ParticipacionResponse> miHistorial() {
        Usuario usuario = obtenerUsuarioActual();
        return participacionRepository
                .findByUsuarioIdOrderByAsignadoEnDesc(usuario.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    //  LISTAR PARTICIPACIONES DE UNA RIFA (admin/manager)
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ParticipacionResponse> listarPorRifa(Integer rifaId) {
        rifaRepository.findById(rifaId)
                .orElseThrow(() -> new RuntimeException(
                        "Rifa no encontrada con id: " + rifaId));

        return participacionRepository.findByRifaId(rifaId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTODOS PRIVADOS DE APOYO
    // ─────────────────────────────────────────────────────────────

    private Usuario obtenerUsuarioActual() {
        String username = SecurityContextHolder
                .getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailOrTelefono(username, username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private ParticipacionResponse mapToResponse(Participacion p) {
        return ParticipacionResponse.builder()
                .id(p.getId())
                .rifaId(p.getRifa().getId())
                .rifaNombre(p.getRifa().getNombre())
                .numero(p.getNumeroRifa().getNumero())
                .usuarioId(p.getUsuario().getId())
                .usuarioNombre(p.getUsuario().getNombreCompleto())
                .asignadoEn(p.getAsignadoEn())
                .build();
    }
}
