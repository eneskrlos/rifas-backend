package com.rifas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RifaRequest — Body para POST /rifas y PUT /rifas/{id}
 *
 * Una rifa se crea en estado BORRADOR.
 * El manager la activa con un endpoint separado PUT /rifas/{id}/activar
 * que es cuando el trigger de BD genera los números.
 */
@Getter
@Setter
public class RifaRequest {

    @NotBlank(message = "El nombre de la rifa es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El combo es obligatorio")
    private Integer comboId;

    @NotNull(message = "El total de números es obligatorio")
    @Min(value = 1,   message = "El mínimo de números es 1")
    @Max(value = 10000, message = "El máximo de números es 10000")
    private Integer totalNumeros;

    /**
     * Límite de números por persona.
     * NULL = sin límite (el participante puede elegir todos los que quiera).
     * Si se especifica debe ser mínimo 1.
     */
    @Min(value = 1, message = "El máximo por persona debe ser al menos 1")
    private Integer maxPorPersona;

    private LocalDateTime inicioEn;
    private LocalDateTime sorteoEn;
}
