package com.example.blog.auth;

public class TokenPairResponse {

    private final String token;
    private final String tokenType;
    private final String refreshToken;
    private final long accessExpireMinutes;
    private final long expireHours;

    public TokenPairResponse(String token, String refreshToken, long accessExpireMinutes, long expireHours) {
        this.token = token;
        this.tokenType = "Bearer";
        this.refreshToken = refreshToken;
        this.accessExpireMinutes = accessExpireMinutes;
        this.expireHours = expireHours;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getAccessExpireMinutes() {
        return accessExpireMinutes;
    }

    public long getExpireHours() {
        return expireHours;
    }
}
