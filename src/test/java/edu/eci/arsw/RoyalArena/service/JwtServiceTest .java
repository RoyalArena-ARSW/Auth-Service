package edu.eci.arsw.RoyalArena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import edu.eci.arsw.RoyalArena.model.Role;
import edu.eci.arsw.RoyalArena.model.User;
import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.RequiredTypeException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Tests del emisor de tokens.
 *
 * Lo importante aquí NO es que el token "se genere": es que cumpla el CONTRATO
 * que el Gateway espera. Auth firma, el Gateway valida — si los claims o el
 * secreto se desalinean, todo el sistema de autenticación se cae. Estos tests
 * congelan ese contrato.
 */
class JwtServiceTest {

    /** Mismo secreto largo que en producción (Auth y Gateway lo comparten). */
    private static final String SECRET =
            "RoyalArena2026SuperLongSecretKeyForHmacSha512JwtValidation!!!!!";
    private static final long EXPIRATION_MS = 86_400_000L; // 24h

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);

        user = User.builder()
                .id(42L)
                .username("diegoortiz")
                .email("diego@example.com")
                .password("$2a$10$hashfalso")
                .role(Role.PLAYER)
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .build();
    }

    /** Parsea con el mismo secreto, igual que hace el Gateway. */
    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    // ===== El contrato =====

    @Test
    @DisplayName("El token lleva los claims que el Gateway necesita")
    void tokenCarriesRequiredClaims() {
        Claims claims = parse(jwtService.generateToken(user));

        // El Gateway lee estos tres. Si alguno cambia de nombre, se rompe.
        assertThat(claims.get("userId")).isNotNull();
        assertThat(claims.get("username", String.class)).isEqualTo("diegoortiz");
        assertThat(claims.get("role", String.class)).isEqualTo("PLAYER");
        // subject: el fallback del Gateway si no encuentra userId
        assertThat(claims.getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("El rol viaja como String, no como enum serializado")
    void roleIsPlainString() {
        User admin = User.builder()
                .id(1L).username("admin").email("a@b.com")
                .role(Role.ADMIN).authProvider(AuthProvider.LOCAL).active(true)
                .build();

        assertThat(parse(jwtService.generateToken(admin)).get("role", String.class))
                .isEqualTo("ADMIN");
    }

    // ===== Firma =====

    @Test
    @DisplayName("Un token firmado con otro secreto NO valida")
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = jwtService.generateToken(user);
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "OtroSecretoCompletamenteDistintoDeSesentaYCuatroCaracteresXXXX".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> Jwts.parser().verifyWith(wrongKey).build()
                .parseSignedClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Un token manipulado NO valida")
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user);
        // Cambiar un caracter del payload invalida la firma
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";

        assertThatThrownBy(() -> parse(tampered)).isInstanceOf(Exception.class);
    }

    // ===== Expiración =====

    @Test
    @DisplayName("El token expira dentro de la ventana configurada")
    void tokenExpiresWithinConfiguredWindow() {
        Claims claims = parse(jwtService.generateToken(user));

        long window = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(window).isEqualTo(EXPIRATION_MS);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("getExpirationSeconds convierte de ms a segundos")
    void expirationSecondsMatchesConfig() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(EXPIRATION_MS / 1000);
    }

    // ===== Unicidad =====

    @Test
    @DisplayName("Usuarios distintos producen tokens distintos")
    void differentUsersGetDifferentTokens() {
        User other = User.builder()
                .id(99L).username("otro").email("o@b.com")
                .role(Role.PLAYER).authProvider(AuthProvider.LOCAL).active(true)
                .build();

        assertThat(jwtService.generateToken(user))
                .isNotEqualTo(jwtService.generateToken(other));
    }

    // ===== ⚠️ El contrato ROTO =====

    /**
     * ATENCIÓN: este test DOCUMENTA UN BUG REAL, no una feature.
     *
     * Auth pone el claim con .claim("userId", user.getId()) → un Long → que
     * viaja como NÚMERO en el JSON del payload. Al parsear, JJWT lo devuelve
     * como Integer (o Long si es grande), nunca como String.
     *
     * Y el AuthFilter del Gateway hace:
     *     String userId = claims.get("userId", String.class);
     *
     * Eso NO devuelve null: LANZA RequiredTypeException. Por eso el fallback
     * a getSubject() nunca se ejecuta, y por eso GET /api/profiles/me devuelve
     * 500 vía Gateway. Este test lo prueba.
     */
    @Test
    @DisplayName("BUG: userId viaja como numero y el Gateway lo lee como String -> explota")
    void userIdClaimIsNumericAndBreaksTheGateway() {
        Claims claims = parse(jwtService.generateToken(user));

        // El claim es numérico, no texto
        assertThat(claims.get("userId")).isInstanceOf(Number.class);

        // Y esto es EXACTAMENTE lo que hace el Gateway en AuthFilter:113
        assertThatThrownBy(() -> claims.get("userId", String.class))
                .isInstanceOf(RequiredTypeException.class)
                .hasMessageContaining("Cannot convert existing claim value");
    }
}