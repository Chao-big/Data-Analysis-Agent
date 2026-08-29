package com.dataagent.platform.modules.profile.repository;

import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;

import java.util.Optional;

public interface ProfileRepository {

    Optional<ProfileResponse> findByUserId(String userId);

    Optional<ProfileResponse> updateByUserId(String userId, ProfileUpdateRequest request);
}
