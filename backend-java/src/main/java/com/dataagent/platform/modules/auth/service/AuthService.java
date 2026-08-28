package com.dataagent.platform.modules.auth.service;

import com.dataagent.platform.common.security.AccessTokenAuthenticationService;
import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.AuthRequestMetadata;
import com.dataagent.platform.modules.auth.domain.dto.CurrentUserResponse;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.domain.dto.TokenResponse;

import java.util.Optional;

public interface AuthService extends AccessTokenAuthenticationService {

    TokenResponse register(AuthRegisterDTO request, AuthRequestMetadata requestMetadata);

    Optional<TokenResponse> login(LoginRequest request, AuthRequestMetadata requestMetadata);

    LogoutResponse logout(String accessToken, LogoutRequest request);

    Optional<TokenResponse> refresh(RefreshTokenRequest request);

    TaskAccessContext contextDemo();

    Optional<CurrentUserResponse> currentUser(String accessToken);
}
