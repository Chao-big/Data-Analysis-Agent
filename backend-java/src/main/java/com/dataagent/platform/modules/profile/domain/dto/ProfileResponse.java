package com.dataagent.platform.modules.profile.domain.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        Short gender,
        String status,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
