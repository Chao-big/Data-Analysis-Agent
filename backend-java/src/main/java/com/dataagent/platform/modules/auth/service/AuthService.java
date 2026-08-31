package com.dataagent.platform.modules.auth.service;

import com.dataagent.platform.common.security.AccessTokenAuthenticationService;
import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.AuthRequestMetadata;
import com.dataagent.platform.modules.auth.domain.dto.CurrentUserResponse;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.model.AuthSession;

import java.util.Optional;

public interface AuthService extends AccessTokenAuthenticationService {

    AuthSession register(AuthRegisterDTO request, AuthRequestMetadata requestMetadata);

    Optional<AuthSession> login(LoginRequest request, AuthRequestMetadata requestMetadata);

    LogoutResponse logout(String accessToken, String refreshToken);

    Optional<AuthSession> refresh(String refreshToken, AuthRequestMetadata requestMetadata);

    TaskAccessContext contextDemo();

    Optional<CurrentUserResponse> currentUser(String accessToken);
}
