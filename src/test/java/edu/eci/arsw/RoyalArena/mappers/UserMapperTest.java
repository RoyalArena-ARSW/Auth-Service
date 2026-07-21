package edu.eci.arsw.RoyalArena.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.eci.arsw.RoyalArena.dto.response.UserResponseDTO;
import edu.eci.arsw.RoyalArena.model.Role;
import edu.eci.arsw.RoyalArena.model.User;
import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    private User fullUser() {
        return User.builder()
                .id(1L)
                .username("diegoortiz")
                .email("diego@example.com")
                .password("$2a$10$secretoQueNuncaDebeSalir")
                .role(Role.PLAYER)
                .authProvider(AuthProvider.LOCAL)
                .googleId("google-sub-123")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Mapea los campos publicos del usuario")
    void mapsPublicFields() {
        UserResponseDTO dto = mapper.toDto(fullUser());

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("diegoortiz");
        assertThat(dto.getEmail()).isEqualTo("diego@example.com");
        assertThat(dto.getRole()).isEqualTo(Role.PLAYER);
        assertThat(dto.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(dto.getActive()).isTrue();
        assertThat(dto.getCreatedAt()).isNotNull();
    }

    /**
     * El DTO NO debe tener siquiera el campo. Si alguien lo agrega, este test
     * falla antes de que el hash llegue a un cliente.
     */
    @Test
    @DisplayName("El DTO de respuesta no expone password ni googleId")
    void responseDtoHasNoSecrets() {
        String[] fields = java.util.Arrays.stream(UserResponseDTO.class.getDeclaredFields())
                .map(Field::getName)
                .toArray(String[]::new);

        assertThat(fields)
                .as("UserResponseDTO nunca debe llevar el hash de la clave")
                .doesNotContain("password")
                .doesNotContain("googleId");
    }

    @Test
    @DisplayName("Mapear null devuelve null")
    void mapsNullToNull() {
        assertThat(mapper.toDto(null)).isNull();
    }
}