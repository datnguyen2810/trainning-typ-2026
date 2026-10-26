package vn.xuandat.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.secret());

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 32 decoded bytes"
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimValidator<List<String>> audienceValidator =
                new JwtClaimValidator<>(
                        "aud",
                        audiences -> audiences != null
                                && audiences.contains(properties.audience())
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(
                        JwtValidators.createDefaultWithIssuer(
                                properties.issuer()
                        ),
                        audienceValidator
                )
        );

        return decoder;
    }
}