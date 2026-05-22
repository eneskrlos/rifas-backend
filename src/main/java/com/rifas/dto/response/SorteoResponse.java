package com.rifas.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * SorteoResponse — Resultado completo del sorteo.
 *
 * Incluye todos los datos necesarios para:
 *  - Mostrar al ganador públicamente
 *  - Permitir auditoría del resultado (semilla + algoritmo)
 *  - Mostrar de qué rifa y con qué combo fue el premio
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SorteoResponse {

    private Integer       id;

    // ── Datos de la rifa ──────────────────────────────────────
    private Integer       rifaId;
    private String        rifaNombre;

    // ── Número ganador ────────────────────────────────────────
    private Integer       numeroGanador;

    // ── Ganador ───────────────────────────────────────────────
    private Integer       usuarioGanadorId;
    private String        usuarioGanadorNombre;
    private String        usuarioGanadorEmail;
    private String        usuarioGanadorTelefono;

    // ── Auditoría del algoritmo ───────────────────────────────
    private String        algoritmo;
    private String        semilla;
    private LocalDateTime ejecutadoEn;
}
