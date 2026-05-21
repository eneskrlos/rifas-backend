package com.rifas.config;

import com.rifas.security.UserDetailsServiceImpl;
import com.rifas.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — El cerebro de la seguridad de la API.
 *
 * ¿Qué configura esta clase?
 *  1. Qué rutas son públicas y cuáles necesitan autenticación
 *  2. Qué rutas requieren un rol específico (admin, manager)
 *  3. Cómo se verifica la identidad (JWT, no sesiones)
 *  4. Cómo se cifran las contraseñas (BCrypt)
 *  5. CORS: qué orígenes pueden llamar a la API (React frontend)
 *
 * @EnableWebSecurity    → activa Spring Security en la app
 * @EnableMethodSecurity → habilita @PreAuthorize en los controllers
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Rutas completamente públicas — no necesitan token.
     * Cualquier persona puede acceder sin autenticarse.
     */
    private static final String[] RUTAS_PUBLICAS = {
            "/auth/login", // login con email/teléfono + contraseña
            "/auth/register", // registro de nuevo participante
            "/swagger-ui/**",        // documentación Swagger UI
            "/swagger-ui.html",
            "/v3/api-docs/**",       // OpenAPI JSON
            "/actuator/health"       // health check
    };

    /**
     * Rutas exclusivas del ADMIN.
     * Solo el rol 'admin' puede acceder.
     */
    private static final String[] RUTAS_ADMIN = {
            "/admin/**"
    };

    /**
     * Rutas del MANAGER (y también accesibles por admin).
     * El admin tiene acceso a todo porque tiene más jerarquía.
     */
    private static final String[] RUTAS_MANAGER = {
            "/productos/**",
            "/combos/**",
            "/rifas/*/activar",
            "/rifas/*/cerrar",
            "/rifas/*/numeros",
            "/sorteos/**"
    };

    // ─────────────────────────────────────────────────────────────
    //  CADENA DE FILTROS — El corazón de la configuración
    // ─────────────────────────────────────────────────────────────

    /**
     * SecurityFilterChain define el comportamiento de cada request.
     *
     * ¿Por qué desactivamos CSRF?
     * CSRF (Cross-Site Request Forgery) es un ataque relevante
     * para aplicaciones con sesiones y cookies. Las APIs REST con JWT
     * no usan cookies de sesión, por lo que CSRF no aplica aquí.
     * Desactivarlo evita complejidad innecesaria.
     *
     * ¿Por qué STATELESS?
     * Le dice a Spring Security que NO cree ni use sesiones HTTP.
     * Cada request se autentica completamente via JWT, sin memoria
     * entre requests. Esto hace la API horizontalmente escalable.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // ── Sin CSRF (API REST con JWT, no sesiones) ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS configurado para el frontend React ────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Reglas de autorización por ruta ───────────────────
            .authorizeHttpRequests(auth -> auth

                // Rutas públicas: sin token
                .requestMatchers(RUTAS_PUBLICAS).permitAll()

                // Solo admin
                .requestMatchers(RUTAS_ADMIN).hasRole("ADMIN")

                // Manager o admin
                .requestMatchers(RUTAS_MANAGER).hasAnyRole("MANAGER", "ADMIN")

                // Todo lo demás requiere estar autenticado
                // (participantes logueados pueden acceder)
                .anyRequest().authenticated()
            )

            // ── Sin sesiones: cada request se autentica con JWT ───
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Proveedor de autenticación personalizado ──────────
            .authenticationProvider(authenticationProvider())

            // ── Insertar nuestro filtro JWT ANTES del filtro estándar ──
            // UsernamePasswordAuthenticationFilter es el filtro por defecto
            // de Spring Security para form login. Lo reemplazamos con el nuestro.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────
    //  AUTENTICACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * AuthenticationProvider — cómo verificamos usuario + contraseña.
     *
     * DaoAuthenticationProvider es la implementación estándar que:
     *  1. Llama a UserDetailsService para cargar el usuario de BD
     *  2. Usa PasswordEncoder para comparar la contraseña ingresada
     *     con el hash almacenado (BCrypt)
     *
     * Nunca compara contraseñas en texto plano.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — el coordinador del proceso de login.
     *
     * El AuthController lo usa para ejecutar la autenticación
     * cuando alguien hace POST /auth/login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt — el algoritmo para cifrar contraseñas.
     *
     * ¿Por qué BCrypt y no MD5 o SHA-256?
     * BCrypt incluye un "salt" aleatorio automáticamente y tiene un
     * factor de costo configurable (por defecto 10 rondas).
     * Esto lo hace extremadamente lento para ataques de fuerza bruta,
     * pero imperceptiblemente rápido para un login normal.
     * MD5/SHA son rápidos → malos para contraseñas.
     * BCrypt es lento a propósito → bueno para contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ─────────────────────────────────────────────────────────────
    //  CORS
    // ─────────────────────────────────────────────────────────────

    /**
     * CorsConfigurationSource — controla quién puede llamar a la API.
     *
     * CORS (Cross-Origin Resource Sharing) es una política del navegador
     * que bloquea requests desde dominios distintos al del servidor.
     * Sin esta configuración, el frontend React en localhost:5173
     * NO podría llamar a la API en localhost:8080.
     *
     * En producción: cambiar "http://localhost:5173" por el dominio real.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend React en desarrollo y producción)
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000"    // Create React App dev server
        ));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permitidos en la request
        // Authorization es el header donde va el JWT
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // ¿Permitir cookies/credenciales? Sí, para JWT en headers
        config.setAllowCredentials(true);

        // Aplicar esta configuración a TODAS las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
