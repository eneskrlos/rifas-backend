package com.rifas.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ParticipacionResponse — Lo que devuelve la API al registrar
 * o consultar una participación.
 *
 * Incluye el número elegido y los datos básicos de la rifa
 * para que el frontend pueda mostrar la confirmación completa
 * sin necesidad de hacer otra consulta.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacionResponse {

    private Integer       id;
    private Integer       rifaId;
    private String        rifaNombre;
    private Integer       numero;
    private Integer       usuarioId;
    private String        usuarioNombre;
    private LocalDateTime asignadoEn;
}
