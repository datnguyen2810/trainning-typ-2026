package vn.xuandat.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import vn.xuandat.backend.dto.response.ErrorResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> handleConflict(ConflictException exception, HttpServletRequest request) {
        return buildResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return buildResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return buildResponse(
                errorCode,
                errorCode.getDefaultMessage(),
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableRequestBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        return buildResponse(
                errorCode,
                errorCode.getDefaultMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while processing {}", request.getRequestURI(), exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return buildResponse(
                errorCode,
                errorCode.getDefaultMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getParameterValidationResults().forEach(result -> {
            String parameterName =
                    result.getMethodParameter().getParameterName();

            result.getResolvableErrors().forEach(error ->
                    fieldErrors.putIfAbsent(
                            parameterName,
                            error.getDefaultMessage()
                    )
            );
        });

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        return buildResponse(
                errorCode,
                errorCode.getDefaultMessage(),
                request,
                fieldErrors
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(message)
                .errorCode(errorCode.name())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
