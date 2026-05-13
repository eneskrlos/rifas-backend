package com.rifas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ApiResponse<T> — Envoltorio estándar para TODAS las respuestas de la API.
 *
 * ¿Por qué usar un wrapper genérico?
 * Para que el frontend siempre reciba la misma estructura JSON,
 * sin importar si es un login, una lista de rifas o un error.
 *
 * Estructura de respuesta exitosa:
 * {
 *   "status":    true,
 *   "message":   "Login exitoso",
 *   "data":      { ...token, usuario... },
 *   "timestamp": "2025-01-15 10:30:00"
 * }
 *
 * Estructura de respuesta con error:
 * {
 *   "status":    false,
 *   "message":   "Credenciales incorrectas",
 *   "data":      null,
 *   "timestamp": "2025-01-15 10:30:00"
 * }
 *
 * @JsonInclude(NON_NULL) → campos null no aparecen en el JSON.
 * Si 'data' es null, no se incluye en la respuesta de error.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean       status;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    // ── Métodos de fábrica estáticos ─────────────────────────────

    /**
     * Respuesta exitosa con datos.
     * Uso: ApiResponse.ok("Operación exitosa", miObjeto)
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Respuesta exitosa sin datos (ej: DELETE, operaciones void).
     * Uso: ApiResponse.ok("Eliminado correctamente")
     */
    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Respuesta de error.
     * Uso: ApiResponse.error("Credenciales incorrectas")
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
