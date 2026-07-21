package edu.eci.arsw.RoyalArena.model;

import java.time.LocalDateTime;

import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible para login y para mostrar. Único en el sistema.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Email para login. Único en el sistema.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Hash BCrypt de la contraseña. NULLABLE porque los usuarios que
     * se registren con Google (futuro) no tendrán password propia.
     */
    @Column(nullable = true, length = 100)
    private String password;

    /**
     * Rol del usuario en el sistema. Por default es PLAYER.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.PLAYER;

    /**
     * Método por el que se registró. Por default LOCAL.
     * Cuando implementemos OAuth, los usuarios de Google tendrán GOOGLE
     * y no tendrán password.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * ID de Google (el claim 'sub' del token de Google) si se registró con OAuth.
     * NULL para usuarios LOCAL. Sirve para lookup rápido cuando alguien hace login
     * con Google: se busca por googleId en vez de por email.
     */
    @Column(name = "google_id", nullable = true, length = 100)
    private String googleId;

    /**
     * Si el usuario está activo. Al desactivarlo, no puede hacer login
     * pero sus datos se conservan (soft delete). Útil para banear cuentas.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}