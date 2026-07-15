package com.malvinas.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.malvinas.auth.config.JwtProperties;
import com.malvinas.auth.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-jwt-service-unit-test";

    private JwtService newService() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenExpiration(3_600_000L);
        props.setRefreshTokenExpiration(604_800_000L);
        return new JwtService(props);
    }

    @Test
    void generateAccessToken_validatesCorrectly() {
        JwtService svc = newService();
        String token = svc.generateAccessToken("42", "ADM", "Admin User");

        assertThat(svc.isTokenValid(token)).isTrue();

        DecodedJWT decoded = svc.validateToken(token);
        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaim("role").asString()).isEqualTo("ADM");
        assertThat(decoded.getClaim("name").asString()).isEqualTo("Admin User");
    }

    @Test
    void tampered_token_isInvalid() {
        JwtService svc = newService();
        String token = svc.generateAccessToken("1", "SUP", "Supervisor");
        assertThat(svc.isTokenValid(token + "tampered")).isFalse();
    }
}
