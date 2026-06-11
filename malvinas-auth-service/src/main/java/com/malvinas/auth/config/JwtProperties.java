package com.malvinas.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration = 3600000;
    private long refreshTokenExpiration = 604800000;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long v) { this.accessTokenExpiration = v; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long v) { this.refreshTokenExpiration = v; }
}
