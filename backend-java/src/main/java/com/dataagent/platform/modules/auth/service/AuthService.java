package com.dataagent.platform.modules.auth.service;

import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.common.security.AccessTokenAuthenticationService;
import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.modules.auth.domain.dto.CurrentUserResponse;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.domain.dto.TokenResponse;
import com.dataagent.platform.modules.auth.domain.model.AuthSession;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.domain.model.JwtUserClaims;
import com.dataagent.platform.modules.auth.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AccessTokenAuthenticationService {

    private final AuthRepository authRepository;
    private final JwtTokenService jwtTokenService;

    public Optional<TokenResponse> login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            log.warn("Login request rejected because username or password is blank");
            return Optional.empty();
        }

        return authRepository.findByUsername(request.username())
                .map(user -> {
                    log.debug("Found user for login username={}", request.username());
                    return user;
                })
                .filter(user -> user.passwordHash().equals(request.password()))
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.status()))
                .map(jwtTokenService::createSession)
                .map(session -> {
                    log.info("Login succeeded for userId={}", session.user().userId());
                    return session;
                })
                .map(this::toTokenResponse)
                .or(() -> {
                    log.warn("Login failed for username={}", request.username());
                    return Optional.empty();
                });
    }

    public LogoutResponse logout(String accessToken) {
        log.info("Processing logout request");
        return new LogoutResponse(true);
    }

    public Optional<TokenResponse> refresh(RefreshTokenRequest request) {
        if (request == null || isBlank(request.refreshToken())) {
            log.warn("Refresh request rejected because refresh token is blank");
            return Optional.empty();
        }

        return jwtTokenService.decodeRefreshToken(request.refreshToken())
                .flatMap(claims -> authRepository.findByUserId(claims.userId()))
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.status()))
                .map(jwtTokenService::createSession)
                .map(session -> {
                    log.info("Refresh token succeeded for userId={}", session.user().userId());
                    return session;
                })
                .map(this::toTokenResponse)
                .or(() -> {
                    log.warn("Refresh token failed");
                    return Optional.empty();
                });
    }

    public TaskAccessContext contextDemo() {
        return new TaskAccessContext(
                "tenant-demo",
                "user-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }

    @Override
    public Optional<AuthenticatedUserPrincipal> authenticate(String accessToken) {
        if (isBlank(accessToken)) {
            return Optional.empty();
        }

        return jwtTokenService.decodeAccessToken(accessToken)
                .flatMap(claims -> buildPrincipal(accessToken, claims))
                .or(() -> {
                    log.debug("No user resolved from access token");
                    return Optional.empty();
                });
    }

    public Optional<CurrentUserResponse> currentUser(String accessToken) {
        return authenticate(accessToken)
                .map(principal -> new CurrentUserResponse(
                        principal.userId(),
                        principal.username(),
                        principal.nickname(),
                        principal.avatarUrl(),
                        principal.status(),
                        principal.tenantId(),
                        principal.roles()
                ));
    }

    private Optional<AuthenticatedUserPrincipal> buildPrincipal(String accessToken, JwtUserClaims claims) {
        return authRepository.findByUserId(claims.userId())
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.status()))
                .map(user -> new AuthenticatedUserPrincipal(
                        accessToken,
                        user.userId(),
                        claims.username(),
                        claims.nickname(),
                        claims.avatarUrl(),
                        claims.status(),
                        user.tenantId(),
                        user.roles(),
                        user.allowedDatasets(),
                        user.maskedColumns()
                ));
    }

    private TokenResponse toTokenResponse(AuthSession session) {
        return new TokenResponse(
                session.accessToken(),
                session.refreshToken(),
                session.accessTokenExpiresIn(),
                session.refreshTokenExpiresIn(),
                session.user().userId(),
                session.user().username(),
                session.user().nickname(),
                session.user().avatarUrl(),
                session.user().status(),
                session.user().tenantId(),
                session.user().roles()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
