package com.rifas.entity;

import com.rifas.entity.enums.EstadoRifa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Rifa — tabla 'rifas'.
 *
 * Representa un evento de rifa completo. Contiene:
 * - El combo que se sortea
 * - La cantidad de números (1..totalNumeros)
 * - El límite de números por persona (NULL = sin límite)
 * - Su estado dentro del ciclo de vida (EstadoRifa)
 *
 * Cuando el admin/manager cambia el estado a ACTIVA,
 * el trigger de BD genera automáticamente los números.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rifas")
public class Rifa extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rifas_usuario"))
    private Usuario creadoPor;

    /**
     * Combo de productos que se sortea en esta rifa.
     * RESTRICT en BD: no se puede borrar un combo que tenga rifas.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id",
                foreignKey = @ForeignKey(name = "fk_rifas_combo"))
    private Combo combo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Cuántos números tendrá la rifa (ej: 200).
     * El trigger de BD los genera al activar.
     */
    @Builder.Default
    @Column(name = "total_numeros", nullable = false)
    private Integer totalNumeros = 200;

    /**
     * Máximo de números que puede elegir un participante.
     * NULL = sin límite superior (mínimo siempre es 1).
     */
    @Column(name = "max_por_persona")
    private Integer maxPorPersona;

    /**
     * Estado actual en el ciclo de vida de la rifa.
     * EnumType.STRING → guarda "ACTIVA" en BD, no "1".
     * Más legible y resistente a cambios de orden del enum.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoRifa estado = EstadoRifa.BORRADOR;

    @Column(name = "inicio_en")
    private LocalDateTime inicioEn;

    @Column(name = "sorteo_en")
    private LocalDateTime sorteoEn;

    /**
     * Números generados para esta rifa.
     * Se populan automáticamente por el trigger al activar.
     */
    @Builder.Default
    @OneToMany(mappedBy = "rifa",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<NumeroRifa> numeros = new ArrayList<>();

    // ── Métodos de conveniencia ───────────────────────────────

    /** ¿La rifa acepta nuevas participaciones? */
    public boolean estaActiva() {
        return EstadoRifa.ACTIVA.equals(this.estado);
    }

    /** Activar la rifa (el trigger de BD generará los números) */
    public void activar() {
        if (!EstadoRifa.BORRADOR.equals(this.estado)) {
            throw new IllegalStateException(
                "Solo se puede activar una rifa en estado BORRADOR"
            );
        }
        this.estado = EstadoRifa.ACTIVA;
    }

    /** Cerrar la rifa para el sorteo */
    public void cerrar() {
        if (!EstadoRifa.ACTIVA.equals(this.estado)) {
            throw new IllegalStateException(
                "Solo se puede cerrar una rifa ACTIVA"
            );
        }
        this.estado = EstadoRifa.CERRADA;
    }

    /** Marcar como sorteada */
    public void marcarSorteada() {
        this.estado = EstadoRifa.SORTEADA;
    }

    /** Cancelar la rifa */
    public void cancelar() {
        if (EstadoRifa.SORTEADA.equals(this.estado)) {
            throw new IllegalStateException(
                "No se puede cancelar una rifa ya sorteada"
            );
        }
        this.estado = EstadoRifa.CANCELADA;
    }

    @Override
    public String toString() {
        return "Rifa{id=" + getId()
             + ", nombre='" + nombre + "'"
             + ", estado=" + estado
             + ", totalNumeros=" + totalNumeros + "}";
    }
}
