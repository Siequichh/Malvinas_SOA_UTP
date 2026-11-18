package com.malvinas.auth.domain.service.impl;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.malvinas.auth.application.dto.*;
import com.malvinas.auth.domain.service.AuthService;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient.EmployeeAuthInfo;
import com.malvinas.auth.infrastructure.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private final PersonalServiceClient personalClient;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(PersonalServiceClient personalClient, JwtService jwtService) {
        this.personalClient = personalClient;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        EmployeeAuthInfo emp = personalClient.findByDni(request.dni());
        if (emp == null || !Boolean.TRUE.equals(emp.isActive())
                || !passwordEncoder.matches(request.password(), emp.passwordHash())) {
            throw invalid();
        }
        return issueTokens(emp);
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        DecodedJWT decoded;
        try {
            decoded = jwtService.validateToken(request.refreshToken());
        } catch (JWTVerificationException e) {
            throw invalid();
        }
        if (!"refresh".equals(decoded.getClaim("type").asString())) throw invalid();
        // Se relee al empleado: el nuevo token lleva su rol/nombre ACTUAL (antes salia con rol vacio -> 403)
        EmployeeAuthInfo emp = personalClient.findById(decoded.getSubject());
        if (emp == null || !Boolean.TRUE.equals(emp.isActive())) throw invalid();
        return issueTokens(emp);
    }

    private LoginResponse issueTokens(EmployeeAuthInfo emp) {
        String id = String.valueOf(emp.id());
        String fullName = emp.firstName() + " " + emp.lastName();
        return new LoginResponse(
            jwtService.generateAccessToken(id, emp.roleCode(), fullName),
            jwtService.generateRefreshToken(id),
            "Bearer", 3600,
            new LoginResponse.EmployeeInfo(emp.id(), emp.dni(), fullName, emp.roleCode())
        );
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @Override
    public void logout(String token) {}

    @Override
    public ValidateResponse validate(String token) {
        try {
            DecodedJWT d = jwtService.validateToken(token);
            return new ValidateResponse(true, d.getSubject(),
                d.getClaim("role").asString(), d.getClaim("name").asString());
        } catch (Exception e) {
            return new ValidateResponse(false, null, null, null);
        }
    }
}
