package com.dataagent.platform.modules.profile.service.impl;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.web.ApiException;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.repository.AuthRepository;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import com.dataagent.platform.modules.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    void updateCurrentProfileShouldReturnCurrentProfileWhenAllFieldsAreNull() {
        ProfileResponse currentProfile = profileResponse();
        when(profileRepository.findByUserId("1")).thenReturn(Optional.of(currentProfile));

        ProfileResponse response = profileService.updateCurrentProfile(principal(), new ProfileUpdateRequest(
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(response).isEqualTo(currentProfile);
        verify(profileRepository, never()).updateByUserId(any(), any());
        verify(authRepository, never()).findByEmail(any());
        verify(authRepository, never()).findByPhone(any());
    }

    @Test
    void updateCurrentProfileShouldValidateAndUpdateOnlyChangedFields() {
        ProfileResponse currentProfile = profileResponse();
        ProfileResponse updatedProfile = new ProfileResponse(
                "1",
                "analyst01",
                "Analyst A",
                "https://static.local/avatar/a.png",
                "new@example.com",
                "13800000001",
                (short) 1,
                "ACTIVE",
                currentProfile.lastLoginAt(),
                currentProfile.lastLoginIp(),
                currentProfile.createdAt(),
                LocalDateTime.of(2026, 8, 29, 16, 30)
        );

        when(profileRepository.findByUserId("1")).thenReturn(Optional.of(currentProfile));
        when(authRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(profileRepository.updateByUserId(eq("1"), any(ProfileUpdateRequest.class))).thenReturn(Optional.of(updatedProfile));

        ProfileResponse response = profileService.updateCurrentProfile(principal(), new ProfileUpdateRequest(
                null,
                null,
                "new@example.com",
                null,
                null
        ));

        assertThat(response.email()).isEqualTo("new@example.com");
        verify(authRepository).findByEmail("new@example.com");
        verify(authRepository, never()).findByPhone(any());
        verify(profileRepository).updateByUserId(eq("1"), eq(new ProfileUpdateRequest(
                null,
                null,
                "new@example.com",
                null,
                null
        )));
    }

    @Test
    void updateCurrentProfileShouldRejectInvalidChangedEmail() {
        when(profileRepository.findByUserId("1")).thenReturn(Optional.of(profileResponse()));

        assertThatThrownBy(() -> profileService.updateCurrentProfile(principal(), new ProfileUpdateRequest(
                null,
                null,
                "invalid-email",
                null,
                null
        )))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("请输入有效邮箱地址");

        verify(profileRepository, never()).updateByUserId(any(), any());
        verify(authRepository, never()).findByEmail(any());
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(
                "access-token",
                "1",
                "analyst01",
                "Analyst A",
                "https://static.local/avatar/a.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }

    private ProfileResponse profileResponse() {
        return new ProfileResponse(
                "1",
                "analyst01",
                "Analyst A",
                "https://static.local/avatar/a.png",
                "analyst@example.com",
                "13800000001",
                (short) 1,
                "ACTIVE",
                LocalDateTime.of(2026, 8, 29, 10, 0),
                "203.0.113.10",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 29, 10, 5)
        );
    }
}
