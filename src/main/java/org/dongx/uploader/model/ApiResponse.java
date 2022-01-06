package org.dongx.uploader.model;

import lombok.Data;

import java.util.Optional;

/**
 * ApiResponse
 *
 * @author <a href="mailto:dongxiang886@gmail.com">Dongx</a>
 * @since 1.0.0
 */
@Data
public class ApiResponse<T> {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private Integer code;

    private String message;

    private T data;

    private ApiResponse() {

    }

    private ApiResponse(Boolean success, Integer code) {
        this.success = success;
        this.code = code;
    }

    private ApiResponse(Boolean success, Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    private ApiResponse(Boolean success, Integer code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    private ApiResponse(Boolean success, Integer code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, 0, "success", null);
    }

    public static <T> ApiResponse<T> success(Integer code) {
        return new ApiResponse<>(true, code, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 0, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 0, message, data);
    }

    public static <T> ApiResponse<T> failure(Integer code, String message) {
        return new ApiResponse<>(false, code, message);
    }

    public static <T> ApiResponse<T> failure(Integer code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }

    public Boolean isSuccess() {
        return this.code == 0;
    }

    public Optional<T> optional() {
        return isSuccess() ? Optional.of(data) : Optional.empty();
    }
}
