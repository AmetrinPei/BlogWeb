package com.example.blog.user;

public class ProfileResponse {

    private final Long userId;
    private final String username;
    private final String role;
    private final String displayName;
    private final String bio;
    private final String avatarUrl;

    public ProfileResponse(
            Long userId,
            String username,
            String role,
            String displayName,
            String bio,
            String avatarUrl
    ) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public static ProfileResponse from(User user) {
        String role = user.getRole() == null ? UserRole.AUTHOR.name() : user.getRole().name();
        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                role,
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl()
        );
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

    public String getBio() {
        return bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
