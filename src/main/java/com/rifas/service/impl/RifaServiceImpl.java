package com.rifas.service.impl;

import com.rifas.dto.request.RifaRequest;
import com.rifas.dto.response.NumeroRifaResponse;
import com.rifas.dto.response.RifaResponse;
import com.rifas.entity.*;
import com.rifas.entity.enums.EstadoNumero;
import com.rifas.entity.enums.EstadoRifa;
import com.rifas.repository.*;
import com.rifas.service.RifaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RifaServiceImpl implements RifaService {

    private final RifaRepository       rifaRepository;
    private final NumeroRifaRepository  numeroRifaRepository;
    private final ComboRepository       comboRepository;
    private final UsuarioRepository     usuarioRepository;

    @Override
    @Transactional
    public RifaResponse crear(RifaRequest request) {
        if (rifaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new RuntimeException("Ya existe una rifa con el nombre '" + request.getNombre() + "'");
        }
        Combo combo = comboRepository.findById(request.getComboId())
                .orElseThrow(() -> new RuntimeException("Combo no encontrado con id: " + request.getComboId()));
        if (!combo.getActivo()) {
            throw new RuntimeException("El combo '" + combo.getNombre() + "' está desactivado");
        }
        Usuario usuarioActual = obtenerUsuarioActual();
        Rifa rifa = Rifa.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .combo(combo)
                .totalNumeros(request.getTotalNumeros())
                .maxPorPersona(request.getMaxPorPersona())
                .estado(EstadoRifa.BORRADOR)
                .inicioEn(request.getInicioEn())
                .sorteoEn(request.getSorteoEn())
                .creadoPor(usuarioActual)
                .build();
        Rifa guardada = rifaRepository.save(rifa);
        log.info("Rifa creada: '{}' en BORRADOR", guardada.getNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public RifaResponse obtenerPorId(Integer id) {
        return mapToResponse(buscarConComboOLanzarError(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RifaResponse> listarPorEstado(String estado, Pageable pageable) {
        EstadoRifa estadoEnum = parsearEstado(estado);
        return rifaRepository.findByEstado(estadoEnum, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public RifaResponse actualizar(Integer id, RifaRequest request) {
        Rifa rifa = buscarOLanzarError(id);
        if (!EstadoRifa.BORRADOR.equals(rifa.getEstado())) {
            throw new RuntimeException("Solo se puede editar una rifa en BORRADOR. Estado actual: " + rifa.getEstado());
        }
        Combo combo = comboRepository.findById(request.getComboId())
                .orElseThrow(() -> new RuntimeException("Combo no encontrado con id: " + request.getComboId()));
        rifa.setNombre(request.getNombre());
        rifa.setDescripcion(request.getDescripcion());
        rifa.setCombo(combo);
        rifa.setTotalNumeros(request.getTotalNumeros());
        rifa.setMaxPorPersona(request.getMaxPorPersona());
        rifa.setInicioEn(request.getInicioEn());
        rifa.setSorteoEn(request.getSorteoEn());
        return mapToResponse(rifaRepository.save(rifa));
    }

    @Override
    @Transactional
    public RifaResponse activar(Integer id) {
        Rifa rifa = buscarOLanzarError(id);
        rifa.activar();
        Rifa activada = rifaRepository.save(rifa);
        log.info("Rifa activada: id={}, trigger generara {} numeros", id, activada.getTotalNumeros());
        return mapToResponse(activada);
    }

    @Override
    @Transactional
    public RifaResponse cerrar(Integer id) {
        Rifa rifa = buscarOLanzarError(id);
        rifa.cerrar();
        return mapToResponse(rifaRepository.save(rifa));
    }

    @Override
    @Transactional
    public RifaResponse cancelar(Integer id) {
        Rifa rifa = buscarOLanzarError(id);
        rifa.cancelar();
        return mapToResponse(rifaRepository.save(rifa));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NumeroRifaResponse> obtenerNumeros(Integer rifaId) {
        buscarOLanzarError(rifaId);
        return numeroRifaRepository.findByRifaIdOrderByNumero(rifaId)
                .stream().map(this::mapNumeroToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NumeroRifaResponse> obtenerNumerosDisponibles(Integer rifaId) {
        buscarOLanzarError(rifaId);
        return numeroRifaRepository.findByRifaIdAndEstadoOrderByNumero(rifaId, EstadoNumero.DISPONIBLE)
                .stream().map(this::mapNumeroToResponse).toList();
    }

    @Override
    @Transactional
    public void agregarNumeros(Integer rifaId, Integer cantidad) {
        Rifa rifa = buscarOLanzarError(rifaId);
        if (!EstadoRifa.ACTIVA.equals(rifa.getEstado())) {
            throw new RuntimeException("Solo se pueden agregar numeros a una rifa ACTIVA");
        }
        if (cantidad < 1 || cantidad > 1000) {
            throw new RuntimeException("La cantidad debe estar entre 1 y 1000");
        }
        int ultimoNumero = numeroRifaRepository.findByRifaIdOrderByNumero(rifaId)
                .stream().mapToInt(NumeroRifa::getNumero).max().orElse(0);
        for (int i = 1; i <= cantidad; i++) {
            numeroRifaRepository.save(NumeroRifa.builder()
                    .rifa(rifa).numero(ultimoNumero + i).estado(EstadoNumero.DISPONIBLE).build());
        }
        rifa.setTotalNumeros(rifa.getTotalNumeros() + cantidad);
        rifaRepository.save(rifa);
        log.info("Agregados {} numeros a rifa id={}. Nuevo total: {}", cantidad, rifaId, rifa.getTotalNumeros());
    }

    @Override
    @Transactional
    public void eliminarNumero(Integer rifaId, Integer numero) {
        Rifa rifa = buscarOLanzarError(rifaId);
        if (!EstadoRifa.ACTIVA.equals(rifa.getEstado())) {
            throw new RuntimeException("Solo se pueden eliminar numeros de una rifa ACTIVA");
        }
        NumeroRifa numeroRifa = numeroRifaRepository.findByRifaIdAndNumero(rifaId, numero)
                .orElseThrow(() -> new RuntimeException("Numero " + numero + " no encontrado en la rifa"));
        if (EstadoNumero.RESERVADO.equals(numeroRifa.getEstado())) {
            throw new RuntimeException("No se puede eliminar el numero " + numero + " porque ya fue reservado");
        }
        numeroRifaRepository.delete(numeroRifa);
        rifa.setTotalNumeros(rifa.getTotalNumeros() - 1);
        rifaRepository.save(rifa);
        log.info("Numero {} eliminado de rifa id={}", numero, rifaId);
    }

    private Rifa buscarOLanzarError(Integer id) {
        return rifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada con id: " + id));
    }

    private Rifa buscarConComboOLanzarError(Integer id) {
        return rifaRepository.findByIdWithCombo(id)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada con id: " + id));
    }

    private Usuario obtenerUsuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailOrTelefono(username, username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private EstadoRifa parsearEstado(String estado) {
        try {
            return EstadoRifa.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado invalido: '" + estado + "'. Valores: BORRADOR, ACTIVA, CERRADA, SORTEADA, CANCELADA");
        }
    }

    private RifaResponse mapToResponse(Rifa rifa) {
        long disponibles = numeroRifaRepository.countByRifaIdAndEstado(rifa.getId(), EstadoNumero.DISPONIBLE);
        long reservados  = numeroRifaRepository.countByRifaIdAndEstado(rifa.getId(), EstadoNumero.RESERVADO);
        return RifaResponse.builder()
                .id(rifa.getId())
                .nombre(rifa.getNombre())
                .descripcion(rifa.getDescripcion())
                .estado(rifa.getEstado())
                .totalNumeros(rifa.getTotalNumeros())
                .maxPorPersona(rifa.getMaxPorPersona())
                .numerosDisponibles(disponibles)
                .numerosReservados(reservados)
                .inicioEn(rifa.getInicioEn())
                .sorteoEn(rifa.getSorteoEn())
                .creadoEn(rifa.getCreadoEn())
                .comboId(rifa.getCombo() != null ? rifa.getCombo().getId() : null)
                .comboNombre(rifa.getCombo() != null ? rifa.getCombo().getNombre() : null)
                .creadoPorId(rifa.getCreadoPor() != null ? rifa.getCreadoPor().getId() : null)
                .creadoPorNombre(rifa.getCreadoPor() != null ? rifa.getCreadoPor().getNombreCompleto() : null)
                .build();
    }

    private NumeroRifaResponse mapNumeroToResponse(NumeroRifa n) {
        return NumeroRifaResponse.builder().id(n.getId()).numero(n.getNumero()).estado(n.getEstado()).build();
    }
}
