package com.example.blog.user;

import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @Size(max = 32, message = "展示名不能超过 32 个字符")
    private String displayName;

    @Size(max = 200, message = "简介不能超过 200 个字符")
    private String bio;

    @Size(max = 512, message = "头像 URL 不能超过 512 个字符")
    private String avatarUrl;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
