package com.dataagent.platform.modules.auth.domain.dto;

import java.util.Set;

public record CurrentUserResponse(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String status,
        String tenantId,
        Set<String> roles
) {
}
