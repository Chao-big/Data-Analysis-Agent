package com.dataagent.platform.modules.auth.util;

public final class AuthTokenRedisKeyUtil {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:user:";
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "auth:access:blacklist:user:";

    private AuthTokenRedisKeyUtil() {
    }

    public static String refreshTokenKey(String userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }

    public static String accessTokenBlacklistKey(String userId, String tokenId) {
        return ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + userId + ":jti:" + tokenId;
    }
}
