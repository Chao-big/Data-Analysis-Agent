package com.dataagent.platform.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        ApiStatusCode statusCode = exception.getStatusCode();
        return ResponseEntity.status(statusCode.httpStatus())
                .body(ApiResponse.fail(statusCode, exception.getMessage()));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        log.warn("Request rejected: {}", exception.getMessage());
        return ResponseEntity.status(ApiStatusCode.BAD_REQUEST.httpStatus())
                .body(ApiResponse.fail(ApiStatusCode.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(ApiStatusCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(ApiResponse.fail(ApiStatusCode.INTERNAL_SERVER_ERROR));
    }
}
