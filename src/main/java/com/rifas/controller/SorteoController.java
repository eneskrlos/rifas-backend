package com.rifas.controller;

import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.SorteoResponse;
import com.rifas.service.SorteoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * SorteoController — Endpoints para ejecutar y consultar sorteos.
 *
 * Permisos:
 *  POST /sorteos/{rifaId}  → solo MANAGER o ADMIN pueden ejecutar
 *  GET  /sorteos/{rifaId}  → cualquier usuario autenticado puede ver el resultado
 */
@RestController
@RequestMapping("/sorteos")
@RequiredArgsConstructor
@Tag(name = "Sorteos", description = "Ejecución y consulta de resultados de sorteos")
@SecurityRequirement(name = "bearerAuth")
public class SorteoController {

    private final SorteoService sorteoService;

    // ── POST /sorteos/{rifaId} ────────────────────────────────────
    @PostMapping("/{rifaId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
        summary     = "Ejecutar sorteo",
        description = "Ejecuta el sorteo de una rifa CERRADA usando SecureRandom. " +
                      "Solo puede ejecutarse UNA vez por rifa. " +
                      "Guarda la semilla para auditoría."
    )
    public ResponseEntity<ApiResponse<SorteoResponse>> ejecutar(
            @PathVariable Integer rifaId) {

        SorteoResponse response = sorteoService.ejecutar(rifaId);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "¡Sorteo ejecutado! El ganador es: " +
                        response.getUsuarioGanadorNombre() +
                        " con el número " + response.getNumeroGanador(),
                        response));
    }

    // ── GET /sorteos/{rifaId} ─────────────────────────────────────
    @GetMapping("/{rifaId}")
    @Operation(
        summary     = "Obtener resultado del sorteo",
        description = "Devuelve el resultado completo incluyendo ganador, " +
                      "número ganador y datos de auditoría (semilla y algoritmo)."
    )
    public ResponseEntity<ApiResponse<SorteoResponse>> obtenerResultado(
            @PathVariable Integer rifaId) {

        return ResponseEntity.ok(
                ApiResponse.ok("Resultado del sorteo",
                        sorteoService.obtenerResultado(rifaId)));
    }
}
