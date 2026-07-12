package edu.eci.arsw.RoyalArena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Bean de BCrypt para hashear y verificar contraseñas.
     * BCrypt es un algoritmo de hashing diseñado específicamente para passwords:
     * es lento a propósito (para dificultar ataques de fuerza bruta) y genera
     * un salt aleatorio por cada hash (dos usuarios con la misma contraseña
     * tendrán hashes distintos).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Desactiva la seguridad HTTP de Spring Security.
     * Este microservicio NO valida JWT ni bloquea endpoints — eso lo hace
     * el API Gateway. Aquí solo necesitamos Spring Security por el BCrypt.
     *
     * Sin esta config, Spring Security bloquearía todos los endpoints
     * pidiendo autenticación básica HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}