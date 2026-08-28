package com.dataagent.platform.modules.auth.domain.dto;

public record AuthRegisterResponse(
        String userId,
        String username,
        String nickname
) {
}
