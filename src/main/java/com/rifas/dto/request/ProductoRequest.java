package com.rifas.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * ProductoRequest — Body para POST /productos y PUT /productos/{id}
 */
@Getter
@Setter
public class ProductoRequest {
    
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;

    private String descripcion;

    @Size(max = 500, message = "la URl de la imagen no puede exceder los 500 caracteres")
    private String imagenUrl;

    @NotNull(message = "El valor estimado es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El valor estimado no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El valor estimado debe tener máximo 10 enteros y 2 decimales")
    private BigDecimal valorEstimado;


}
