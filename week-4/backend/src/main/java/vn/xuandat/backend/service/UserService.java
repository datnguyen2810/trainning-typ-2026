package vn.xuandat.backend.service;

import vn.xuandat.backend.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);
}
