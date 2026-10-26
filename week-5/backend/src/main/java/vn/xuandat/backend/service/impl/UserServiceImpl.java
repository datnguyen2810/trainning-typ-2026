package vn.xuandat.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import vn.xuandat.backend.dto.response.UserResponse;
import vn.xuandat.backend.entity.User;
import vn.xuandat.backend.enums.UserStatus;
import vn.xuandat.backend.exception.ErrorCode;
import vn.xuandat.backend.exception.ResourceNotFoundException;
import vn.xuandat.backend.mapper.UserMapper;
import vn.xuandat.backend.repository.UserRepository;
import vn.xuandat.backend.service.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is not active");
        }

        return userMapper.toUserResponse(user);
    }


}
