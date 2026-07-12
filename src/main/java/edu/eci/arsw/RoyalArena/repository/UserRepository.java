package edu.eci.arsw.RoyalArena.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.eci.arsw.RoyalArena.model.enums.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca por email exacto. Se usa en el flujo de login normal
     * cuando el usuario ingresa un email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca por username exacto. Se usa en el flujo de login
     * cuando el usuario ingresa un username en vez de email.
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca por email O username. Spring Data traduce esto automáticamente a:
     *   SELECT * FROM users WHERE email = ? OR username = ?
     *
     * Este es el método clave para el login flexible: el usuario ingresa
     * su "identifier" (que puede ser cualquiera de los dos), y buscamos
     * pasándolo como parámetro para ambos campos.
     */
    Optional<User> findByEmailOrUsername(String email, String username);

    /**
     * Busca por googleId. Para el futuro flujo de login con Google:
     * cuando alguien hace login con Google, buscamos por el 'sub' del token
     * en vez de por email (más rápido y confiable).
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Verifica si ya existe un usuario con ese email. Se usa al registrar
     * para validar unicidad antes de intentar crear el registro.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si ya existe un usuario con ese username. Se usa al registrar
     * para validar unicidad.
     */
    boolean existsByUsername(String username);
}