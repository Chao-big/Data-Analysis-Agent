package com.dataagent.platform.modules.auth.domain.vo;

import java.time.LocalDateTime;

public record AuthUserVO(
        Long id,
        String username,
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
