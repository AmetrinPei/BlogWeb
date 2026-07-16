package com.example.blog.user;

public class ChangePasswordResponse {

    private final Long userId;
    private final String username;

    public ChangePasswordResponse(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
