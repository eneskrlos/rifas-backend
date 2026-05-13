package com.rifas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Clase base abstracta que todas las entidades heredan.
 *
 * - @MappedSuperclass  → JPA incluye sus campos en cada tabla hija,
 *                        pero esta clase NO tiene tabla propia.
 * - @EntityListeners  → Activa el mecanismo de JPA Auditing para
 *                        poblar automáticamente 'creadoEn'.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Se rellena automáticamente al hacer el primer INSERT.
     * updatable = false → nunca se modifica después de creado.
     */
    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
