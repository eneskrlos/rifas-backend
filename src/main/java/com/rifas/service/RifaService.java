package com.rifas.service;

import com.rifas.dto.request.RifaRequest;
import com.rifas.dto.response.NumeroRifaResponse;
import com.rifas.dto.response.RifaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RifaService {

    RifaResponse            crear(RifaRequest request);
    RifaResponse            obtenerPorId(Integer id);
    Page<RifaResponse>      listarPorEstado(String estado, Pageable pageable);
    RifaResponse            actualizar(Integer id, RifaRequest request);

    RifaResponse            activar(Integer id);
    RifaResponse            cerrar(Integer id);
    RifaResponse            cancelar(Integer id);

    List<NumeroRifaResponse> obtenerNumeros(Integer rifaId);
    List<NumeroRifaResponse> obtenerNumerosDisponibles(Integer rifaId);
    void                     agregarNumeros(Integer rifaId, Integer cantidad);
    void                     eliminarNumero(Integer rifaId, Integer numero);
}
