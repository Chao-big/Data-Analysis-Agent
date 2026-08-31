package com.dataagent.platform.modules.profile.service.impl;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.web.ApiException;
import com.dataagent.platform.common.web.ApiStatusCode;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.repository.AuthRepository;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import com.dataagent.platform.modules.profile.repository.ProfileRepository;
import com.dataagent.platform.modules.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final short GENDER_UNKNOWN = 0;
    private static final short GENDER_MALE = 1;
    private static final short GENDER_FEMALE = 2;
    private static final int NICKNAME_MAX_LENGTH = 64;
    private static final int EMAIL_MAX_LENGTH = 128;
    private static final int PHONE_MAX_LENGTH = 32;
    private static final int AVATAR_URL_MAX_LENGTH = 512;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final ProfileRepository profileRepository;
    private final AuthRepository authRepository;

    @Override
    public ProfileResponse getCurrentProfile(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiStatusCode.UNAUTHORIZED, "login required");
        }

        return profileRepository.findByUserId(principal.userId())
                .or(() -> authRepository.findByUserId(principal.userId()).map(this::toFallbackResponse))
                .orElseThrow(() -> new ApiException(ApiStatusCode.NOT_FOUND, "profile not found"));
    }

    @Override
    public ProfileResponse updateCurrentProfile(AuthenticatedUserPrincipal principal, ProfileUpdateRequest request) {
        if (principal == null) {
            throw new ApiException(ApiStatusCode.UNAUTHORIZED, "login required");
        }

        if (request == null) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "profile update request is required");
        }

        ProfileResponse currentProfile = getCurrentProfile(principal);

        String nickname = normalizeNullable(request.nickname());
        String avatarUrl = normalizeNullable(request.avatarUrl());
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());
        Short gender = normalizeGender(request.gender());

        validateNickname(nickname);
        validateAvatarUrl(avatarUrl);
        validateEmail(email);
        validatePhone(phone);
        validateOwnership(principal.userId(), email, phone);

        if (nickname == null && avatarUrl == null && email == null && phone == null && gender == null) {
            return currentProfile;
        }

        ProfileUpdateRequest normalizedRequest = new ProfileUpdateRequest(
                nickname,
                avatarUrl,
                email,
                phone,
                gender
        );

        return profileRepository.updateByUserId(principal.userId(), normalizedRequest)
                .orElseThrow(() -> new ApiException(ApiStatusCode.NOT_FOUND, "profile not found"));
    }

    private void validateNickname(String nickname) {
        if (nickname == null) {
            return;
        }

        if (nickname.isBlank()) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "昵称不能为空");
        }

        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "昵称长度不能超过 64 个字符");
        }
    }

    private void validateAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return;
        }

        if (avatarUrl.length() > AVATAR_URL_MAX_LENGTH) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "头像地址长度不能超过 512 个字符");
        }

        try {
            URI uri = new URI(avatarUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ApiException(ApiStatusCode.BAD_REQUEST, "头像地址只支持 http 或 https");
            }
        } catch (URISyntaxException exception) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "头像地址格式不正确");
        }
    }

    private void validateEmail(String email) {
        if (email == null) {
            return;
        }

        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "请输入有效邮箱地址");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null) {
            return;
        }

        if (phone.length() > PHONE_MAX_LENGTH || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "请输入有效手机号");
        }
    }

    private void validateOwnership(String currentUserId, String email, String phone) {
        if (email != null) {
            authRepository.findByEmail(email)
                    .filter(user -> !user.userId().equals(currentUserId))
                    .ifPresent(user -> {
                        throw new ApiException(ApiStatusCode.BAD_REQUEST, "邮箱已被其他账号使用");
                    });
        }

        if (phone != null) {
            authRepository.findByPhone(phone)
                    .filter(user -> !user.userId().equals(currentUserId))
                    .ifPresent(user -> {
                        throw new ApiException(ApiStatusCode.BAD_REQUEST, "手机号已被其他账号使用");
                    });
        }
    }

    private ProfileResponse toFallbackResponse(AuthUser user) {
        return new ProfileResponse(
                user.userId(),
                user.username(),
                user.nickname(),
                user.avatarUrl(),
                user.email(),
                user.phone(),
                user.gender(),
                user.status(),
                null,
                null,
                null,
                null
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Short normalizeGender(Short gender) {
        if (gender == null) {
            return null;
        }

        if (gender != GENDER_UNKNOWN && gender != GENDER_MALE && gender != GENDER_FEMALE) {
            throw new ApiException(ApiStatusCode.BAD_REQUEST, "gender 仅支持 0、1、2");
        }

        return gender;
    }
}
