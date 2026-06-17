package com.exchange.platform.common.core.result;
import java.time.LocalDateTime;

public record ApiResponse<T>(int code, String message, T data, String requestId, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "Success", data, requestId, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId, LocalDateTime.now());
    }

} 
