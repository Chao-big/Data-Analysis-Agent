package com.dataagent.platform.modules.auth.domain.dto;

import java.time.LocalDateTime;

public record AuthUserDTO(
        Long id,
        String username,
        String passwordHash,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        Short gender,
        String status,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
