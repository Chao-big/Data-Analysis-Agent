package com.dataagent.platform.modules.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {

    private String refreshTokenCookieName = "data-analysis-agent-refresh-token";
    private String refreshTokenCookiePath = "/api/auth";
    private boolean refreshTokenCookieSecure = false;
    private String refreshTokenCookieSameSite = "Lax";
    private Duration inactivityTimeout = Duration.ofHours(48);
}
