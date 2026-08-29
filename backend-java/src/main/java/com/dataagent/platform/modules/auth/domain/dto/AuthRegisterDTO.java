package com.dataagent.platform.modules.auth.domain.dto;

public record AuthRegisterDTO(
        String username,
        String password,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        Short gender,
        String remark
) {
}
