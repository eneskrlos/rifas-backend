package com.rifas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * RegisterRequest — Lo que el cliente envía en POST /auth/register.
 *
 * Reglas de negocio validadas aquí:
 *  - nombre_completo obligatorio
 *  - email O teléfono obligatorio (al menos uno)
 *    → Esta validación cross-field se hace en el servicio,
 *      porque @NotBlank no puede hacer "o esto o aquello".
 *  - contraseña mínimo 8 caracteres
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombreCompleto;

    /**
     * Email opcional, pero si se proporciona debe tener formato válido.
     * @Email valida el formato (contiene @, dominio, etc.)
     */
    @Email(message = "El formato del email no es válido")
    private String email;

    /**
     * Teléfono opcional. Si se proporciona debe ser numérico
     * y tener entre 7 y 15 dígitos (estándar internacional E.164).
     */
    @Pattern(
        regexp = "^[0-9]{7,15}$",
        message = "El teléfono debe contener solo dígitos (7-15 caracteres)"
    )
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    private String password;
}
