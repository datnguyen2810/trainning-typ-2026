package vn.xuandat.backend.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration accessTokenTtl,
        @NotBlank String secret,
        @NotNull Duration refreshTokenTtl
) {

    public JwtProperties {
        if (accessTokenTtl != null
                && (accessTokenTtl.isNegative() || accessTokenTtl.isZero())) {
            throw new IllegalArgumentException(
                    "JWT access token TTL must be positive"
            );
        }

        if (refreshTokenTtl != null
                && (refreshTokenTtl.isNegative() || refreshTokenTtl.isZero())) {
            throw new IllegalArgumentException(
                    "JWT refresh token TTL must be positive"
            );
        }
    }
}