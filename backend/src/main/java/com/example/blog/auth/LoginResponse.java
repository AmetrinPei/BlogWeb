package com.example.blog.auth;

public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expireHours;
    private final Long userId;
    private final String username;
    private final String role;

    public LoginResponse(String token, long expireHours, Long userId, String username, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expireHours = expireHours;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpireHours() {
        return expireHours;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
