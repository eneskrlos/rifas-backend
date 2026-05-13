package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad Sorteo — tabla 'sorteos'.
 *
 * Registra el resultado de un sorteo con trazabilidad total.
 * Solo puede existir UN sorteo por rifa (relación 1:1).
 *
 * El campo 'semilla' almacena el valor usado para inicializar
 * el generador aleatorio. Esto permite que cualquier auditor
 * pueda reproducir el mismo resultado matemáticamente.
 *
 * El campo 'algoritmo' documenta qué clase/método se usó
 * (ej: "java.security.SecureRandom").
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sorteos")
public class Sorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * La rifa que fue sorteada.
     * UNIQUE → solo un sorteo por rifa.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_sorteos_rifa"))
    private Rifa rifa;

    /** El número ganador dentro de la rifa. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_ganador_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sorteos_numero"))
    private NumeroRifa numeroGanador;

    /** El usuario que tenía ese número y ganó el premio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ganador_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sorteos_ganador"))
    private Usuario usuarioGanador;

    /**
     * Nombre del algoritmo utilizado para el sorteo.
     * Ej: "java.security.SecureRandom"
     */
    @Builder.Default
    @Column(name = "algoritmo", nullable = false, length = 100)
    private String algoritmo = "java.security.SecureRandom";

    /**
     * Semilla usada para el generador aleatorio.
     * Permite reproducir el sorteo para auditoría.
     * Guardada como String para soportar distintos formatos.
     */
    @Column(name = "semilla", nullable = false, length = 255)
    private String semilla;

    @CreatedDate
    @Column(name = "ejecutado_en", nullable = false, updatable = false)
    private LocalDateTime ejecutadoEn;

    @Override
    public String toString() {
        return "Sorteo{id=" + id
             + ", rifa=" + (rifa != null ? rifa.getId() : "N/A")
             + ", numeroGanador=" + (numeroGanador != null ? numeroGanador.getNumero() : "N/A")
             + ", ganador=" + (usuarioGanador != null ? usuarioGanador.getId() : "N/A") + "}";
    }
}
