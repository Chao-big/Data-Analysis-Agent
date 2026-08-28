package com.dataagent.platform.modules.auth.service.impl;

import com.dataagent.platform.modules.auth.config.JwtProperties;
import com.dataagent.platform.modules.auth.domain.model.AuthSession;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.domain.model.JwtUserClaims;
import com.dataagent.platform.modules.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_NICKNAME = "nickname";
    private static final String CLAIM_AVATAR_URL = "avatar_url";
    private static final String CLAIM_STATUS = "status";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Override
    public AuthSession createSession(AuthUser user) {
        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plus(jwtProperties.getAccessTokenExpire());
        Instant refreshExpiresAt = issuedAt.plus(jwtProperties.getRefreshTokenExpire());

        String accessToken = encodeToken(user, issuedAt, accessExpiresAt, TOKEN_TYPE_ACCESS);
        String refreshToken = encodeToken(user, issuedAt, refreshExpiresAt, TOKEN_TYPE_REFRESH);

        return new AuthSession(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpire().toSeconds(),
                jwtProperties.getRefreshTokenExpire().toSeconds(),
                user
        );
    }

    @Override
    public Optional<JwtUserClaims> decodeAccessToken(String token) {
        return decodeToken(token, TOKEN_TYPE_ACCESS);
    }

    @Override
    public Optional<JwtUserClaims> decodeRefreshToken(String token) {
        return decodeToken(token, TOKEN_TYPE_REFRESH);
    }

    private String encodeToken(
            AuthUser user,
            Instant issuedAt,
            Instant expiresAt,
            String tokenType
    ) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.userId())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_USERNAME, user.username())
                .claim(CLAIM_NICKNAME, user.nickname())
                .claim(CLAIM_STATUS, user.status())
                .claim(CLAIM_TENANT_ID, user.tenantId())
                .claim(CLAIM_ROLES, user.roles());

        if (user.avatarUrl() != null && !user.avatarUrl().isBlank()) {
            builder.claim(CLAIM_AVATAR_URL, user.avatarUrl());
        }

        JwtClaimsSet claimsSet = builder.build();

        JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    private Optional<JwtUserClaims> decodeToken(String token, String expectedTokenType) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String tokenType = jwt.getClaimAsString(CLAIM_TOKEN_TYPE);
            if (!expectedTokenType.equals(tokenType)) {
                log.warn("JWT token type mismatch. expected={}, actual={}", expectedTokenType, tokenType);
                return Optional.empty();
            }

            List<String> roleClaims = jwt.getClaimAsStringList(CLAIM_ROLES);
            Set<String> roles = roleClaims == null ? Set.of() : Set.copyOf(roleClaims);
            return Optional.of(new JwtUserClaims(
                    jwt.getId(),
                    tokenType,
                    jwt.getSubject(),
                    jwt.getClaimAsString(CLAIM_USERNAME),
                    jwt.getClaimAsString(CLAIM_NICKNAME),
                    jwt.getClaimAsString(CLAIM_AVATAR_URL),
                    jwt.getClaimAsString(CLAIM_STATUS),
                    jwt.getClaimAsString(CLAIM_TENANT_ID),
                    roles,
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt()
            ));
        } catch (JwtException exception) {
            log.warn("JWT decode failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
