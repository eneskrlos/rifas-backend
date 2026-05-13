package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad Producto — tabla 'productos'.
 *
 * Un producto es la unidad mínima de un premio.
 * Siempre pertenece a un Combo (a través de ComboProducto).
 *
 * Usamos BigDecimal para valorEstimado porque representa
 * dinero: nunca usar float/double para valores monetarios.
 */
@Getter
@Setter
@Builder // Builder para facilitar la creación de objetos en pruebas y servicios
@NoArgsConstructor // Constructor sin argumentos requerido por JPA
@AllArgsConstructor // Constructor con todos los argumentos para facilitar pruebas y uso interno
@Entity
@Table(name = "productos")
public class Producto extends BaseEntity {

    /**
     * Usuario (admin o manager) que creó el producto.
     * LAZY: no cargamos el usuario completo a menos que lo pidamos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false,
                foreignKey = @ForeignKey(name = "fk_productos_usuario"))
    private Usuario creadoPor;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * URL de la imagen del producto.
     * El archivo físico vive en un servicio de almacenamiento
     * (ej: S3, Cloudinary). Aquí solo guardamos la URL.
     */
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /**
     * Valor estimado del producto en la moneda local.
     * precision=12, scale=2 → máximo 9,999,999,999.99
     */
    @Builder.Default
    @Column(name = "valor_estimado", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal valorEstimado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Override
    public String toString() {
        return "Producto{id=" + getId()
             + ", nombre='" + nombre + "'"
             + ", valor=" + valorEstimado
             + ", activo=" + activo + "}";
    }
}
