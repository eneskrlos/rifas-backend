package com.rifas.controller;

import com.rifas.dto.request.ProductoRequest;
import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.ProductoResponse;
import com.rifas.service.ProductoService;
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

/**
 * ProductoController — CRUD de productos.
 *
 * Permisos:
 *  - GET  → cualquier usuario autenticado puede ver productos
 *  - POST, PUT, DELETE → solo MANAGER o ADMIN
 *
 * @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") se evalúa
 * ANTES de ejecutar el método. Si el rol no coincide devuelve 403.
 */
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos para combos")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService productoService;

    // ── POST /productos ───────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Crear producto")
    public ResponseEntity<ApiResponse<ProductoResponse>> crear(
            @Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = productoService.crearProducto(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Producto creado exitosamente", response));
    }

    // ── GET /productos/{id} ───────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public ResponseEntity<ApiResponse<ProductoResponse>> obtenerPorId(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Producto encontrado", productoService.obtenerPorId(id)));
    }

    // ── GET /productos ────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "Listar productos activos paginados",
               description = "Parámetros: ?page=0&size=10&sort=nombre,asc")
    public ResponseEntity<ApiResponse<Page<ProductoResponse>>> listar(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok("Productos obtenidos",
                        productoService.listarActivos(pageable)));
    }

    // ── GET /productos/buscar?nombre=xxx ──────────────────────────
    @GetMapping("/buscar")
    @Operation(summary = "Buscar productos por nombre")
    public ResponseEntity<ApiResponse<Page<ProductoResponse>>> buscar(
            @RequestParam String nombre,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok("Resultados de búsqueda",
                        productoService.buscarPorNombre(nombre, pageable)));
    }

    // ── GET /productos/sin-combo ──────────────────────────────────
    @GetMapping("/sin-combo")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Listar productos no asignados a ningún combo activo")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> sinCombo() {
        return ResponseEntity.ok(
                ApiResponse.ok("Productos disponibles",
                        productoService.listarSinComboActivo()));
    }

    // ── PUT /productos/{id} ───────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Actualizar producto")
    public ResponseEntity<ApiResponse<ProductoResponse>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Producto actualizado",
                        productoService.actualizarProducto(id, request)));
    }

    // ── DELETE /productos/{id} ────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Desactivar producto (soft delete)")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.ok(ApiResponse.ok("Producto desactivado"));
    }
}
