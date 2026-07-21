package edu.eci.arsw.RoyalArena.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    /**
     * JWT que el cliente debe usar en el header Authorization: Bearer <token>
     * para todas las peticiones autenticadas.
     */
    private String token;

    /**
     * Tipo de token, siempre "Bearer" (estándar OAuth2). Se lo mandamos al frontend
     * para que arme el header correctamente sin tener que hardcodearlo.
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Duración del token en segundos, útil para que el frontend sepa
     * cuándo pedir refresh.
     */
    private Long expiresIn;

    /**
     * Info del usuario autenticado. Evita que el frontend tenga que hacer
     * una segunda llamada a /api/auth/me después del login.
     */
    private UserResponseDTO user;
}