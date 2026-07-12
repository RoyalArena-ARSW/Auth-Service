package edu.eci.arsw.RoyalArena.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.arsw.RoyalArena.dto.request.LoginRequestDTO;
import edu.eci.arsw.RoyalArena.dto.request.RegisterRequestDTO;
import edu.eci.arsw.RoyalArena.dto.response.AuthResponseDTO;
import edu.eci.arsw.RoyalArena.dto.response.UserResponseDTO;
import edu.eci.arsw.RoyalArena.exception.InvalidCredentialsException;
import edu.eci.arsw.RoyalArena.exception.UserAlreadyExistsException;
import edu.eci.arsw.RoyalArena.mappers.UserMapper;
import edu.eci.arsw.RoyalArena.model.enums.AuthProvider;
import edu.eci.arsw.RoyalArena.model.enums.Role;
import edu.eci.arsw.RoyalArena.model.enums.User;
import edu.eci.arsw.RoyalArena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /**
     * Registra un usuario nuevo. Valida unicidad de email y username,
     * hashea la contraseña con BCrypt, guarda, y devuelve un token listo
     * para usar (auto-login después de registrar).
     */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Register attempt for username: {}", request.getUsername());

        // 1. Validar que el email no esté en uso.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                "Email '" + request.getEmail() + "' is already registered");
        }

        // 2. Validar que el username no esté en uso.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                "Username '" + request.getUsername() + "' is already taken");
        }

        // 3. Construir el usuario. La contraseña se hashea con BCrypt AQUÍ,
        //    nunca se guarda en texto plano.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PLAYER)                 // Todo registro nuevo es PLAYER
                .authProvider(AuthProvider.LOCAL)  // Registro tradicional
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully: {} (id={})", saved.getUsername(), saved.getId());

        // 4. Generar token y devolver respuesta (auto-login).
        return buildAuthResponse(saved);
    }

    /**
     * Autentica un usuario. El identifier puede ser email o username.
     * Si las credenciales son válidas, devuelve un token nuevo.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for identifier: {}", request.getIdentifier());

        // 1. Buscar por email O username (login flexible).
        User user = userRepository
                .findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for {}", request.getIdentifier());
                    // Mensaje genérico a propósito (evita user enumeration)
                    return new InvalidCredentialsException("Invalid credentials");
                });

        // 2. Verificar que la cuenta esté activa.
        if (Boolean.FALSE.equals(user.getActive())) {
            log.warn("Login failed: account disabled for {}", request.getIdentifier());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // 3. Verificar que sea un usuario LOCAL (no de Google).
        //    Un usuario de Google no tiene password, así que no puede hacer login normal.
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPassword() == null) {
            log.warn("Login failed: user {} registered via {}, not LOCAL",
                    request.getIdentifier(), user.getAuthProvider());
            throw new InvalidCredentialsException(
                "This account uses a different sign-in method");
        }

        // 4. Comparar la contraseña con el hash almacenado.
        //    passwordEncoder.matches hashea la contraseña ingresada y la compara
        //    con el hash guardado. Nunca desencriptamos (BCrypt es one-way).
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password for {}", request.getIdentifier());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        log.info("Login successful for user {} (id={})", user.getUsername(), user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Devuelve la info del usuario a partir de su ID.
     * Se usa en GET /api/auth/me, donde el Gateway ya validó el token
     * e inyectó el X-User-Id.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        return userMapper.toDto(user);
    }

    /**
     * Helper: construye el AuthResponseDTO con token + info del usuario.
     */
    private AuthResponseDTO buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(userMapper.toDto(user))
                .build();
    }
}