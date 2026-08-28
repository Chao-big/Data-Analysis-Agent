package com.dataagent.platform.modules.auth.service.impl;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.AuthRequestMetadata;
import com.dataagent.platform.modules.auth.domain.dto.CurrentUserResponse;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.domain.dto.TokenResponse;
import com.dataagent.platform.modules.auth.domain.model.AuthSession;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.domain.model.JwtUserClaims;
import com.dataagent.platform.modules.auth.domain.po.AuthLoginLogPO;
import com.dataagent.platform.modules.auth.mapper.AuthLoginLogMapper;
import com.dataagent.platform.modules.auth.repository.AuthRepository;
import com.dataagent.platform.modules.auth.service.AuthTokenStoreService;
import com.dataagent.platform.modules.auth.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private AuthTokenStoreService authTokenStoreService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthLoginLogMapper authLoginLogMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldCreateUserPersistRefreshTokenAndWriteAuditLog() {
        AuthRegisterDTO request = new AuthRegisterDTO(
                " new-user ",
                "Password@123",
                " ",
                "",
                " new-user@example.com ",
                " 13800000011 ",
                null,
                " "
        );
        AuthUser createdUser = new AuthUser(
                "100",
                "new-user",
                "encoded-password",
                "new-user",
                "https://static.local/avatar/new-user.png",
                "new-user@example.com",
                "13800000011",
                "UNKNOWN",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
        AuthSession session = session(createdUser, "access-token", "refresh-token", 900L, 604800L);

        when(authRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("new-user@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByPhone("13800000011")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(authRepository.create(any(AuthRegisterDTO.class), eq("encoded-password"))).thenReturn(createdUser);
        when(jwtTokenService.createSession(createdUser)).thenReturn(session);

        TokenResponse response = authService.register(
                request,
                new AuthRequestMetadata("203.0.113.10", "JUnit/Register")
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo("100");
        assertThat(response.username()).isEqualTo("new-user");
        assertThat(response.nickname()).isEqualTo("new-user");

        verify(authRepository).create(
                eq(new AuthRegisterDTO(
                        "new-user",
                        "Password@123",
                        "new-user",
                        null,
                        "new-user@example.com",
                        "13800000011",
                        null,
                        null
                )),
                eq("encoded-password")
        );
        verify(authTokenStoreService).storeRefreshToken(
                "100",
                "refresh-token",
                Duration.ofSeconds(604800L)
        );

        ArgumentCaptor<LocalDateTime> loginAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(authRepository).updateLoginSuccess(eq("100"), loginAtCaptor.capture(), eq("203.0.113.10"));
        assertThat(loginAtCaptor.getValue()).isNotNull();

        ArgumentCaptor<AuthLoginLogPO> loginLogCaptor = ArgumentCaptor.forClass(AuthLoginLogPO.class);
        verify(authLoginLogMapper).insert(loginLogCaptor.capture());
        assertThat(loginLogCaptor.getValue().getUserId()).isEqualTo(100L);
        assertThat(loginLogCaptor.getValue().getUsername()).isEqualTo("new-user");
        assertThat(loginLogCaptor.getValue().getClientPublicIp()).isEqualTo("203.0.113.10");
        assertThat(loginLogCaptor.getValue().getUserAgent()).isEqualTo("JUnit/Register");
        assertThat(loginLogCaptor.getValue().getLoginAt()).isNotNull();
    }

    @Test
    void loginShouldCreateSessionForActiveUserAndWriteAuditLog() {
        AuthUser user = user("1001", "analyst01", "Password@123");
        AuthSession session = session(user, "access-token", "refresh-token", 900L, 604800L);

        when(authRepository.findByIdentifier("analyst01")).thenReturn(Optional.of(user));
        when(jwtTokenService.createSession(user)).thenReturn(session);

        Optional<TokenResponse> response = authService.login(
                new LoginRequest(" analyst01 ", "Password@123"),
                new AuthRequestMetadata("198.51.100.20", "JUnit/Login")
        );

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().accessToken()).isEqualTo("access-token");
        verify(authTokenStoreService).storeRefreshToken(
                "1001",
                "refresh-token",
                Duration.ofSeconds(604800L)
        );

        ArgumentCaptor<LocalDateTime> loginAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(authRepository).updateLoginSuccess(eq("1001"), loginAtCaptor.capture(), eq("198.51.100.20"));
        assertThat(loginAtCaptor.getValue()).isNotNull();

        ArgumentCaptor<AuthLoginLogPO> loginLogCaptor = ArgumentCaptor.forClass(AuthLoginLogPO.class);
        verify(authLoginLogMapper).insert(loginLogCaptor.capture());
        assertThat(loginLogCaptor.getValue().getUserId()).isEqualTo(1001L);
        assertThat(loginLogCaptor.getValue().getUsername()).isEqualTo("analyst01");
        assertThat(loginLogCaptor.getValue().getClientPublicIp()).isEqualTo("198.51.100.20");
        assertThat(loginLogCaptor.getValue().getUserAgent()).isEqualTo("JUnit/Login");
    }

    @Test
    void loginShouldNotWriteAuditWhenPasswordIsInvalid() {
        AuthUser user = user("1001", "analyst01", "Password@123");

        when(authRepository.findByIdentifier("analyst01")).thenReturn(Optional.of(user));

        Optional<TokenResponse> response = authService.login(
                new LoginRequest("analyst01", "wrong-password"),
                new AuthRequestMetadata("198.51.100.21", "JUnit/Login-Fail")
        );

        assertThat(response).isEmpty();
        verify(authRepository, never()).updateLoginSuccess(any(), any(), any());
        verify(authTokenStoreService, never()).storeRefreshToken(any(), any(), any());
        verify(authLoginLogMapper, never()).insert(any(AuthLoginLogPO.class));
    }

    @Test
    void refreshShouldIssueNewSessionWhenRefreshTokenMatchesRedis() {
        AuthUser user = user("user-001", "analyst01", "Password@123");
        AuthSession session = session(user, "new-access-token", "new-refresh-token", 900L, 604800L);
        JwtUserClaims refreshClaims = new JwtUserClaims(
                "refresh-jti",
                "refresh",
                "user-001",
                "analyst01",
                "analyst",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Instant.now(),
                Instant.now().plus(Duration.ofDays(7))
        );

        when(jwtTokenService.decodeRefreshToken("refresh-token")).thenReturn(Optional.of(refreshClaims));
        when(authTokenStoreService.matchesRefreshToken("user-001", "refresh-token")).thenReturn(true);
        when(authRepository.findByUserId("user-001")).thenReturn(Optional.of(user));
        when(jwtTokenService.createSession(user)).thenReturn(session);

        Optional<TokenResponse> response = authService.refresh(new RefreshTokenRequest("refresh-token"));

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().refreshToken()).isEqualTo("new-refresh-token");
        verify(authTokenStoreService).storeRefreshToken(
                "user-001",
                "new-refresh-token",
                Duration.ofSeconds(604800L)
        );
    }

    @Test
    void logoutShouldRemoveRefreshTokenAndBlacklistAccessToken() {
        JwtUserClaims accessClaims = new JwtUserClaims(
                "access-jti",
                "access",
                "user-001",
                "analyst01",
                "analyst",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Instant.now(),
                Instant.now().plus(Duration.ofMinutes(15))
        );
        JwtUserClaims refreshClaims = new JwtUserClaims(
                "refresh-jti",
                "refresh",
                "user-001",
                "analyst01",
                "analyst",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Instant.now(),
                Instant.now().plus(Duration.ofDays(7))
        );

        when(jwtTokenService.decodeAccessToken("access-token")).thenReturn(Optional.of(accessClaims));
        when(jwtTokenService.decodeRefreshToken("refresh-token")).thenReturn(Optional.of(refreshClaims));

        LogoutResponse response = authService.logout("access-token", new LogoutRequest("refresh-token"));

        assertThat(response.loggedOut()).isTrue();

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(authTokenStoreService).blacklistAccessToken(eq("user-001"), eq("access-jti"), ttlCaptor.capture());
        verify(authTokenStoreService, atLeastOnce()).removeRefreshToken("user-001");
        assertThat(ttlCaptor.getValue()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    void authenticateShouldReturnEmptyWhenAccessTokenIsBlacklisted() {
        JwtUserClaims accessClaims = new JwtUserClaims(
                "access-jti",
                "access",
                "user-001",
                "analyst01",
                "analyst",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Instant.now(),
                Instant.now().plus(Duration.ofMinutes(15))
        );

        when(jwtTokenService.decodeAccessToken("access-token")).thenReturn(Optional.of(accessClaims));
        when(authTokenStoreService.isAccessTokenBlacklisted("user-001", "access-jti")).thenReturn(true);

        Optional<AuthenticatedUserPrincipal> response = authService.authenticate("access-token");

        assertThat(response).isEmpty();
        verify(authRepository, never()).findByUserId("user-001");
    }

    @Test
    void currentUserShouldResolvePrincipalInformationFromAccessToken() {
        AuthUser user = user("user-001", "analyst01", "Password@123");
        JwtUserClaims accessClaims = new JwtUserClaims(
                "access-jti",
                "access",
                "user-001",
                "analyst01",
                "analyst",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Instant.now(),
                Instant.now().plus(Duration.ofMinutes(15))
        );

        when(jwtTokenService.decodeAccessToken("access-token")).thenReturn(Optional.of(accessClaims));
        when(authTokenStoreService.isAccessTokenBlacklisted("user-001", "access-jti")).thenReturn(false);
        when(authRepository.findByUserId("user-001")).thenReturn(Optional.of(user));

        Optional<CurrentUserResponse> response = authService.currentUser("access-token");

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().username()).isEqualTo("analyst01");
        assertThat(response.orElseThrow().roles()).containsExactly("ANALYST");
    }

    @Test
    void contextDemoShouldReturnExpectedDemoAccessContext() {
        TaskAccessContext response = authService.contextDemo();

        assertThat(response.tenantId()).isEqualTo("tenant-demo");
        assertThat(response.userId()).isEqualTo("user-demo");
        assertThat(response.roles()).containsExactly("ANALYST");
        assertThat(response.allowedDatasets()).containsExactly("dataset-sales");
        assertThat(response.maskedColumns()).containsExactly("phone");
    }

    private AuthUser user(String userId, String username, String passwordHash) {
        return new AuthUser(
                userId,
                username,
                passwordHash,
                "analyst",
                "https://static.local/avatar/analyst01.png",
                username + "@example.com",
                "13800000001",
                "UNKNOWN",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }

    private AuthSession session(
            AuthUser user,
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn
    ) {
        return new AuthSession(
                accessToken,
                refreshToken,
                accessTokenExpiresIn,
                refreshTokenExpiresIn,
                user
        );
    }
}
