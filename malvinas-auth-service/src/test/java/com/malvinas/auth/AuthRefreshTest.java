package com.malvinas.auth;

import com.malvinas.auth.application.dto.LoginResponse;
import com.malvinas.auth.application.dto.RefreshRequest;
import com.malvinas.auth.config.JwtProperties;
import com.malvinas.auth.domain.service.impl.AuthServiceImpl;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient.EmployeeAuthInfo;
import com.malvinas.auth.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regresion: el refresh emitia el access token con rol vacio y el usuario quedaba en 403. */
class AuthRefreshTest {

    private final PersonalServiceClient personal = mock(PersonalServiceClient.class);
    private final JwtService jwt = newJwt();
    private final AuthServiceImpl auth = new AuthServiceImpl(personal, jwt);

    private static JwtService newJwt() {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-auth-refresh");
        p.setAccessTokenExpiration(3_600_000L);
        p.setRefreshTokenExpiration(604_800_000L);
        return new JwtService(p);
    }

    @Test
    void refresh_keepsRoleAndName() {
        when(personal.findById("5")).thenReturn(
                new EmployeeAuthInfo(5L, "00000011", "Ana", "Perez", "x", "SUP", true));

        LoginResponse r = auth.refresh(new RefreshRequest(jwt.generateRefreshToken("5")));

        var decoded = jwt.validateToken(r.accessToken());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("SUP");
        assertThat(decoded.getClaim("name").asString()).isEqualTo("Ana Perez");
        assertThat(r.employee().role()).isEqualTo("SUP");
    }

    @Test
    void refresh_inactiveAccount_isRejected() {
        when(personal.findById("5")).thenReturn(
                new EmployeeAuthInfo(5L, "00000011", "Ana", "Perez", "x", "SUP", false));

        assertThatThrownBy(() -> auth.refresh(new RefreshRequest(jwt.generateRefreshToken("5"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void refresh_withAccessToken_isRejected() {
        String access = jwt.generateAccessToken("5", "SUP", "Ana Perez");
        assertThatThrownBy(() -> auth.refresh(new RefreshRequest(access)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
