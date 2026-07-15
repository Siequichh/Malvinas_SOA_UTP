package com.malvinas.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.malvinas.gateway.config.JwtProperties;
import com.malvinas.gateway.filter.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-jwt-filter";

    private JwtAuthFilter filter() {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        return new JwtAuthFilter(p);
    }

    private String token(String employeeId, String role) {
        return JWT.create()
                .withSubject(employeeId)
                .withClaim("role", role)
                .withClaim("name", "Test")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    @Test
    void validToken_passesAndInjectsHeaders() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/dispatches");
        req.addHeader("Authorization", "Bearer " + token("42", "ADM"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(((HttpServletRequest) chain.getRequest()).getHeader("X-Employee-Id")).isEqualTo("42");
        assertThat(((HttpServletRequest) chain.getRequest()).getHeader("X-Employee-Role")).isEqualTo("ADM");
    }

    @Test
    void noToken_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/dispatches");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter().doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void authPath_exempt_noTokenNeeded() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRequestURI("/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
