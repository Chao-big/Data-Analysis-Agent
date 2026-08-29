package com.dataagent.platform.modules.auth.domain.model;

import java.time.Instant;

public record RefreshTokenSessionState(
        Instant expiresAt,
        Instant lastActivityAt
) {
}
