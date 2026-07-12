package edu.eci.arsw.RoyalArena.exception;

/**
 * Se lanza cuando las credenciales de login son incorrectas:
 * el usuario no existe, la contraseña no coincide, o la cuenta está desactivada.
 *
 * IMPORTANTE: no distinguimos entre "usuario no existe" y "contraseña incorrecta"
 * en el mensaje al cliente. Ambos casos devuelven el mismo error genérico
 * para evitar user enumeration attacks (un atacante que descubre qué emails
 * están registrados intentando distintos combinaciones).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}