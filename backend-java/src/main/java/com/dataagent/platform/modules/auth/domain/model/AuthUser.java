package com.dataagent.platform.modules.auth.domain.model;

import java.util.Set;

public record AuthUser(
        String userId,
        String username,
        String passwordHash,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        String gender,
        String status,
        String tenantId,
        Set<String> roles,
        Set<String> allowedDatasets,
        Set<String> maskedColumns
) {
}
