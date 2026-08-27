package com.dataagent.platform.modules.auth.service;

import com.dataagent.platform.modules.auth.domain.model.AuthSession;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.domain.model.JwtUserClaims;

import java.util.Optional;

public interface JwtTokenService {

    AuthSession createSession(AuthUser user);

    Optional<JwtUserClaims> decodeAccessToken(String token);

    Optional<JwtUserClaims> decodeRefreshToken(String token);
}
