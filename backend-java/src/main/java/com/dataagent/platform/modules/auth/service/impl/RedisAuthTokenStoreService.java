package com.dataagent.platform.modules.auth.service.impl;

import com.dataagent.platform.modules.auth.domain.model.RefreshTokenSessionState;
import com.dataagent.platform.modules.auth.service.AuthTokenStoreService;
import com.dataagent.platform.modules.auth.util.AuthTokenRedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisAuthTokenStoreService implements AuthTokenStoreService {

    private static final String ACCESS_TOKEN_BLACKLIST_VALUE = "1";
    private static final String REFRESH_TOKEN_FIELD_DELIMITER = "|";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void storeRefreshToken(String userId, String refreshToken, Duration ttl, Instant lastActivityAt) {
        if (isBlank(userId) || isBlank(refreshToken) || isInvalidTtl(ttl) || lastActivityAt == null) {
            return;
        }

        Instant expiresAt = Instant.now().plus(ttl);
        String value = encodeRefreshTokenSession(refreshToken, expiresAt, lastActivityAt);
        stringRedisTemplate.opsForValue().set(
                AuthTokenRedisKeyUtil.refreshTokenKey(userId),
                value,
                ttl
        );
    }

    @Override
    public Optional<RefreshTokenSessionState> findRefreshTokenSession(String userId, String refreshToken) {
        if (isBlank(userId) || isBlank(refreshToken)) {
            return Optional.empty();
        }

        String key = AuthTokenRedisKeyUtil.refreshTokenKey(userId);
        String cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (cachedValue == null || cachedValue.isBlank()) {
            return Optional.empty();
        }

        String[] segments = cachedValue.split("\\|", -1);
        if (segments.length != 3) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }

        String presentedHash = hashRefreshToken(refreshToken);
        if (!safeEquals(segments[0], presentedHash)) {
            return Optional.empty();
        }

        try {
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(segments[1]));
            Instant lastActivityAt = Instant.ofEpochMilli(Long.parseLong(segments[2]));
            return Optional.of(new RefreshTokenSessionState(expiresAt, lastActivityAt));
        } catch (RuntimeException exception) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
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

    private String encodeRefreshTokenSession(String refreshToken, Instant expiresAt, Instant lastActivityAt) {
        return String.join(
                REFRESH_TOKEN_FIELD_DELIMITER,
                hashRefreshToken(refreshToken),
                String.valueOf(expiresAt.toEpochMilli()),
                String.valueOf(lastActivityAt.toEpochMilli())
        );
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private boolean safeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }

        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isInvalidTtl(Duration ttl) {
        return ttl == null || ttl.isZero() || ttl.isNegative();
    }
}
