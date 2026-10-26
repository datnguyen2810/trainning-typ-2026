package vn.xuandat.backend.service;

import jakarta.validation.Valid;
import vn.xuandat.backend.dto.request.LoginRequest;
import vn.xuandat.backend.dto.request.LogoutRequest;
import vn.xuandat.backend.dto.request.RefreshTokenRequest;
import vn.xuandat.backend.dto.request.RegisterRequest;
import vn.xuandat.backend.dto.response.LoginResponse;
import vn.xuandat.backend.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    LoginResponse login(@Valid LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(UUID userId, LogoutRequest request);
}

