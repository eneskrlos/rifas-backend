package com.rifas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidad Usuario — tabla 'usuarios'.
 *
 * Implementa UserDetails para integrarse directamente con
 * Spring Security sin necesidad de clases adaptadoras extra.
 *
 * Regla de negocio: email O teléfono debe estar presente.
 * Esta validación existe en la BD (CHECK constraint) y
 * también en el DTO de entrada con Bean Validation.
 */
@Getter
@Setter
@Builder // Permite construir objetos con Usuario.builder().campo(valor).build()
@NoArgsConstructor // Constructor sin argumentos requerido por JPA
@AllArgsConstructor // Constructor con todos los argumentos para facilitar pruebas y uso interno
@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_usuarios_email",    columnNames = "email"),
        @UniqueConstraint(name = "uq_usuarios_telefono", columnNames = "telefono")
    }
)
public class Usuario extends BaseEntity implements UserDetails {

    // ── Relación con Rol ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_usuarios_rol"))
    private Rol rol;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    /**
     * Email único. Puede ser NULL si el usuario se registró
     * con teléfono, pero al menos uno de los dos debe existir.
     */
    @Column(name = "email", unique = true, length = 150)
    private String email;

    /**
     * Teléfono único. Puede ser NULL si el usuario se registró
     * con email.
     */
    @Column(name = "telefono", unique = true, length = 20)
    private String telefono;

    /**
     * Contraseña almacenada como hash BCrypt.
     * NUNCA se almacena en texto plano.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ── UserDetails — Spring Security ────────────────────────

    /**
     * El "username" para Spring Security es el email si existe,
     * de lo contrario el teléfono. Es el identificador único
     * que se guarda en el token JWT.
     */
    @Override
    public String getUsername() {
        return email != null ? email : telefono;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * El rol se expone como autoridad con prefijo ROLE_
     * para que Spring Security entienda @PreAuthorize("hasRole('ADMIN')")
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + rol.getNombre().toUpperCase())
        );
    }

    @Override
    public boolean isAccountNonExpired()    { return true; }

    @Override
    public boolean isAccountNonLocked()     { return true; }

    @Override
    public boolean isCredentialsNonExpired(){ return true; }

    @Override
    public boolean isEnabled()              { return activo; }

    // ── toString sin datos sensibles ──────────────────────────
    @Override
    public String toString() {
        return "Usuario{id=" + getId()
             + ", nombre='" + nombreCompleto + "'"
             + ", rol='" + (rol != null ? rol.getNombre() : "N/A") + "'"
             + ", activo=" + activo + "}";
    }
}
