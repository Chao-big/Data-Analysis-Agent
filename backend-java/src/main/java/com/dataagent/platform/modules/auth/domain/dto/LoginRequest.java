package com.dataagent.platform.modules.auth.domain.dto;

public record LoginRequest(
        String username,
        String password
) {
}
