package com.example.blog.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.jwt")
public class JwtProperties {

    private String secret = "blog-mvp-jwt-secret-key-change-me-32bytes-min";
    /** Access JWT TTL in minutes (primary). */
    private int accessExpireMinutes = 30;
    /** Refresh token TTL in days. */
    private int refreshExpireDays = 14;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getAccessExpireMinutes() {
        return accessExpireMinutes;
    }

    public void setAccessExpireMinutes(int accessExpireMinutes) {
        this.accessExpireMinutes = accessExpireMinutes;
    }

    public int getRefreshExpireDays() {
        return refreshExpireDays;
    }

    public void setRefreshExpireDays(int refreshExpireDays) {
        this.refreshExpireDays = refreshExpireDays;
    }

    /** Compat: hours rounded up from minutes, at least 1. */
    public long compatExpireHours() {
        return Math.max(1L, Math.round(accessExpireMinutes / 60.0));
    }
}
