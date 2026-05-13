package com.rifas.exception;

import com.rifas.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — Captura TODAS las excepciones de la API.
 *
 * ¿Por qué centralizarlo aquí?
 * Sin este handler, Spring devolvería stack traces en JSON o HTML
 * cuando algo falla. Con este handler, SIEMPRE devolvemos
 * nuestro ApiResponse limpio con un mensaje legible.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Intercepta excepciones de todos los controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Errores de validación (@Valid en el controller).
     * Ej: campo @NotBlank vacío, @Email con formato inválido.
     * → 400 Bad Request con detalle de cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo   = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });

        log.warn("Error de validación: {}", errores);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validación"));
    }

    /**
     * Credenciales incorrectas en el login.
     * Spring Security lanza BadCredentialsException cuando
     * el usuario no existe o la contraseña no coincide.
     * → 401 Unauthorized
     *
     * IMPORTANTE: el mensaje es genérico a propósito.
     * No decimos "usuario no existe" ni "contraseña incorrecta"
     * para no dar pistas a atacantes.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Intento de login con credenciales inválidas");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales incorrectas"));
    }

    /**
     * Usuario desactivado intenta loguearse.
     * → 401 Unauthorized
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledUser(
            DisabledException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Usuario desactivado. Contacte al administrador"));
    }

    /**
     * Errores de negocio generales (RuntimeException).
     * Ej: email ya registrado, rifa no encontrada, etc.
     * → 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex) {

        log.error("Error de negocio: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Cualquier error inesperado no capturado.
     * → 500 Internal Server Error
     * El mensaje al cliente es genérico para no exponer internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex) {

        log.error("Error inesperado: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor"));
    }
}
