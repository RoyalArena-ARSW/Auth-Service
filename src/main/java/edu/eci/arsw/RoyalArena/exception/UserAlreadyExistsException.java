package edu.eci.arsw.RoyalArena.exception;

/**
 * Se lanza cuando alguien intenta registrarse con un email o username
 * que ya está en uso.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}