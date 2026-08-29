package com.dataagent.platform.modules.profile.service;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;

public interface ProfileService {

    ProfileResponse getCurrentProfile(AuthenticatedUserPrincipal principal);

    ProfileResponse updateCurrentProfile(AuthenticatedUserPrincipal principal, ProfileUpdateRequest request);
}
