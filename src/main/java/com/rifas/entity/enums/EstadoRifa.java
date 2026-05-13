package com.rifas.entity.enums;

/**
 * Ciclo de vida de una Rifa.
 *
 * borrador  → La rifa fue creada pero no está visible al público.
 * activa    → Visible. Los participantes pueden elegir números.
 *             Al activar, el trigger de BD genera los números automáticamente.
 * cerrada   → Ya no se aceptan nuevas participaciones. Previo al sorteo.
 * sorteada  → El sorteo fue ejecutado. Hay un ganador registrado.
 * cancelada → La rifa fue cancelada. Los números quedan sin efecto.
 */
public enum EstadoRifa {
    BORRADOR,
    ACTIVA,
    CERRADA,
    SORTEADA,
    CANCELADA
}
