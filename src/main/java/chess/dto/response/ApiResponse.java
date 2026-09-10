package chess.dto.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        Integer errorCode,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, "Success", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, null, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(int errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null, Instant.now());
    }
}