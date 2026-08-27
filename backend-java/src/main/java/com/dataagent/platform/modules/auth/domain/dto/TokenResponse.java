package com.dataagent.platform.modules.auth.domain.dto;

import java.util.Set;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String status,
        String tenantId,
        Set<String> roles
) {
}
