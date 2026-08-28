package com.dataagent.platform.modules.auth.service;

import java.time.Duration;

public interface AuthTokenStoreService {

    void storeRefreshToken(String userId, String refreshToken, Duration ttl);

    boolean matchesRefreshToken(String userId, String refreshToken);

    void removeRefreshToken(String userId);

    void blacklistAccessToken(String userId, String tokenId, Duration ttl);

    boolean isAccessTokenBlacklisted(String userId, String tokenId);
}
