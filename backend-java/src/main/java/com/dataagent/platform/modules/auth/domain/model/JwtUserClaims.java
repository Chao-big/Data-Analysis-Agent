package com.dataagent.platform.modules.auth.domain.model;

import java.time.Instant;
import java.util.Set;

public record JwtUserClaims(
        String tokenId,
        String tokenType,
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String status,
        String tenantId,
        Set<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}
