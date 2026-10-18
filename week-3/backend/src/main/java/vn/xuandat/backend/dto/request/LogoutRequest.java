package vn.xuandat.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "Refresh token is required")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "Refresh token format is invalid"
    )
    private String refreshToken;
}

