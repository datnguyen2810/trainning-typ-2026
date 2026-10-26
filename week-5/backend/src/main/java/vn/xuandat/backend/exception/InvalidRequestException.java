package vn.xuandat.backend.exception;

public class InvalidRequestException extends RuntimeException {

    private final ErrorCode errorCode;

    public InvalidRequestException(
            ErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
