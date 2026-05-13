package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad Participacion — tabla 'participaciones'.
 *
 * Registra que un usuario eligió un número específico
 * dentro de una rifa. Es el corazón transaccional del sistema.
 *
 * Reglas de negocio que se verifican antes de insertar:
 *  1. El número debe estar en estado DISPONIBLE
 *  2. La rifa debe estar en estado ACTIVA
 *  3. El usuario no debe superar max_por_persona (si aplica)
 *     → El trigger de BD es la segunda línea de defensa.
 *
 * No hereda BaseEntity para tener control total sobre
 * la PK y el timestamp de asignación.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "participaciones",
    uniqueConstraints = {
        // Un número solo puede estar en UNA participación
        @UniqueConstraint(
            name = "uq_numero_participacion",
            columnNames = "numero_rifa_id"
        )
    }
)
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_part_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_part_rifa"))
    private Rifa rifa;

    /**
     * El número elegido. Unique → garantiza que dos personas
     * no puedan tener el mismo número en la misma rifa.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_rifa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_part_numero"))
    private NumeroRifa numeroRifa;

    @CreatedDate
    @Column(name = "asignado_en", nullable = false, updatable = false)
    private LocalDateTime asignadoEn;

    @Override
    public String toString() {
        return "Participacion{id=" + id
             + ", usuario=" + (usuario != null ? usuario.getId() : "N/A")
             + ", rifa=" + (rifa != null ? rifa.getId() : "N/A")
             + ", numero=" + (numeroRifa != null ? numeroRifa.getNumero() : "N/A") + "}";
    }
}
