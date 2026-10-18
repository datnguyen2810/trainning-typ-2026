package vn.xuandat.backend.mapper;

import org.springframework.stereotype.Component;
import vn.xuandat.backend.dto.response.UserResponse;
import vn.xuandat.backend.entity.User;

@Component
public class UserMapper {
    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
