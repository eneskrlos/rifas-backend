package com.rifas.service;

import com.rifas.dto.request.ElegirNumeroRequest;
import com.rifas.dto.response.ParticipacionResponse;

import java.util.List;

public interface ParticipacionService {

    // Participante elige un número
    ParticipacionResponse elegirNumero(ElegirNumeroRequest request);

    // Historial del usuario autenticado
    List<ParticipacionResponse> miHistorial();

    // Todas las participaciones de una rifa (admin/manager)
    List<ParticipacionResponse> listarPorRifa(Integer rifaId);
}
