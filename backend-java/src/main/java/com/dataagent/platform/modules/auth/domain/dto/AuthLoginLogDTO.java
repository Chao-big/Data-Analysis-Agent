package com.dataagent.platform.modules.auth.domain.dto;

import java.time.LocalDateTime;

public record AuthLoginLogDTO(
        Long id,
        Long userId,
        String username,
        String clientPublicIp,
        String userAgent,
        LocalDateTime loginAt
) {
}
