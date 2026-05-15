package com.rifas.dto.response;

import com.rifas.entity.enums.EstadoNumero;
import lombok.*;

/**
 * NumeroRifaResponse — Representa un número en la grilla de la rifa.
 *
 * El frontend usa este DTO para pintar la grilla de números:
 * - DISPONIBLE → celda verde/blanca, clickeable
 * - RESERVADO  → celda gris/roja, no clickeable
 *
 * No incluimos quién reservó el número por privacidad.
 * Solo el admin puede ver esa información.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumeroRifaResponse {

    private Integer     id;
    private Integer     numero;
    private EstadoNumero estado;
}
