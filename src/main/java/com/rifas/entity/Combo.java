package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Combo — tabla 'combos'.
 *
 * Un combo agrupa uno o más productos que se sortean juntos.
 * Toda rifa apunta a un combo (incluso si tiene un solo producto).
 *
 * La relación con productos se maneja a través de ComboProducto
 * (tabla pivote con campo 'cantidad').
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "combos")
public class Combo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false,
                foreignKey = @ForeignKey(name = "fk_combos_usuario"))
    private Usuario creadoPor;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Lista de productos del combo con sus cantidades.
     *
     * CascadeType.ALL → si borramos el combo, se borran sus
     * filas en combo_productos (ON DELETE CASCADE también en BD).
     *
     * orphanRemoval = true → si quitamos un ComboProducto de
     * esta lista y guardamos, se elimina de la BD.
     */
    @Builder.Default
    @OneToMany(mappedBy = "combo",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ComboProducto> comboProductos = new ArrayList<>();

    // ── Métodos de conveniencia ───────────────────────────────

    /**
     * Agrega un producto al combo con su cantidad.
     * Mantiene la bidireccionalidad de la relación.
     */
    public void agregarProducto(Producto producto, int cantidad) {
        ComboProducto cp = ComboProducto.builder()
                .combo(this)
                .producto(producto)
                .cantidad(cantidad)
                .build();
        comboProductos.add(cp);
    }

    @Override
    public String toString() {
        return "Combo{id=" + getId()
             + ", nombre='" + nombre + "'"
             + ", activo=" + activo + "}";
    }
}
