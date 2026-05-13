package com.rifas.security.filter;

import com.rifas.security.UserDetailsServiceImpl;
import com.rifas.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter — El portero de la API.
 *
 * Se ejecuta UNA VEZ por cada request HTTP (OncePerRequestFilter).
 * Su trabajo: leer el token JWT del header, validarlo y,
 * si es válido, decirle a Spring Security quién es el usuario.
 *
 * ¿Por qué extends OncePerRequestFilter?
 * Spring Security puede encadenar filtros que se ejecutan varias veces
 * por request en algunos escenarios (forward, include).
 * OncePerRequestFilter garantiza una sola ejecución por request real.
 *
 * Flujo de cada request:
 *
 * Request llega
 *      │
 *      ▼
 * ¿Tiene header Authorization: Bearer <token>?
 *      │ NO → continúa sin autenticar (las rutas públicas pasan igual)
 *      │ SI
 *      ▼
 * ¿El token es válido y no expiró?
 *      │ NO → continúa sin autenticar (Spring Security devolverá 401)
 *      │ SI
 *      ▼
 * Carga el usuario de BD y lo pone en SecurityContext
 *      │
 *      ▼
 * El Controller recibe la request con el usuario autenticado
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil              jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ── PASO 1: Verificar que el header existe y tiene formato correcto ──
        // El estándar es: Authorization: Bearer eyJhbGciOiJ...
        // Si no hay token o no empieza con "Bearer ", dejamos pasar sin autenticar.
        // Las rutas públicas (/auth/login, /auth/register) no necesitan token.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── PASO 2: Extraer el token (quitar el prefijo "Bearer ") ──
        final String token = authHeader.substring(7);

        // ── PASO 3: Extraer el username del token ──
        final String username;
        try {
            username = jwtUtil.extraerUsername(token);
        } catch (Exception e) {
            // Token malformado o con firma inválida → continuamos sin autenticar
            log.warn("Token JWT inválido: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── PASO 4: Solo autenticar si aún no hay autenticación en el contexto ──
        // SecurityContextHolder es el "bolsillo" de Spring Security por request.
        // Si ya tiene un usuario autenticado (por ejemplo, otro filtro lo puso),
        // no lo sobreescribimos.
        if (username != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ── PASO 5: Cargar el usuario real desde la BD ──
            // Necesitamos los detalles completos (rol, estado activo, etc.)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // ── PASO 6: Validar el token contra el usuario cargado ──
            // Verifica que el username del token coincida y que no haya expirado
            if (jwtUtil.esTokenValido(token, userDetails)) {

                // ── PASO 7: Crear el objeto de autenticación ──
                // UsernamePasswordAuthenticationToken es el objeto estándar
                // de Spring Security para representar un usuario autenticado.
                // Constructor de 3 parámetros → marca como autenticado (credentials=null)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials: null porque ya autenticamos
                                userDetails.getAuthorities()   // roles/permisos del usuario
                        );

                // Adjuntamos detalles del request (IP, session) al token de auth
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ── PASO 8: Registrar la autenticación en el contexto ──
                // A partir de aquí, en cualquier punto del código puedes hacer:
                // SecurityContextHolder.getContext().getAuthentication()
                // y obtener el usuario autenticado
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Usuario autenticado via JWT: {}, rol: {}",
                        username, userDetails.getAuthorities());
            }
        }

        // ── PASO 9: Continuar con el siguiente filtro/controller ──
        filterChain.doFilter(request, response);
    }
}
