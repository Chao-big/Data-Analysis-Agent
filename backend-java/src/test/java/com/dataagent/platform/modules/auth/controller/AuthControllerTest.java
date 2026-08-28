package com.dataagent.platform.modules.auth.controller;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.security.BearerTokenAuthenticationFilter;
import com.dataagent.platform.common.security.SecurityAccessContextHolder;
import com.dataagent.platform.common.security.SecurityAccessDeniedHandler;
import com.dataagent.platform.common.security.SecurityAuthenticationEntryPoint;
import com.dataagent.platform.common.security.SecurityConfig;
import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.domain.dto.TokenResponse;
import com.dataagent.platform.modules.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        SecurityAuthenticationEntryPoint.class,
        SecurityAccessDeniedHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityAccessContextHolder securityAccessContextHolder;

    @Test
    void registerShouldReturnTokenResponse() throws Exception {
        TokenResponse tokenResponse = tokenResponse();
        AuthRegisterDTO request = new AuthRegisterDTO(
                "new-user",
                "Password@123",
                "新用户",
                "https://static.local/avatar/new-user.png",
                "new-user@example.com",
                "13800000011",
                "UNKNOWN",
                "test register"
        );

        when(authService.register(request)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.userId").value("user-001"));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("analyst01", "wrong-password");

        when(authService.login(request)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void refreshShouldReturnNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(authService.refresh(request)).thenReturn(Optional.of(tokenResponse()));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void meShouldReturnCurrentUserWhenBearerTokenIsValid() throws Exception {
        when(authService.authenticate("access-token")).thenReturn(Optional.of(principal()));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-001"))
                .andExpect(jsonPath("$.data.username").value("analyst01"))
                .andExpect(jsonPath("$.data.roles[0]").value("ANALYST"));
    }

    @Test
    void meShouldReturnUnauthorizedWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void logoutShouldDeleteCurrentSessionWhenBearerTokenIsValid() throws Exception {
        LogoutRequest request = new LogoutRequest("refresh-token");

        when(authService.authenticate("access-token")).thenReturn(Optional.of(principal()));
        when(authService.logout(eq("access-token"), eq(request))).thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true));
    }

    @Test
    void accessContextShouldReturnScopedDatasets() throws Exception {
        TaskAccessContext accessContext = new TaskAccessContext(
                "tenant-demo",
                "user-001",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );

        when(authService.authenticate("access-token")).thenReturn(Optional.of(principal()));
        when(securityAccessContextHolder.currentTaskAccessContext(Set.of("dataset-sales"))).thenReturn(accessContext);

        mockMvc.perform(post("/api/auth/access-context")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetIds": ["dataset-sales"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-001"))
                .andExpect(jsonPath("$.data.allowedDatasets[0]").value("dataset-sales"));

        verify(securityAccessContextHolder).currentTaskAccessContext(Set.of("dataset-sales"));
    }

    @Test
    void contextDemoShouldBeAccessibleWithoutAuthentication() throws Exception {
        TaskAccessContext accessContext = new TaskAccessContext(
                "tenant-demo",
                "user-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );

        when(authService.contextDemo()).thenReturn(accessContext);

        mockMvc.perform(get("/api/auth/context-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-demo"));
    }

    private TokenResponse tokenResponse() {
        return new TokenResponse(
                "access-token",
                "refresh-token",
                900L,
                604800L,
                "user-001",
                "analyst01",
                "分析师一号",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST")
        );
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(
                "access-token",
                "user-001",
                "analyst01",
                "分析师一号",
                "https://static.local/avatar/analyst01.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }
}
