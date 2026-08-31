package com.dataagent.platform.modules.auth.domain.dto;

public record AuthUserUpdateDTO(
        Long userId,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        Short gender,
        String status,
        String remark
) {
}
