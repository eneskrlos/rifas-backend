package com.rifas.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * OpenApiConfig — Configura Swagger UI para soportar JWT.
 *
 * Sin esta clase, Swagger no muestra el botón "Authorize"
 * y no puede enviar el header Authorization: Bearer <token>
 * en las peticiones protegidas.
 *
 * Después de hacer login, copias el token y lo pegas en
 * el botón "Authorize" de Swagger para probar endpoints
 * que requieren autenticación como /auth/change-password.
 */
@Configuration
@OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title       = "Sistema de Rifas API",
        version     = "1.0",
        description = "API REST para gestión de rifas, combos y participantes"
    )
)
@SecurityScheme(
    name =  "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Autenticación con JWT. Ingrese el token después de 'Bearer '"
)
public class OpenApiConfig {
    // No se necesita código aquí, las anotaciones hacen todo el trabajo.
}
