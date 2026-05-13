package com.rifas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * AuditConfig — Le dice a JPA quién es el usuario actual.
 *
 * ¿Para qué sirve?
 * Cuando una entidad tiene @CreatedBy o @LastModifiedBy,
 * JPA necesita saber quién está logueado para poblar esos campos.
 *
 * En nuestro caso lo usamos principalmente para que
 * @EnableJpaAuditing funcione correctamente con @CreatedDate.
 *
 * AuditorAware<String> → devuelve el username del usuario activo.
 */
@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (auth == null || !auth.isAuthenticated()
                    || auth.getPrincipal().equals("anonymousUser")) {
                return Optional.of("sistema");
            }

            return Optional.of(auth.getName());
        };
    }
}
