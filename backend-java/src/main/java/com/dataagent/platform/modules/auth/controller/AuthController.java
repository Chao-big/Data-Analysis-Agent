package com.dataagent.platform.modules.auth.controller;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.security.SecurityAccessContextHolder;
import com.dataagent.platform.common.security.TaskAccessContext;
import com.dataagent.platform.common.web.ApiException;
import com.dataagent.platform.common.web.ApiResponse;
import com.dataagent.platform.common.web.ApiStatusCode;
import com.dataagent.platform.modules.auth.domain.dto.AccessContextRequest;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.AuthRequestMetadata;
import com.dataagent.platform.modules.auth.domain.dto.CurrentUserResponse;
import com.dataagent.platform.modules.auth.domain.dto.LoginRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutRequest;
import com.dataagent.platform.modules.auth.domain.dto.LogoutResponse;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.domain.dto.TokenResponse;
import com.dataagent.platform.modules.auth.service.AuthService;
import com.dataagent.platform.modules.auth.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityAccessContextHolder securityAccessContextHolder;

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(
            @RequestBody AuthRegisterDTO request,
            HttpServletRequest httpServletRequest
    ) {
        log.debug("Received register request for username={}", request == null ? null : request.username());
        return ApiResponse.ok(authService.register(request, requestMetadata(httpServletRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.debug("Received login request for username={}", request == null ? null : request.username());
        return authService.login(request, requestMetadata(httpServletRequest))
                .map(ApiResponse::ok)
                .orElseThrow(() -> new ApiException(ApiStatusCode.UNAUTHORIZED, "账号或密码错误。"));
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody(required = false) LogoutRequest request
    ) {
        log.debug("Received logout request");
        if (principal == null) {
            throw new ApiException(ApiStatusCode.UNAUTHORIZED, "login required");
        }
        return ApiResponse.ok(authService.logout(principal.accessToken(), request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.debug("Received refresh request");
        return authService.refresh(request)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new ApiException(
                        ApiStatusCode.UNAUTHORIZED,
                        "refresh token is invalid or expired"
                ));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        log.debug("Received current user request");
        if (principal == null) {
            throw new ApiException(ApiStatusCode.UNAUTHORIZED, "login required");
        }
        return ApiResponse.ok(new CurrentUserResponse(
                principal.userId(),
                principal.username(),
                principal.nickname(),
                principal.avatarUrl(),
                principal.status(),
                principal.tenantId(),
                principal.roles()
        ));
    }

    @PostMapping("/access-context")
    public ApiResponse<TaskAccessContext> accessContext(@RequestBody AccessContextRequest request) {
        log.debug("Received access context request for datasetIds={}", request == null ? null : request.datasetIds());
        return ApiResponse.ok(securityAccessContextHolder.currentTaskAccessContext(
                request == null ? null : request.datasetIds()
        ));
    }

    @GetMapping("/context-demo")
    public ApiResponse<TaskAccessContext> contextDemo() {
        return ApiResponse.ok(authService.contextDemo());
    }

    private AuthRequestMetadata requestMetadata(HttpServletRequest request) {
        if (request == null) {
            return new AuthRequestMetadata(null, null);
        }

        return new AuthRequestMetadata(
                RequestIpUtil.resolveClientIp(request),
                normalizeNullable(request.getHeader("User-Agent"))
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
