package com.example.student.security;

import com.example.student.model.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    private UserDetails userDetails(String username) {
        return User.withUsername(username).password("pass").authorities("ROLE_ADMIN").build();
    }

    @Test
    void generateToken_shouldContainUsername() {
        String token = jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }

    @Test
    void validateToken_whenUsernameMatches_shouldReturnTrue() {
        String token = jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN));

        assertThat(jwtService.validateToken(token, userDetails("admin@example.com"))).isTrue();
    }

    @Test
    void extractUsername_whenTokenIsInvalid_shouldThrowException() {
        assertThrows(JwtException.class, () -> jwtService.extractUsername("not.a.valid.token"));
    }
}
