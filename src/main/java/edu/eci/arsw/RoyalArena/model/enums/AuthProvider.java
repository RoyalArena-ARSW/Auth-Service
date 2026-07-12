package edu.eci.arsw.royalarena.auth.model.enums;

/**
 * Método por el cual el usuario se registró.
 * LOCAL: registro tradicional con email + password (guardado con BCrypt).
 * GOOGLE: registro vía OAuth con Google (no tiene password local).
 *
 * De momento solo usamos LOCAL. GOOGLE queda planificado para el futuro
 * sin implementar la lógica todavía.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}