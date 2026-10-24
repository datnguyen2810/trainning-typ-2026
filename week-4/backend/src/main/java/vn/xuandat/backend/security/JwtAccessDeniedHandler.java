package vn.xuandat.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.xuandat.backend.dto.response.ErrorResponse;
import vn.xuandat.backend.exception.ErrorCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

// Trả JSON 403 khi đã xác thực nhưng thiếu quyền.
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getDefaultMessage())
                .errorCode(errorCode.name())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
