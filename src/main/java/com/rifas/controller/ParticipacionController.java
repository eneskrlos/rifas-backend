package com.rifas.controller;

import com.rifas.dto.request.ElegirNumeroRequest;
import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.ParticipacionResponse;
import com.rifas.service.ParticipacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ParticipacionController — Endpoints para elegir números y consultar participaciones.
 *
 * Permisos:
 *  POST /participaciones          → cualquier usuario autenticado (participante)
 *  GET  /participaciones/mis      → cualquier usuario autenticado (su propio historial)
 *  GET  /participaciones/rifa/{id}→ solo MANAGER o ADMIN
 */
@RestController
@RequestMapping("/participaciones")
@RequiredArgsConstructor
@Tag(name = "Participaciones", description = "Elección de números y consulta de participaciones")
@SecurityRequirement(name = "bearerAuth")
public class ParticipacionController {

    private final ParticipacionService participacionService;

    // ── POST /participaciones ─────────────────────────────────────
    @PostMapping
    @Operation(
        summary     = "Elegir un número de la rifa",
        description = "El usuario autenticado elige un número disponible. " +
                      "Valida que la rifa esté activa, el número disponible " +
                      "y que no se supere el límite por persona."
    )
    public ResponseEntity<ApiResponse<ParticipacionResponse>> elegirNumero(
            @Valid @RequestBody ElegirNumeroRequest request) {

        ParticipacionResponse response = participacionService.elegirNumero(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "¡Número " + response.getNumero() + " reservado exitosamente!",
                        response));
    }

    // ── GET /participaciones/mis ──────────────────────────────────
    @GetMapping("/mis")
    @Operation(
        summary     = "Ver mis participaciones",
        description = "Historial de todos los números elegidos por el usuario autenticado."
    )
    public ResponseEntity<ApiResponse<List<ParticipacionResponse>>> miHistorial() {
        return ResponseEntity.ok(
                ApiResponse.ok("Tus participaciones",
                        participacionService.miHistorial()));
    }

    // ── GET /participaciones/rifa/{rifaId} ────────────────────────
    @GetMapping("/rifa/{rifaId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
        summary     = "Ver participaciones de una rifa",
        description = "Lista todos los números reservados de una rifa con su participante."
    )
    public ResponseEntity<ApiResponse<List<ParticipacionResponse>>> listarPorRifa(
            @PathVariable Integer rifaId) {

        return ResponseEntity.ok(
                ApiResponse.ok("Participaciones de la rifa",
                        participacionService.listarPorRifa(rifaId)));
    }
}
