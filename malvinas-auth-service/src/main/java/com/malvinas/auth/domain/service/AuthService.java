package com.malvinas.auth.domain.service;

import com.malvinas.auth.application.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshRequest request);
    void logout(String token);
    ValidateResponse validate(String token);
}
