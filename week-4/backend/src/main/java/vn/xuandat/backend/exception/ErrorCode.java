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
    CATEGORY_IN_USE(
            HttpStatus.CONFLICT,
            "Category cannot be deleted because it contains products"
    ),
    PRODUCT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product SKU or slug already exists"
    ),
    PRODUCT_SKU_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product sku already exists"
    ),
    PRODUCT_SLUG_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Product slug already exists"
    ),
    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Product not found"
    ),
    PRODUCT_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "Product has been modified by another request"
    ),
    INVALID_PRICE_RANGE(
            HttpStatus.BAD_REQUEST,
            "Minimum price must not be greater than maximum price"
    ),
    INVALID_SORT_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "Sort parameter is invalid"
    ),
    USERNAME_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Username already exists"
    ),
    USER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Username or email already exists"
    ),
    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Email already exists"
    ),
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password"
    ),
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required or token is invalid"
    ),
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource"
    ),
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "User not found"
    ),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid or expired"
    ),


    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Request body is invalid"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;
}
