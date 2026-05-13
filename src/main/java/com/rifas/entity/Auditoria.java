package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad Auditoria — tabla 'auditoria'.
 *
 * Log inmutable de todas las acciones importantes del sistema.
 * Ningún campo debe poder modificarse después de insertado:
 * - No hay @Setter global (solo @Getter)
 * - Todos los @Column tienen updatable = false
 *
 * El campo usuario_id es nullable intencionalmente para
 * soportar acciones del sistema (sin usuario autenticado),
 * como migraciones Flyway o procesos batch.
 *
 * No hereda BaseEntity para mantener control total.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Quién ejecutó la acción. Puede ser NULL si es
     * una acción automática del sistema.
     * Sin @ManyToOne para no depender del ciclo de vida
     * del usuario (un log debe sobrevivir a un usuario borrado).
     */
    @Column(name = "usuario_id", updatable = false)
    private Integer usuarioId;

    /**
     * Descripción corta de la acción.
     * Ejemplos: "RIFA_ACTIVADA", "NUMERO_ELEGIDO", "SORTEO_EJECUTADO"
     */
    @Column(name = "accion", nullable = false, length = 100, updatable = false)
    private String accion;

    /**
     * Tabla de BD que fue afectada.
     * Ejemplo: "rifas", "participaciones"
     */
    @Column(name = "tabla_afectada", length = 100, updatable = false)
    private String tablaAfectada;

    /**
     * ID del registro afectado en esa tabla.
     * Permite rastrear exactamente qué fila cambió.
     */
    @Column(name = "registro_id", updatable = false)
    private Integer registroId;

    /**
     * Detalle libre en texto. Se puede usar para guardar
     * el JSON anterior/nuevo del objeto, o un mensaje descriptivo.
     */
    @Column(name = "detalle", columnDefinition = "TEXT", updatable = false)
    private String detalle;

    /**
     * IP desde donde se originó la acción.
     * length = 45 para soportar IPv6 completo.
     */
    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;

    @CreatedDate
    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private LocalDateTime ocurridoEn;

    @Override
    public String toString() {
        return "Auditoria{id=" + id
             + ", accion='" + accion + "'"
             + ", tabla='" + tablaAfectada + "'"
             + ", registroId=" + registroId
             + ", ocurrido=" + ocurridoEn + "}";
    }
}
