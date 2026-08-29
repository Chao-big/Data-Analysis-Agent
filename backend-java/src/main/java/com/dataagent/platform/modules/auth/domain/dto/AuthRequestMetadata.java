package com.dataagent.platform.modules.auth.domain.dto;

import java.time.Instant;

public record AuthRequestMetadata(
        String clientPublicIp,
        String userAgent,
        Instant clientLastActivityAt
) {
}
