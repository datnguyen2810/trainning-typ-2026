package vn.xuandat.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[a-z0-9_.]{3,50}$",
            message = "Username format is incorrect"
    )
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public void setUsername(String username) {
        this.username = username == null
                ? null
                : username.trim().toLowerCase(Locale.ROOT);
    }
}