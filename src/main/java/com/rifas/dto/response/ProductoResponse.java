package com.rifas.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

/**
 * ProductoResponse — Lo que devuelve la API al consultar un producto.
 *
 * Nótese que NO incluimos el objeto Usuario completo de 'creadoPor',
 * solo su ID y nombre. Esto evita exponer datos sensibles del usuario
 * y reduce el tamaño del JSON.
 */
@Getter
@Setter
@Builder // Builder para facilitar la creación de objetos en pruebas y servicios
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    
    private Integer       id;
    private String        nombre;
    private String        descripcion;
    private String        imagenUrl;
    private BigDecimal    valorEstimado;
    private Boolean       activo;
    private Integer       creadoPorId;
    private String        creadoPorNombre;
    private LocalDateTime creadoEn;

}
