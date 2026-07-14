package com.example.blog.auth;

public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expireHours;
    private final Long userId;
    private final String username;
    private final String role;
    private final String displayName;
    private final String avatarUrl;

    public LoginResponse(
            String token,
            long expireHours,
            Long userId,
            String username,
            String role,
            String displayName,
            String avatarUrl
    ) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expireHours = expireHours;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
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

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
