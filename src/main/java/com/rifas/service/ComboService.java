package com.rifas.service;

import com.rifas.dto.request.ComboRequest;
import com.rifas.dto.response.ComboResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComboService {

    ComboResponse              crear(ComboRequest request);
    ComboResponse              obtenerPorId(Integer id);
    Page<ComboResponse>        listarActivos(Pageable pageable);
    ComboResponse              actualizar(Integer id, ComboRequest request);
    void                       desactivar(Integer id);
    List<ComboResponse>        listarDisponiblesParaRifa();    
}