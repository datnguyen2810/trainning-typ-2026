package vn.xuandat.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    private final int status;
    private final String message;
    private final String errorCode;
    private final String path;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;
}
