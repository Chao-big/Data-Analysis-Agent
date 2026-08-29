package com.dataagent.platform.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

public interface AuthRefreshCookieService {

    String resolveRefreshToken(HttpServletRequest request);

    void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken, Duration maxAge);

    void clearRefreshTokenCookie(HttpServletResponse response);
}
