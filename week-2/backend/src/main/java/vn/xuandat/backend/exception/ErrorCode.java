package vn.xuandat.backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CATEGORY_SLUG_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Category slug already exists"
    ),
    CATEGORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Category not found"
    ),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Request body is invalid"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;
}
