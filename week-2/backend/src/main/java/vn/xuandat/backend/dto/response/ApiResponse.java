package vn.xuandat.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({ "status", "message", "data" }) // Ép Jackson sắp xếp theo đúng thứ tự này
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;

    // Hàm tạo tổng quát đầy đủ tham số
    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }

    public static <T> ApiResponse<T> message(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message, null);
    }

    // Trường hợp thành công mặc định (Chỉ cần truyền data)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "Success", data);
    }

    // Trường hợp thành công có kèm message tùy biến
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }
}
