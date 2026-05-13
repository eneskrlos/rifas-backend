package com.rifas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AuthResponse — Lo que devuelve la API tras un login o registro exitoso.
 *
 * ¿Por qué devolvemos el rol y el nombre?
 * Para que el frontend React pueda:
 *  - Mostrar el nombre del usuario en la interfaz inmediatamente
 *  - Redirigir al panel correcto según el rol (admin, manager, participante)
 *  - Sin necesidad de hacer otra llamada a la API para obtener el perfil
 *
 * El token es lo más importante: el frontend lo guarda
 * y lo envía en cada request posterior en el header:
 * Authorization: Bearer <token>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String  token;
    private String  tipo;           // siempre "Bearer"
    private Integer usuarioId;
    private String  nombreCompleto;
    private String  email;
    private String  telefono;
    private String  rol;
}
