package com.dataagent.platform.common.web;

public class ApiException extends RuntimeException {

    private final ApiStatusCode statusCode;

    public ApiException(ApiStatusCode statusCode) {
        super(statusCode.message());
        this.statusCode = statusCode;
    }

    public ApiException(ApiStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiStatusCode getStatusCode() {
        return statusCode;
    }
}
