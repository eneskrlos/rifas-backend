package com.rifas.service.impl;

import com.rifas.dto.request.ChangePasswordRequest;
import com.rifas.dto.request.LoginRequest;
import com.rifas.dto.request.RegisterRequest;
import com.rifas.dto.response.AuthResponse;
import com.rifas.entity.Rol;
import com.rifas.entity.Usuario;
import com.rifas.repository.RolRepository;
import com.rifas.repository.UsuarioRepository;
import com.rifas.security.util.JwtUtil;
import com.rifas.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthServiceImpl — Implementa la lógica de login y registro.
 *
 * Dos flujos principales:
 *
 * LOGIN:
 *   1. AuthenticationManager verifica usuario + contraseña contra BD
 *   2. Si es correcto, carga el Usuario completo
 *   3. Genera y devuelve el JWT
 *
 * REGISTER:
 *   1. Valida que email o teléfono no estén ya registrados
 *   2. Crea el Usuario con rol 'participante' y contraseña hasheada
 *   3. Genera y devuelve el JWT (el usuario queda logueado al registrarse)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository     usuarioRepository;
    private final RolRepository         rolRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;

    // ─────────────────────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        /**
         * AuthenticationManager.authenticate() hace todo el trabajo:
         *  1. Llama a UserDetailsServiceImpl.loadUserByUsername()
         *  2. Verifica la contraseña con BCrypt
         *  3. Lanza BadCredentialsException si algo falla
         *
         * Usamos el 'identifier' (email o teléfono) como username.
         * UserDetailsServiceImpl ya sabe buscar por ambos.
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );
        
        // Si llegamos aquí, las credenciales son correctas.
        // Cargamos el usuario completo para construir el token.
        Usuario usuario = usuarioRepository
                .findByEmailOrTelefono(
                        request.getIdentifier(),
                        request.getIdentifier()
                )
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado")
                );

        String token = jwtUtil.generarToken(usuario);

        log.info("Login exitoso para usuario: {}", usuario.getUsername());

        return construirAuthResponse(token, usuario);
    }

    // ─────────────────────────────────────────────────────────────
    //  REGISTRO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // ── Validar que al menos email o teléfono esté presente ──
        if ((request.getEmail() == null    || request.getEmail().isBlank()) &&
            (request.getTelefono() == null || request.getTelefono().isBlank())) {
            throw new RuntimeException(
                "Debe proporcionar al menos un email o número de teléfono"
            );
        }

        // ── Verificar que no exista ya ese email ──────────────────
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                "El email '" + request.getEmail() + "' ya está registrado"
            );
        }

        // ── Verificar que no exista ya ese teléfono ───────────────
        if (request.getTelefono() != null && !request.getTelefono().isBlank()
                && usuarioRepository.existsByTelefono(request.getTelefono())) {
            throw new RuntimeException(
                "El teléfono '" + request.getTelefono() + "' ya está registrado"
            );
        }

        // ── Obtener el rol 'participante' por defecto ─────────────
        Rol rolParticipante = rolRepository.findByNombre("participante")
                .orElseThrow(() ->
                        new RuntimeException("Rol 'participante' no encontrado en BD")
                );

        // ── Construir y guardar el nuevo usuario ──────────────────
        Usuario nuevoUsuario = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto())
                .email(request.getEmail() != null && !request.getEmail().isBlank()
                        ? request.getEmail() : null)
                .telefono(request.getTelefono() != null && !request.getTelefono().isBlank()
                        ? request.getTelefono() : null)
                // La contraseña SIEMPRE se hashea con BCrypt, nunca texto plano
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(rolParticipante)
                .activo(true)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // ── Generar token para que quede logueado al registrarse ──
        String token = jwtUtil.generarToken(usuarioGuardado);

        log.info("Nuevo participante registrado: {}", usuarioGuardado.getUsername());

        return construirAuthResponse(token, usuarioGuardado);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTODO PRIVADO DE APOYO
    // ─────────────────────────────────────────────────────────────

    /**
     * Construye el AuthResponse a partir del token y el usuario.
     * Reutilizado por login() y register() para no duplicar código.
     */
    private AuthResponse construirAuthResponse(String token, Usuario usuario) {
        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol().getNombre())
                .build();
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        // ── 1. Obtener el usuario del token JWT actual ────────────
        // SecurityContextHolder guarda quién está logueado en este request.
        // Como el endpoint requiere autenticación, siempre habrá un usuario.
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        log.info("Usuario autenticado para cambio de contraseña: {}", username);
        Usuario usuario = usuarioRepository
                .findByEmailOrTelefono(username, username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en BD"));
        // ── 2. Verificar que la contraseña actual sea correcta ───────
        // Usamos passwordEncoder.matches() que compara texto plano
        // contra el hash BCrypt almacenado. Nunca texto plano con texto plano.
        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPasswordHash())){
                throw new RuntimeException("la contraseña actual es incorrecta");
        }

        // ── 3. Verificar que la nueva contraseña y su confirmación coincidan ──
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new RuntimeException("La nueva contraseña y su confirmación no coinciden");
        }

        // ── 4. Verificar que la nueva sea distinta a la actual ───────────────
        // No tiene sentido "cambiar" a la misma contraseña, aunque técnicamente no es un error.
        if (passwordEncoder.matches(request.getNewPassword(), usuario.getPasswordHash())) {
                throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
        }

        // ── 5. Guardar el nuevo hash ──────────────────────────────
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada exitosamente para usuario: {}", usuario.getUsername());
    }
}
