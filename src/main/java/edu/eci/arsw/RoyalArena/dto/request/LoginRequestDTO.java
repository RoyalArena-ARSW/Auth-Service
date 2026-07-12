package edu.eci.arsw.royalarena.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    /**
     * Puede ser el email O el username del usuario. El service busca por ambos.
     * Ejemplos válidos:
     *   - "diego@example.com"
     *   - "diegoortiz"
     */
    @NotBlank(message = "Identifier (email or username) is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}