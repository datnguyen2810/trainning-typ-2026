package vn.xuandat.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.xuandat.backend.dto.response.LoginResponse;
import vn.xuandat.backend.entity.RefreshToken;
import vn.xuandat.backend.entity.User;
import vn.xuandat.backend.enums.UserStatus;
import vn.xuandat.backend.exception.InvalidRefreshTokenException;
import vn.xuandat.backend.mapper.UserMapper;
import vn.xuandat.backend.repository.RefreshTokenRepository;
import vn.xuandat.backend.repository.UserRepository;
import vn.xuandat.backend.security.JwtProperties;
import vn.xuandat.backend.security.JwtService;
import vn.xuandat.backend.service.RefreshTokenService;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    // Raw token chỉ trả về client. Database lưu kết quả hashToken(rawToken)
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional
    public String createForLogin(User user) {
        Instant now = Instant.now();
        String rawToken = generateRawToken();

        RefreshToken token = RefreshToken.builder()
                .tokenHash(hashToken(rawToken))
                .familyId(UUID.randomUUID())
                .expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
                .createdAt(now)
                .user(user)
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginResponse refresh(String rawToken) {
        String tokenHash = hashToken(rawToken);

        // Chỉ lấy UUID trước khi lock, chưa load entity RefreshToken
        UUID userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        // Serialize refresh/logout của cùng User
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        // Đọc token sau khi đã có lock
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        if (oldToken.isRevoked()) {
            // Token đã rotate mà vẫn bị sử dụng lại: revoke cả family
            if (oldToken.getReplacedByTokenId() != null) {
                refreshTokenRepository.revokeFamily(oldToken.getFamilyId(), now);
            }

            throw new InvalidRefreshTokenException();
        }

        if (oldToken.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeFamily(oldToken.getFamilyId(), now);

            throw new InvalidRefreshTokenException();
        }

        String newRawToken = generateRawToken();

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(newRawToken))
                .familyId(oldToken.getFamilyId())
                .createdAt(now)
                // Giữ thời hạn tuyệt đối của family
                .expiresAt(oldToken.getExpiresAt())
                .build();

        // Lưu token mới trước để có ID cho replacement
        RefreshToken savedToken = refreshTokenRepository.saveAndFlush(newToken);

        // oldToken đang managed, Hibernate tự update khi commit
        oldToken.markReplaced(savedToken.getId(), now);

        String accessToken = jwtService.generateAccessToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.accessTokenTtl().toSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(UUID currentUserId, String rawRefreshToken) {
        // Dùng cùng lock với refresh để tránh logout/refresh chạy chéo
        if (userRepository.findByIdForUpdate(currentUserId).isEmpty()) {
            return;
        }

        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);

        // Không tiết lộ token có tồn tại hoặc thuộc ai
        if (token == null || !token.getUser().getId().equals(currentUserId)) {
            return;
        }

        refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
    }

}
