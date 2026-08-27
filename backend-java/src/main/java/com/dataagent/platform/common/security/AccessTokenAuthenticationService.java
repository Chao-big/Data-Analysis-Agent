package com.dataagent.platform.common.security;

import java.util.Optional;

public interface AccessTokenAuthenticationService {

    Optional<AuthenticatedUserPrincipal> authenticate(String accessToken);
}
