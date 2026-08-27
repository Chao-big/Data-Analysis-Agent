package com.dataagent.platform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenAuthenticationService authenticationService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationFilter(
            AccessTokenAuthenticationService authenticationService,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.authenticationService = authenticationService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Optional<String> accessToken = resolveAccessToken(request.getHeader(HttpHeaders.AUTHORIZATION));
            if (accessToken.isPresent()) {
                AuthenticatedUserPrincipal principal = authenticationService.authenticate(accessToken.get())
                        .orElseThrow(() -> new BadCredentialsException("access token is invalid or expired"));

                PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                        principal,
                        principal.accessToken(),
                        principal.authorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, exception);
        }
    }

    private Optional<String> resolveAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("authorization header must use bearer token");
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isEmpty()) {
            throw new BadCredentialsException("access token is blank");
        }
        return Optional.of(accessToken);
    }
}
