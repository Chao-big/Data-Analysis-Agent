package com.dataagent.platform.common.web;

public record ApiResponse<T>(boolean success, int code, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, ApiStatusCode.SUCCESS.message());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, ApiStatusCode.SUCCESS.code(), data, message);
    }

    public static <T> ApiResponse<T> fail(ApiStatusCode statusCode) {
        return fail(statusCode, statusCode.message());
    }

    public static <T> ApiResponse<T> fail(ApiStatusCode statusCode, String message) {
        return new ApiResponse<>(false, statusCode.code(), null, message);
    }
}
