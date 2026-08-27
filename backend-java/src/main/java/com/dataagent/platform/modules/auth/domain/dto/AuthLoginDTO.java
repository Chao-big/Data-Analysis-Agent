package com.dataagent.platform.modules.auth.domain.dto;

public record AuthLoginDTO(
        String account,
        String password,
        String clientIp,
        String userAgent
) {
}
