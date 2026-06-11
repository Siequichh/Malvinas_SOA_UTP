package com.malvinas.auth.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.malvinas.auth.config.JwtProperties;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;

    public JwtService(JwtProperties props) { this.props = props; }

    public String generateAccessToken(String employeeId, String role, String name) {
        return JWT.create()
                .withSubject(employeeId)
                .withClaim("role", role)
                .withClaim("name", name)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + props.getAccessTokenExpiration()))
                .sign(Algorithm.HMAC256(props.getSecret()));
    }

    public String generateRefreshToken(String employeeId) {
        return JWT.create()
                .withSubject(employeeId)
                .withClaim("type", "refresh")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + props.getRefreshTokenExpiration()))
                .sign(Algorithm.HMAC256(props.getSecret()));
    }

    public DecodedJWT validateToken(String token) {
        return JWT.require(Algorithm.HMAC256(props.getSecret())).build().verify(token);
    }

    public boolean isTokenValid(String token) {
        try { validateToken(token); return true; }
        catch (JWTVerificationException e) { return false; }
    }
}
