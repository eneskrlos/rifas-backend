package com.rifas.controller;

import com.rifas.dto.request.ChangePasswordRequest;
import com.rifas.dto.request.LoginRequest;
import com.rifas.dto.request.RegisterRequest;
import com.rifas.dto.response.ApiResponse;
import com.rifas.dto.response.AuthResponse;
import com.rifas.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — Endpoints públicos de autenticación.
 *
 * Rutas:
 *  POST /api/auth/login    → Login con email/teléfono + contraseña
 *  POST /api/auth/register → Registro de nuevo participante
 *
 * ¿Por qué estas rutas son públicas?
 * Declaradas en SecurityConfig como RUTAS_PUBLICAS (/auth/**).
 * No requieren token porque son el punto de entrada al sistema.
 *
 * ¿Por qué el controller es tan simple?
 * Porque TODA la lógica vive en AuthService.
 * El controller solo recibe, valida y delega.
 * Esto se llama "thin controller, fat service".
 *
 * @Valid → activa la validación de los DTOs (Bean Validation).
 * Si un campo @NotBlank está vacío, Spring devuelve 400 automáticamente
 * ANTES de llegar al método del controller.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y registro de usuarios")
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────
    //  POST /auth/login
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
        summary     = "Iniciar sesión",
        description = "Autentica con email o teléfono + contraseña. Devuelve JWT."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Intento de login para: {}", request.getIdentifier());

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.ok("Login exitoso", authResponse)
        );
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /auth/register
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(
        summary     = "Registrar participante",
        description = "Crea una nueva cuenta de participante. Devuelve JWT."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Solicitud de registro para: {}",
                request.getEmail() != null
                        ? request.getEmail()
                        : request.getTelefono());

        AuthResponse authResponse = authService.register(request);

        // 201 Created es el código correcto para creación de recursos
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso", authResponse));
    }

    @PostMapping("/change-password")
    @Operation(
        summary     = "Cambiar contraseña",
        description = "Permite a un usuario autenticado cambiar su contraseña actual por una nueva."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request
    ){
        log.info("Request: "+ request.getCurrentPassword());
        authService.changePassword(request);
        return ResponseEntity.ok(
            ApiResponse.ok("Contraseña cambiada exitosamente", null)
        );
    }
}
