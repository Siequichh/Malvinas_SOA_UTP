package com.malvinas.auth.infrastructure.controller;

import com.malvinas.auth.application.dto.*;
import com.malvinas.auth.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT authentication operations")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate employee and obtain JWT tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String header) {
        authService.logout(header.startsWith("Bearer ") ? header.substring(7) : header);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token — consumed by API Gateway")
    public ResponseEntity<ValidateResponse> validate(@RequestParam String token) {
        return ResponseEntity.ok(authService.validate(token));
    }
}
