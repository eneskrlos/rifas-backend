package com.rifas.entity.enums;

/**
 * Estado de un número dentro de una rifa.
 *
 * DISPONIBLE → Nadie lo ha elegido. Puede ser tomado.
 * RESERVADO  → Ya fue elegido por un participante.
 *              El trigger de BD lo cambia automáticamente
 *              al insertar una Participacion.
 */
public enum EstadoNumero {
    DISPONIBLE,
    RESERVADO
}
