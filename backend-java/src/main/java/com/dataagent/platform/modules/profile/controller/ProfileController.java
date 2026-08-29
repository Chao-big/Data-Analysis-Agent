package com.dataagent.platform.modules.profile.controller;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.web.ApiResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import com.dataagent.platform.modules.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> currentProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        log.debug("Received current profile request");
        return ApiResponse.ok(profileService.getCurrentProfile(principal));
    }

    @PutMapping("/me")
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody ProfileUpdateRequest request
    ) {
        log.debug("Received profile update request");
        return ApiResponse.ok(profileService.updateCurrentProfile(principal, request));
    }
}
