package com.dataagent.platform.modules.auth.domain.dto;

public record AuthRequestMetadata(
        String clientPublicIp,
        String userAgent
) {
}
