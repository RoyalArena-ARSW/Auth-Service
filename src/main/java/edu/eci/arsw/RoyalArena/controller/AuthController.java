package edu.eci.arsw.RoyalArena.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.arsw.RoyalArena.dto.request.LoginRequestDTO;
import edu.eci.arsw.RoyalArena.dto.request.RegisterRequestDTO;
import edu.eci.arsw.RoyalArena.dto.response.AuthResponseDTO;
import edu.eci.arsw.RoyalArena.dto.response.UserResponseDTO;
import edu.eci.arsw.RoyalArena.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registro de usuario nuevo. Ruta pública (el Gateway no le aplica AuthFilter).
     * Devuelve un token para auto-login inmediato.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("POST /api/auth/register - username: {}", request.getUsername());
        AuthResponseDTO response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login. Ruta pública. El identifier puede ser email o username.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("POST /api/auth/login - identifier: {}", request.getIdentifier());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve la info del usuario autenticado. Ruta protegida.
     * El Gateway valida el JWT e inyecta X-User-Id, que leemos aquí.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/auth/me - userId: {}", userId);
        return ResponseEntity.ok(authService.getUserById(userId));
    }
}