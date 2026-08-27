package com.dataagent.platform.modules.auth.domain.model;

public record AuthSession(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        AuthUser user
) {
}
