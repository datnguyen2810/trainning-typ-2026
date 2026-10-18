package vn.xuandat.backend.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import vn.xuandat.backend.dto.response.LoginResponse;
import vn.xuandat.backend.entity.User;

import java.util.UUID;

public interface RefreshTokenService {

    String createForLogin(User user);

    LoginResponse refresh(@NotBlank(message = "Refresh token is required") @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "Refresh token format is invalid"
    ) String refreshToken);

    void logout(UUID userId, @NotBlank(message = "Refresh token is required") @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "Refresh token format is invalid"
    ) String refreshToken);
}
