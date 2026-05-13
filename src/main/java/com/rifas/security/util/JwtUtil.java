package com.rifas.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.rifas.entity.Usuario;

/**
 * JwtUtil — Responsabilidad única: todo lo relacionado con tokens JWT.
 *
 * ¿Por qué @Component y no @Service?
 * Porque no contiene lógica de negocio, es una utilidad técnica pura.
 * @Component es más honesto semánticamente.
 *
 * ¿Por qué HMAC-SHA256 (HS256)?
 * Usamos clave simétrica: la misma clave firma y verifica.
 * Es suficiente para una API donde el servidor es el único
 * que firma Y verifica. Si necesitaras que terceros verifiquen
 * sin poder firmar, usarías RS256 (asimétrico).
 */
@Component
public class JwtUtil {

    /**
     * La clave secreta viene del application.yml (jwt.secret).
     * NUNCA hardcodeada en el código fuente.
     * En producción esta clave vive en variables de entorno.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Tiempo de vida del token de acceso.
     * 86400000 ms = 24 horas.
     * Después de este tiempo el usuario debe volver a loguearse.
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ────────────────────────────────────────────────────────────
    //  GENERACIÓN DE TOKEN
    // ────────────────────────────────────────────────────────────

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * ¿Qué guardamos en el payload (claims)?
     *  - subject   → el username (email o teléfono): identifica al usuario
     *  - rol       → el rol del usuario: para autorización sin ir a la BD
     *  - userId    → el ID del usuario: para buscar datos rápidamente
     *  - iat       → issued at: cuándo fue creado
     *  - exp       → expiration: cuándo expira
     *
     * ¿Por qué guardar el rol en el token?
     * Para que el filtro pueda verificar permisos SIN consultar la BD
     * en cada request. Una petición con token válido ya lleva su rol.
     *
     * IMPORTANTE: No guardes datos sensibles en el payload del JWT
     * porque es Base64 decodificable (no es cifrado, es firmado).
     */
    public String generarToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(); // Creacion del claims vacío

        // Accedemos al Usuario real para extraer rol e ID
        if (userDetails instanceof Usuario usuario) {
            claims.put("rol",    usuario.getRol().getNombre());
            claims.put("userId", usuario.getId());
        }

        return construirToken(claims, userDetails.getUsername());
    }

    private String construirToken(Map<String, Object> claims, String subject) {
        Date ahora    = new Date();
        Date expira   = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)           // email o teléfono
                .issuedAt(ahora)            // fecha de creación
                .expiration(expira)         // fecha de expiración
                .signWith(getSecretKey())   // firma con HMAC-SHA256
                .compact();                 // serializa a String
    }

    // ────────────────────────────────────────────────────────────
    //  VALIDACIÓN DE TOKEN
    // ────────────────────────────────────────────────────────────

    /**
     * Valida que el token sea legítimo y corresponda al usuario.
     *
     * Verifica dos cosas:
     *  1. El username del token coincide con el usuario cargado de BD
     *  2. El token no ha expirado
     *
     * La firma ya fue verificada implícitamente al parsear el token.
     * Si la firma fuera inválida, parsearToken() lanzaría JwtException
     * antes de llegar aquí.
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return username.equals(userDetails.getUsername())
               && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    // ────────────────────────────────────────────────────────────
    //  EXTRACCIÓN DE DATOS DEL TOKEN
    // ────────────────────────────────────────────────────────────

    /** Extrae el username (email o teléfono) del token. */
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /** Extrae el rol del token (sin ir a la BD). */
    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    /** Extrae el ID del usuario del token (sin ir a la BD). */
    public Integer extraerUserId(String token) {
        return extraerClaim(token, claims -> claims.get("userId", Integer.class));
    }

    private Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * Método genérico para extraer cualquier claim del token.
     *
     * ¿Por qué usar Function<Claims, T>?
     * Porque todos los claims viven en el mismo objeto Claims.
     * Con este patrón evitamos parsear el token múltiples veces.
     * El caller decide qué extraer pasando una lambda.
     *
     * Ejemplo: extraerClaim(token, Claims::getSubject)
     *          extraerClaim(token, c -> c.get("rol", String.class))
     */
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parsearToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y verifica la firma del token.
     *
     * Si el token fue alterado, la firma no coincidirá y
     * lanzará JwtException automáticamente.
     * Si el token expiró, lanzará ExpiredJwtException.
     */
    private Claims parsearToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Convierte el String secreto en una clave criptográfica real.
     *
     * ¿Por qué Keys.hmacShaKeyFor()?
     * JJWT requiere una SecretKey tipada, no un String crudo.
     * Este método garantiza que la clave tenga el tamaño mínimo
     * requerido para HMAC-SHA256 (256 bits = 32 bytes).
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Verifica si un token es parseble sin lanzar excepción.
     * Útil para logging y diagnóstico.
     */
    public boolean esTokenParseable(String token) {
        try {
            parsearToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
