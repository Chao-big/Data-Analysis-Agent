package com.dataagent.platform.common.security;

import com.dataagent.platform.common.web.ApiException;
import com.dataagent.platform.common.web.ApiStatusCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SecurityAccessContextHolder {

    public AuthenticatedUserPrincipal requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ApiException(ApiStatusCode.UNAUTHORIZED, "login required");
        }
        return principal;
    }

    public TaskAccessContext currentTaskAccessContext(Set<String> requestedDatasetIds) {
        return requireCurrentUser().toTaskAccessContext(requestedDatasetIds);
    }
}
