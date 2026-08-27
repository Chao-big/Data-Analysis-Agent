package com.dataagent.platform.modules.auth.domain.vo;

import java.time.Duration;
import java.time.LocalDateTime;

public record AuthLoginVO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Duration accessTokenExpire,
        Duration refreshTokenExpire,
        LocalDateTime issuedAt,
        AuthUserVO user
) {
}
