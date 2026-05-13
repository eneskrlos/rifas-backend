package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad ComboProducto — tabla pivote 'combo_productos'.
 *
 * Resuelve la relación Many-to-Many entre Combo y Producto,
 * agregando el campo extra 'cantidad' (cuántas unidades de
 * ese producto incluye el combo).
 *
 * No hereda BaseEntity porque no necesita 'creadoEn'
 * y su PK es propia SERIAL, no compuesta.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "combo_productos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_combo_producto",
            columnNames = {"combo_id", "producto_id"}
        )
    }
)
public class ComboProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * El combo al que pertenece este ítem.
     * LAZY: no cargamos el combo completo desde aquí.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comboprod_combo"))
    private Combo combo;

    /**
     * El producto referenciado.
     * ON DELETE RESTRICT en BD: no se puede borrar un producto
     * que esté en un combo activo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comboprod_producto"))
    private Producto producto;

    /**
     * Cuántas unidades de este producto incluye el combo.
     * Mínimo 1 (validado también en BD con CHECK).
     */
    @Builder.Default
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @Override
    public String toString() {
        return "ComboProducto{id=" + id
             + ", combo=" + (combo != null ? combo.getId() : "N/A")
             + ", producto=" + (producto != null ? producto.getId() : "N/A")
             + ", cantidad=" + cantidad + "}";
    }
}
