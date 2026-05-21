package com.rifas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * ElegirNumeroRequest — Body para POST /participaciones
 *
 * El usuario envía qué número quiere de qué rifa.
 * El servicio valida que:
 *  1. La rifa esté ACTIVA
 *  2. El número esté DISPONIBLE
 *  3. El usuario no supere el límite (maxPorPersona)
 */
@Getter
@Setter
public class ElegirNumeroRequest {

    @NotNull(message = "El ID de la rifa es obligatorio")
    private Integer rifaId;

    @NotNull(message = "El número elegido es obligatorio")
    @Min(value = 1, message = "El número debe ser mayor a 0")
    private Integer numero;
}
