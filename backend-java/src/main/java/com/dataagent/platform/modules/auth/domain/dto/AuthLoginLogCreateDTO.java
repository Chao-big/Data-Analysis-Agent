package com.dataagent.platform.modules.auth.domain.dto;

import java.time.LocalDateTime;

public record AuthLoginLogCreateDTO(
        Long userId,
        String username,
        String loginResult,
        String failureReason,
        String clientIp,
        String userAgent,
        LocalDateTime loginAt
) {
}
