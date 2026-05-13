package com.rifas.service;

import com.rifas.dto.request.ChangePasswordRequest;
import com.rifas.dto.request.LoginRequest;
import com.rifas.dto.request.RegisterRequest;
import com.rifas.dto.response.AuthResponse;

/**
 * AuthService — Contrato del servicio de autenticación.
 *
 * Define QUÉ puede hacer la capa de auth, sin decir CÓMO.
 * El controller solo conoce esta interfaz, nunca la implementación.
 *
 * Beneficio: si mañana cambias la implementación (ej: agregar
 * autenticación con Google OAuth), el controller no cambia nada.
 */
public interface AuthService {

    /**
     * Autentica al usuario y devuelve un token JWT.
     * Lanza excepción si las credenciales son incorrectas.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registra un nuevo participante y devuelve su token JWT.
     * Lanza excepción si el email/teléfono ya está registrado.
     */
    AuthResponse register(RegisterRequest request);

    void changePassword(ChangePasswordRequest request);
}
