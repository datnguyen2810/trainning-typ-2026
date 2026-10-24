package vn.xuandat.backend.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[a-z0-9_.]{3,50}$",
            message = "Username must contain 3–50 lowercase letters, numbers, underscores or dots"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email format is incorrect"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must contain 6 - 72 characters")
    @Pattern(
            regexp = "^[\\x20-\\x7E]+$",
            message = "Password must contain only ASCII letters, numbers, spaces or symbols"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    public void setUsername(String username) {
        this.username = username == null
                ? null
                : username.trim().toLowerCase(Locale.ROOT);
    }

    public void setEmail(String email) {
        this.email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

}

