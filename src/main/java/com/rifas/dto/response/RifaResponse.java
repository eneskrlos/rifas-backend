package com.rifas.dto.response;

import com.rifas.entity.enums.EstadoRifa;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RifaResponse — Lo que devuelve la API al consultar una rifa.
 *
 * Incluye contadores de números disponibles y reservados
 * para que el frontend pueda mostrar el progreso de la rifa
 * sin necesidad de cargar toda la lista de números.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RifaResponse {

    private Integer       id;
    private String        nombre;
    private String        descripcion;
    private EstadoRifa    estado;
    private Integer       totalNumeros;
    private Integer       maxPorPersona;
    private long          numerosDisponibles;
    private long          numerosReservados;
    private LocalDateTime inicioEn;
    private LocalDateTime sorteoEn;
    private LocalDateTime creadoEn;

    // Datos del combo asociado (resumen, no el objeto completo)
    private Integer       comboId;
    private String        comboNombre;

    // Datos del creador
    private Integer       creadoPorId;
    private String        creadoPorNombre;
}
