package com.dataagent.platform.modules.profile.domain.dto;

public record ProfileUpdateRequest(
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        Short gender
) {
}
