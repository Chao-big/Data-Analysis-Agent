package com.dataagent.platform.modules.auth.domain.vo;

import java.time.LocalDateTime;

public record AuthLoginLogVO(
        Long id,
        Long userId,
        String username,
        String loginResult,
        String failureReason,
        String clientIp,
        String userAgent,
        LocalDateTime loginAt
) {
}
