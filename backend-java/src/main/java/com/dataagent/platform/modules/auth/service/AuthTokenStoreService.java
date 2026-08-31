package com.dataagent.platform.modules.auth.service;

import com.dataagent.platform.modules.auth.domain.model.RefreshTokenSessionState;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface AuthTokenStoreService {

    void storeRefreshToken(String userId, String refreshToken, Duration ttl, Instant lastActivityAt);

    Optional<RefreshTokenSessionState> findRefreshTokenSession(String userId, String refreshToken);

    void removeRefreshToken(String userId);

    void blacklistAccessToken(String userId, String tokenId, Duration ttl);

    boolean isAccessTokenBlacklisted(String userId, String tokenId);
}
