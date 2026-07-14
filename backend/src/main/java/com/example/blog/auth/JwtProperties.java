package com.example.blog.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.jwt")
public class JwtProperties {

    private String secret = "blog-mvp-jwt-secret-key-change-me-32bytes-min";
    private int expireHours = 2;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(int expireHours) {
        this.expireHours = expireHours;
    }
}
