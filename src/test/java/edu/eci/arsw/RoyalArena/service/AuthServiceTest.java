package edu.eci.arsw.RoyalArena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import edu.eci.arsw.RoyalArena.dto.request.LoginRequestDTO;
import edu.eci.arsw.RoyalArena.dto.request.RegisterRequestDTO;
import edu.eci.arsw.RoyalArena.dto.response.AuthResponseDTO;
import edu.eci.arsw.RoyalArena.exception.InvalidCredentialsException;
import edu.eci.arsw.RoyalArena.exception.UserAlreadyExistsException;
import edu.eci.arsw.RoyalArena.mappers.UserMapperImpl;
import edu.eci.arsw.RoyalArena.model.Role;
import edu.eci.arsw.RoyalArena.model.User;
import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;
import edu.eci.arsw.RoyalArena.repository.UserRepository;

/**
 * Tests de registro y login.
 *
 * El repositorio va mockeado (no queremos BD), pero el PasswordEncoder es
 * BCrypt REAL: justamente queremos verificar el hashing de verdad, no que un
 * mock devuelva lo que le digamos.
 */
class AuthServiceTest {

    private static final String SECRET =
            "RoyalArena2026SuperLongSecretKeyForHmacSha512JwtValidation!!!!!";
    private static final String RAW_PASSWORD = "miPassword123";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(); // real, no mock

        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);

        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, new UserMapperImpl());
    }

    /** Usuario LOCAL activo con la contraseña ya hasheada. */
    private User existingUser() {
        return User.builder()
                .id(1L)
                .username("diegoortiz")
                .email("diego@example.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .role(Role.PLAYER)
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .build();
    }

    private RegisterRequestDTO registerRequest() {
        return RegisterRequestDTO.builder()
                .username("diegoortiz")
                .email("diego@example.com")
                .password(RAW_PASSWORD)
                .build();
    }

    // ===== Registro =====

    @Test
    @DisplayName("La contrasena NUNCA se guarda en texto plano")
    void passwordIsNeverStoredInPlainText() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        String stored = captor.getValue().getPassword();

        assertThat(stored).isNotEqualTo(RAW_PASSWORD);
        assertThat(stored).startsWith("$2");                    // formato BCrypt
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored)).isTrue();
    }

    @Test
    @DisplayName("El registro asigna PLAYER y LOCAL por defecto, y deja la cuenta activa")
    void registerAssignsDefaults() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(Role.PLAYER);
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getGoogleId()).isNull();
    }

    @Test
    @DisplayName("El registro devuelve token listo: auto-login")
    void registerReturnsUsableToken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponseDTO response = authService.register(registerRequest());

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isPositive();
        assertThat(response.getUser().getUsername()).isEqualTo("diegoortiz");
    }

    @Test
    @DisplayName("Email duplicado: 409 y no se guarda nada")
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("diego@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("diego@example.com");

        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never())
                .save(any(User.class));
    }

    @Test
    @DisplayName("Username duplicado: 409 y no se guarda nada")
    void duplicateUsernameIsRejected() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("diegoortiz")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(UserAlreadyExistsException.class);

        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never())
                .save(any(User.class));
    }

    // ===== Login flexible =====

    @Test
    @DisplayName("Se puede entrar con el email")
    void loginWithEmail() {
        when(userRepository.findByEmailOrUsername("diego@example.com", "diego@example.com"))
                .thenReturn(Optional.of(existingUser()));

        AuthResponseDTO res = authService.login(
                new LoginRequestDTO("diego@example.com", RAW_PASSWORD));

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getUser().getEmail()).isEqualTo("diego@example.com");
    }

    @Test
    @DisplayName("Se puede entrar con el username")
    void loginWithUsername() {
        when(userRepository.findByEmailOrUsername("diegoortiz", "diegoortiz"))
                .thenReturn(Optional.of(existingUser()));

        AuthResponseDTO res = authService.login(
                new LoginRequestDTO("diegoortiz", RAW_PASSWORD));

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getUser().getUsername()).isEqualTo("diegoortiz");
    }

    // ===== Seguridad del login =====

    /**
     * Anti user-enumeration: "no existe" y "clave mala" deben ser
     * INDISTINGUIBLES desde fuera. Si difirieran, un atacante podría descubrir
     * qué emails están registrados probando contraseñas cualquiera.
     */
    @Test
    @DisplayName("Usuario inexistente y clave incorrecta dan EL MISMO mensaje")
    void wrongUserAndWrongPasswordAreIndistinguishable() {
        when(userRepository.findByEmailOrUsername("fantasma", "fantasma"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailOrUsername("diegoortiz", "diegoortiz"))
                .thenReturn(Optional.of(existingUser()));

        String noSuchUser = catchMessage(() ->
                authService.login(new LoginRequestDTO("fantasma", "loquesea")));
        String wrongPassword = catchMessage(() ->
                authService.login(new LoginRequestDTO("diegoortiz", "claveIncorrecta")));

        assertThat(noSuchUser).isEqualTo(wrongPassword);
    }

    private String catchMessage(Runnable action) {
        try {
            action.run();
            throw new AssertionError("se esperaba InvalidCredentialsException");
        } catch (InvalidCredentialsException e) {
            return e.getMessage();
        }
    }

    @Test
    @DisplayName("El mensaje de error no filtra el email ni el username")
    void errorMessageLeaksNothing() {
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("diego@example.com", "x")))
                .isInstanceOf(InvalidCredentialsException.class)
                .extracting(Throwable::getMessage)
                .satisfies(msg -> assertThat((String) msg)
                        .doesNotContain("diego@example.com")
                        .doesNotContain("diegoortiz"));
    }

    @Test
    @DisplayName("Una cuenta desactivada no puede entrar")
    void inactiveAccountCannotLogin() {
        User banned = existingUser();
        banned.setActive(false);
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("diegoortiz", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Un usuario de Google no puede entrar por login local")
    void googleUserCannotUseLocalLogin() {
        User googleUser = User.builder()
                .id(2L).username("googleboy").email("g@gmail.com")
                .password(null)                       // sin clave local
                .role(Role.PLAYER)
                .authProvider(AuthProvider.GOOGLE)
                .googleId("google-sub-123")
                .active(true)
                .build();
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.login(
                new LoginRequestDTO("googleboy", "loquesea")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ===== /me =====

    @Test
    @DisplayName("getUserById devuelve el perfil del usuario")
    void getUserByIdReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));

        assertThat(authService.getUserById(1L).getUsername()).isEqualTo("diegoortiz");
    }

    @Test
    @DisplayName("getUserById con id inexistente lanza excepcion")
    void getUserByIdThrowsWhenMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(999L))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}