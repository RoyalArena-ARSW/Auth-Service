package edu.eci.arsw.RoyalArena.dto.response;

import java.time.LocalDateTime;

import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;
import edu.eci.arsw.RoyalArena.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private AuthProvider authProvider;
    private Boolean active;
    private LocalDateTime createdAt;

    // Ojo: NO incluimos password ni googleId. El password nunca sale del servidor,
    // y googleId es info interna de OAuth que el frontend no necesita.
}