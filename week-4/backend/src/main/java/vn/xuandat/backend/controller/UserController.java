package vn.xuandat.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.xuandat.backend.dto.response.ApiResponse;
import vn.xuandat.backend.dto.response.UserResponse;
import vn.xuandat.backend.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // @AuthenticationPrincipal Jwt jwt lấy token đã được Spring Security xác thực, không phải tự parse token từ request
    // lấy sub từ JWT → tìm đúng User đó → trả UserResponse
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        UserResponse user = userService.getCurrentUser(userId);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "User retrieved successfully", user));
    }


}
