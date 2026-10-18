package vn.xuandat.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.xuandat.backend.dto.request.LoginRequest;
import vn.xuandat.backend.dto.request.LogoutRequest;
import vn.xuandat.backend.dto.request.RefreshTokenRequest;
import vn.xuandat.backend.dto.request.RegisterRequest;
import vn.xuandat.backend.dto.response.LoginResponse;
import vn.xuandat.backend.dto.response.UserResponse;
import vn.xuandat.backend.entity.Role;
import vn.xuandat.backend.entity.User;
import vn.xuandat.backend.enums.RoleCode;
import vn.xuandat.backend.enums.UserStatus;
import vn.xuandat.backend.exception.ConflictException;
import vn.xuandat.backend.exception.ErrorCode;
import vn.xuandat.backend.mapper.UserMapper;
import vn.xuandat.backend.repository.RoleRepository;
import vn.xuandat.backend.repository.UserRepository;
import vn.xuandat.backend.security.JwtProperties;
import vn.xuandat.backend.security.JwtService;
import vn.xuandat.backend.service.AuthService;
import vn.xuandat.backend.service.RefreshTokenService;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException(
                    ErrorCode.USERNAME_ALREADY_EXISTS,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email already exists"
            );
        }

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseThrow(() ->
                        new IllegalStateException("Required role CUSTOMER is missing")
                );

        String rawPassword = request.getPassword();
        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .build();

        user.addRole(customerRole);

        try {
            userRepository.saveAndFlush(user);
            return userMapper.toUserResponse(user);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof java.sql.SQLException sql
                    && "23505".equals(sql.getSQLState())) {
                throw new ConflictException(
                        ErrorCode.USER_ALREADY_EXISTS,
                        "Username or email already exists",
                        exception
                );
            }

            throw exception;
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatches || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createForLogin(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.accessTokenTtl().toSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        return refreshTokenService.refresh(request.getRefreshToken());
    }

    @Override
    public void logout(UUID userId, LogoutRequest request) {
        refreshTokenService.logout(userId, request.getRefreshToken());
    }


}

