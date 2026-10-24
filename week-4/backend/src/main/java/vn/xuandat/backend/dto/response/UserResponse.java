package vn.xuandat.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import vn.xuandat.backend.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private UserStatus status;
    private Instant createdAt;
}