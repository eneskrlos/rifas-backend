package com.rifas.controller;

import com.rifas.dto.request.ComboRequest;
import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.ComboResponse;
import com.rifas.service.ComboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@Tag(name = "Combos", description = "Gestión de combos de productos para rifas")
@SecurityRequirement(name = "bearerAuth")
public class ComboController {

    private final ComboService comboService;

    // ── POST /combos ──────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Crear combo con productos")
    public ResponseEntity<ApiResponse<ComboResponse>> crear(
            @Valid @RequestBody ComboRequest request) {
        ComboResponse response = comboService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Combo creado exitosamente", response));
    }

    // ── GET /combos/{id} ──────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Obtener combo por ID con sus productos")
    public ResponseEntity<ApiResponse<ComboResponse>> obtenerPorId(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Combo encontrado", comboService.obtenerPorId(id)));
    }

    // ── GET /combos ───────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "Listar combos activos paginados")
    public ResponseEntity<ApiResponse<Page<ComboResponse>>> listar(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok("Combos obtenidos", comboService.listarActivos(pageable)));
    }

    // ── GET /combos/disponibles ───────────────────────────────────
    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Listar combos disponibles para asignar a una rifa")
    public ResponseEntity<ApiResponse<List<ComboResponse>>> disponibles() {
        return ResponseEntity.ok(
                ApiResponse.ok("Combos disponibles",
                        comboService.listarDisponiblesParaRifa()));
    }

    // ── PUT /combos/{id} ──────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Actualizar combo (reemplaza todos sus productos)")
    public ResponseEntity<ApiResponse<ComboResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Combo actualizado", comboService.actualizar(id, request)));
    }

    // ── DELETE /combos/{id} ───────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Desactivar combo (soft delete)")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        comboService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Combo desactivado"));
    }
}
