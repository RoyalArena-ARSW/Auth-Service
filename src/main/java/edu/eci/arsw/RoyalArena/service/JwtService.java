package edu.eci.arsw.RoyalArena.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.eci.arsw.RoyalArena.model.enums.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Construye la clave secreta a partir del string del application.properties.
     * Debe ser el MISMO secret que usa el Gateway para validar.
     */
    private SecretKey buildKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un JWT para el usuario dado. Los claims que incluimos:
     * - subject (sub): el ID del usuario como string (estándar JWT).
     * - userId: el ID del usuario (el Gateway lo lee para inyectar X-User-Id).
     * - username: para mostrar sin tener que consultar la BD.
     * - role: para autorización (el Gateway lo inyecta como X-User-Role).
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(buildKey())
                .compact();

        log.debug("Generated JWT for user {} (id={})", user.getUsername(), user.getId());
        return token;
    }

    /**
     * Devuelve la duración del token en segundos, para el AuthResponseDTO.
     */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}