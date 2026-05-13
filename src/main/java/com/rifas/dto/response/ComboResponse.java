package com.rifas.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ComboResponse — Lo que devuelve la API al consultar un combo.
 *
 * Incluye la lista de productos con sus cantidades y el
 * valor total calculado (suma de valorEstimado * cantidad).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboResponse {

    private Integer           id;
    private String            nombre;
    private String            descripcion;
    private Boolean           activo;
    private BigDecimal        valorTotalEstimado;
    private Integer           creadoPorId;
    private String            creadoPorNombre;
    private LocalDateTime     creadoEn;
    private List<ComboItemResponse> productos;

    // ── Clase interna para cada producto del combo ────────────

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemResponse {
        private Integer    productoId;
        private String     productoNombre;
        private String     imagenUrl;
        private BigDecimal valorEstimado;
        private Integer    cantidad;
        private BigDecimal subtotal;   // valorEstimado * cantidad
    }
}
