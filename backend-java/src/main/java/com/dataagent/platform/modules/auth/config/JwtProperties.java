package com.dataagent.platform.modules.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String algorithm;
    private Resource privateKeyPath;
    private Resource publicKeyPath;
    private Duration accessTokenExpire;
    private Duration refreshTokenExpire;
    private String issuer;
    private String header;
    private String prefix;
}
