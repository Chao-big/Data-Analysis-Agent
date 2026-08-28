package com.dataagent.platform.modules.auth.service.impl;

import com.dataagent.platform.modules.auth.service.AuthTokenStoreService;
import com.dataagent.platform.modules.auth.util.AuthTokenRedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RedisAuthTokenStoreService implements AuthTokenStoreService {

    private static final String ACCESS_TOKEN_BLACKLIST_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void storeRefreshToken(String userId, String refreshToken, Duration ttl) {
        if (isBlank(userId) || isBlank(refreshToken) || isInvalidTtl(ttl)) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                AuthTokenRedisKeyUtil.refreshTokenKey(userId),
                refreshToken,
                ttl
        );
    }

    @Override
    public boolean matchesRefreshToken(String userId, String refreshToken) {
        if (isBlank(userId) || isBlank(refreshToken)) {
            return false;
        }

        String cachedToken = stringRedisTemplate.opsForValue().get(AuthTokenRedisKeyUtil.refreshTokenKey(userId));
        return Objects.equals(cachedToken, refreshToken);
    }

    @Override
    public void removeRefreshToken(String userId) {
        if (isBlank(userId)) {
            return;
        }

        stringRedisTemplate.delete(AuthTokenRedisKeyUtil.refreshTokenKey(userId));
    }

    @Override
    public void blacklistAccessToken(String userId, String tokenId, Duration ttl) {
        if (isBlank(userId) || isBlank(tokenId) || isInvalidTtl(ttl)) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                AuthTokenRedisKeyUtil.accessTokenBlacklistKey(userId, tokenId),
                ACCESS_TOKEN_BLACKLIST_VALUE,
                ttl
        );
    }

    @Override
    public boolean isAccessTokenBlacklisted(String userId, String tokenId) {
        if (isBlank(userId) || isBlank(tokenId)) {
            return false;
        }

        Boolean exists = stringRedisTemplate.hasKey(AuthTokenRedisKeyUtil.accessTokenBlacklistKey(userId, tokenId));
        return Boolean.TRUE.equals(exists);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isInvalidTtl(Duration ttl) {
        return ttl == null || ttl.isZero() || ttl.isNegative();
    }
}
