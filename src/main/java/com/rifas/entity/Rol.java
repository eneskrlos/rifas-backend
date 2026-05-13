package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa un rol del sistema (admin, manager, participante).
 *
 * Hereda 'id' y 'creadoEn' de BaseEntity.
 * No tiene @Setter global porque el rol no debería
 * modificarse libremente; se usa @Builder para construcción.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Rol extends BaseEntity {

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // ── toString para logs (sin datos sensibles) ──────────────
    @Override
    public String toString() {
        return "Rol{id=" + getId() + ", nombre='" + nombre + "'}";
    }
}
