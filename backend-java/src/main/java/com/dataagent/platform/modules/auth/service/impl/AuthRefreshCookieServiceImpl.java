package com.dataagent.platform.modules.auth.service.impl;

import com.dataagent.platform.modules.auth.config.AuthSessionProperties;
import com.dataagent.platform.modules.auth.service.AuthRefreshCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthRefreshCookieServiceImpl implements AuthRefreshCookieService {

    private final AuthSessionProperties authSessionProperties;

    @Override
    public String resolveRefreshToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> authSessionProperties.getRefreshTokenCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken, Duration maxAge) {
        if (response == null || refreshToken == null || refreshToken.isBlank() || maxAge == null || maxAge.isNegative()) {
            return;
        }

        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(
                        authSessionProperties.getRefreshTokenCookieName(),
                        refreshToken
                )
                .httpOnly(true)
                .secure(authSessionProperties.isRefreshTokenCookieSecure())
                .sameSite(authSessionProperties.getRefreshTokenCookieSameSite())
                .path(authSessionProperties.getRefreshTokenCookiePath())
                .maxAge(maxAge)
                .build()
                .toString());
    }

    @Override
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(
                        authSessionProperties.getRefreshTokenCookieName(),
                        ""
                )
                .httpOnly(true)
                .secure(authSessionProperties.isRefreshTokenCookieSecure())
                .sameSite(authSessionProperties.getRefreshTokenCookieSameSite())
                .path(authSessionProperties.getRefreshTokenCookiePath())
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }
}
