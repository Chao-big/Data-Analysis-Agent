package com.dataagent.platform.modules.profile.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dataagent.platform.modules.auth.domain.po.AuthUserPO;
import com.dataagent.platform.modules.auth.mapper.AuthUserMapper;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final AuthUserMapper authUserMapper;

    @Override
    public Optional<ProfileResponse> findByUserId(String userId) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectById(numericUserId);
        return user == null ? Optional.empty() : Optional.of(toResponse(user));
    }

    @Override
    public Optional<ProfileResponse> updateByUserId(String userId, ProfileUpdateRequest request) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null || request == null) {
            return Optional.empty();
        }

        boolean hasChanges = request.nickname() != null
                || request.avatarUrl() != null
                || request.email() != null
                || request.phone() != null
                || request.gender() != null;

        if (!hasChanges) {
            return findByUserId(userId);
        }

        authUserMapper.update(null, new LambdaUpdateWrapper<AuthUserPO>()
                .eq(AuthUserPO::getId, numericUserId)
                .set(request.nickname() != null, AuthUserPO::getNickname, request.nickname())
                .set(request.avatarUrl() != null, AuthUserPO::getAvatarUrl, request.avatarUrl())
                .set(request.email() != null, AuthUserPO::getEmail, request.email())
                .set(request.phone() != null, AuthUserPO::getPhone, request.phone())
                .set(request.gender() != null, AuthUserPO::getGender, request.gender()));

        return findByUserId(userId);
    }

    private ProfileResponse toResponse(AuthUserPO user) {
        return new ProfileResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
