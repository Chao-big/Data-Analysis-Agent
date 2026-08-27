package com.dataagent.platform.common.web;

import org.springframework.http.HttpStatus;

public enum ApiStatusCode {
    SUCCESS(0, HttpStatus.OK, "ok"),
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "bad request"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "unauthorized"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "forbidden"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "resource not found"),
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ApiStatusCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
