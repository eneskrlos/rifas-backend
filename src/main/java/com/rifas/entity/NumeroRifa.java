package com.rifas.entity;

import com.rifas.entity.enums.EstadoNumero;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad NumeroRifa — tabla 'numeros_rifa'.
 *
 * Representa cada número individual dentro de una rifa.
 * Los 200 números (o los que correspondan) se generan
 * automáticamente por el trigger de BD al activar la rifa.
 *
 * El estado cambia de DISPONIBLE a RESERVADO cuando un
 * participante lo elige (también via trigger de BD).
 *
 * No hereda BaseEntity porque no necesita 'creadoEn'.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "numeros_rifa",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_numero_por_rifa",
            columnNames = {"rifa_id", "numero"}
        )
    }
)
public class NumeroRifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Rifa a la que pertenece este número.
     * Si se borra la rifa, se borran sus números (CASCADE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_numeros_rifa"))
    private Rifa rifa;

    /**
     * El número en sí (ej: 1, 2, ... 200).
     * Es único por rifa (UQ constraint arriba).
     */
    @Column(name = "numero", nullable = false)
    private Integer numero;

    /**
     * Estado del número.
     * EnumType.STRING → guarda "DISPONIBLE" o "RESERVADO" en BD.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoNumero estado = EstadoNumero.DISPONIBLE;

    // ── Métodos de conveniencia ───────────────────────────────

    public boolean estaDisponible() {
        return EstadoNumero.DISPONIBLE.equals(this.estado);
    }

    public void reservar() {
        this.estado = EstadoNumero.RESERVADO;
    }

    @Override
    public String toString() {
        return "NumeroRifa{id=" + id
             + ", numero=" + numero
             + ", estado=" + estado + "}";
    }
}
