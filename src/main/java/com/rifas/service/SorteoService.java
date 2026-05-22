package com.rifas.service;

import com.rifas.dto.response.SorteoResponse;

public interface SorteoService {

    /**
     * Ejecuta el sorteo de una rifa CERRADA.
     * Solo puede ejecutarse una vez por rifa.
     */
    SorteoResponse ejecutar(Integer rifaId);

    /**
     * Obtiene el resultado de un sorteo ya ejecutado.
     */
    SorteoResponse obtenerResultado(Integer rifaId);
}
