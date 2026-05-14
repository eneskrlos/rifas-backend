package com.rifas.controller;

import com.rifas.dto.request.RifaRequest;
import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.NumeroRifaResponse;
import com.rifas.dto.response.RifaResponse;
import com.rifas.service.RifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/rifas")
@RequiredArgsConstructor
@Tag(name = "Rifas", description = "Gestión del ciclo de vida de rifas y sus números")
@SecurityRequirement(name = "bearerAuth")
public class RifaController {

    private final RifaService rifaService;

    // ── POST /rifas ───────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Crear rifa en estado BORRADOR")
    public ResponseEntity<ApiResponse<RifaResponse>> crear(
            @Valid @RequestBody RifaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rifa creada", rifaService.crear(request)));
    }

    // ── GET /rifas/{id} ───────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Obtener rifa por ID con detalle del combo")
    public ResponseEntity<ApiResponse<RifaResponse>> obtenerPorId(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifa encontrada", rifaService.obtenerPorId(id)));
    }

    // ── GET /rifas?estado=ACTIVA ──────────────────────────────────
    @GetMapping
    @Operation(summary = "Listar rifas por estado",
               description = "Estados: BORRADOR, ACTIVA, CERRADA, SORTEADA, CANCELADA")
    public ResponseEntity<ApiResponse<Page<RifaResponse>>> listar(
            @RequestParam(defaultValue = "ACTIVA") String estado,
            @PageableDefault(size = 10, sort = "creadoEn") Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifas obtenidas",
                        rifaService.listarPorEstado(estado, pageable)));
    }

    // ── PUT /rifas/{id} ───────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Actualizar rifa (solo en estado BORRADOR)")
    public ResponseEntity<ApiResponse<RifaResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody RifaRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifa actualizada", rifaService.actualizar(id, request)));
    }

    // ── PUT /rifas/{id}/activar ───────────────────────────────────
    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Activar rifa: BORRADOR → ACTIVA",
               description = "El trigger de BD genera automáticamente los números al activar.")
    public ResponseEntity<ApiResponse<RifaResponse>> activar(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifa activada correctamente", rifaService.activar(id)));
    }

    // ── PUT /rifas/{id}/cerrar ────────────────────────────────────
    @PutMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Cerrar rifa: ACTIVA → CERRADA",
               description = "Ya no se aceptan participaciones. Previo al sorteo.")
    public ResponseEntity<ApiResponse<RifaResponse>> cerrar(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifa cerrada", rifaService.cerrar(id)));
    }

    // ── PUT /rifas/{id}/cancelar ──────────────────────────────────
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Cancelar rifa")
    public ResponseEntity<ApiResponse<RifaResponse>> cancelar(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Rifa cancelada", rifaService.cancelar(id)));
    }

    // ── GET /rifas/{id}/numeros ───────────────────────────────────
    @GetMapping("/{id}/numeros")
    @Operation(summary = "Obtener todos los números de la rifa (disponibles y reservados)")
    public ResponseEntity<ApiResponse<List<NumeroRifaResponse>>> obtenerNumeros(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Números obtenidos", rifaService.obtenerNumeros(id)));
    }

    // ── GET /rifas/{id}/numeros/disponibles ───────────────────────
    @GetMapping("/{id}/numeros/disponibles")
    @Operation(summary = "Obtener solo los números disponibles de la rifa")
    public ResponseEntity<ApiResponse<List<NumeroRifaResponse>>> obtenerDisponibles(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Números disponibles",
                        rifaService.obtenerNumerosDisponibles(id)));
    }

    // ── POST /rifas/{id}/numeros ──────────────────────────────────
    @PostMapping("/{id}/numeros")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Agregar más números a una rifa ACTIVA")
    public ResponseEntity<ApiResponse<Void>> agregarNumeros(
            @PathVariable Integer id,
            @RequestParam @Min(1) Integer cantidad) {
        rifaService.agregarNumeros(id, cantidad);
        return ResponseEntity.ok(
                ApiResponse.ok("Se agregaron " + cantidad + " números a la rifa"));
    }

    // ── DELETE /rifas/{id}/numeros/{numero} ───────────────────────
    @DeleteMapping("/{id}/numeros/{numero}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Eliminar un número disponible de una rifa ACTIVA")
    public ResponseEntity<ApiResponse<Void>> eliminarNumero(
            @PathVariable Integer id,
            @PathVariable Integer numero) {
        rifaService.eliminarNumero(id, numero);
        return ResponseEntity.ok(
                ApiResponse.ok("Número " + numero + " eliminado de la rifa"));
    }
}
