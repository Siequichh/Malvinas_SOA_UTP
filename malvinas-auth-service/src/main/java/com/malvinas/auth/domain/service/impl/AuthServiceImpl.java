package com.malvinas.auth.domain.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.malvinas.auth.application.dto.*;
import com.malvinas.auth.domain.service.AuthService;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient;
import com.malvinas.auth.infrastructure.client.PersonalServiceClient.EmployeeAuthInfo;
import com.malvinas.auth.infrastructure.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
        if (emp == null || !Boolean.TRUE.equals(emp.isActive())) throw new RuntimeException("Invalid credentials");
        if (!passwordEncoder.matches(request.password(), emp.passwordHash()))
            throw new RuntimeException("Invalid credentials");
        String fullName = emp.firstName() + " " + emp.lastName();
        return new LoginResponse(
            jwtService.generateAccessToken(String.valueOf(emp.id()), emp.roleCode(), fullName),
            jwtService.generateRefreshToken(String.valueOf(emp.id())),
            "Bearer", 3600,
            new LoginResponse.EmployeeInfo(emp.id(), emp.dni(), fullName, emp.roleCode())
        );
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        DecodedJWT decoded = jwtService.validateToken(request.refreshToken());
        if (!"refresh".equals(decoded.getClaim("type").asString()))
            throw new RuntimeException("Invalid refresh token");
        String empId = decoded.getSubject();
        if (!personalClient.isEmployeeActive(empId)) throw new RuntimeException("Account inactive");
        return new LoginResponse(
            jwtService.generateAccessToken(empId, "", ""),
            jwtService.generateRefreshToken(empId),
            "Bearer", 3600, null
        );
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
