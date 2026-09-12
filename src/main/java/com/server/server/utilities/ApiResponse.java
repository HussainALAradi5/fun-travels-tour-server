package com.server.server.utilities;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Map<String, String> fieldErrors;
    private Instant timestamp;
    private String path;

    public ApiResponse(boolean success, String message, T data) {
        this(success, success ? "SUCCESS" : "REQUEST_FAILED", message, data,
                Map.of(), Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success("SUCCESS", "Success", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return success("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return success("SUCCESS", message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("REQUEST_FAILED", message, Map.of(), null);
    }

    public static <T> ApiResponse<T> error(String code, String message,
            Map<String, String> fieldErrors, String path) {
        return new ApiResponse<>(false, code, message, null, fieldErrors, Instant.now(), path);
    }

    private static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, Map.of(), Instant.now(), null);
    }
}
