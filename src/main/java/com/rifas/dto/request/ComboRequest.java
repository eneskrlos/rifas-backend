package com.rifas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ComboRequest — Body para POST /combos y PUT /combos/{id}
 *
 * Incluye la lista de productos con sus cantidades.
 * @Valid en la lista activa la validación de cada ComboItemRequest.
 */
@Getter
@Setter
public class ComboRequest {

    @NotBlank(message = "El nombre del combo es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;

    private String descripcion;

    @NotEmpty(message = "El combo debe tener al menos un producto")
    @Valid
    private List<ComboItemRequest> productos;

    // ── Clase interna para cada ítem del combo ────────────────

    /**
     * Representa un producto dentro del combo con su cantidad.
     * Clase interna porque solo tiene sentido en el contexto de ComboRequest.
     */
    @Getter
    @Setter
    public static class ComboItemRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        private Integer productoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 100, message = "La cantidad máxima es 100")
        private Integer cantidad;
    }
}
