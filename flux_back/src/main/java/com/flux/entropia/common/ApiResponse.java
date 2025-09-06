package com.flux.entropia.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified API response structure.
 * @param <T> The type of the data payload.
 */
@Data
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * Creates a success response with a data payload.
     * @param data the data to include in the response.
     * @return a success ApiResponse.
     * @param <T> the type of the data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    /**
     * Creates a success response with no data payload.
     * @return a success ApiResponse.
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "Operation successful", null);
    }

    /**
     * Creates an error response with a message.
     * @param message the error message.
     * @return an error ApiResponse.
     * @param <T> the type of the data (will be null).
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
