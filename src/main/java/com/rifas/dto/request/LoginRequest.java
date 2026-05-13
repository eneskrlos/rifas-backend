package com.rifas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * LoginRequest — Lo que el cliente envía en el body del POST /auth/login.
 *
 * ¿Por qué 'identifier' y no 'email'?
 * Porque en nuestro sistema el usuario puede loguearse con
 * email O teléfono. Usamos un campo genérico que acepta ambos.
 * El servicio decidirá cuál es buscando primero por email,
 * luego por teléfono.
 *
 * @NotBlank valida que el campo no sea null, vacío ni solo espacios.
 * Si falla, Spring devuelve 400 Bad Request automáticamente.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "El usuario (email o teléfono) es obligatorio")
    private String identifier;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
