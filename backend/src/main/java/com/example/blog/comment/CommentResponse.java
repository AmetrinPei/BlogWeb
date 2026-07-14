package com.example.blog.comment;

import java.time.LocalDateTime;

public class CommentResponse {

    private final Long id;
    private final Long articleId;
    private final Long userId;
    private final String username;
    private final String content;
    private final LocalDateTime createdAt;
    private final long likeCount;
    private final boolean likedByMe;

    public CommentResponse(
            Long id,
            Long articleId,
            Long userId,
            String username,
            String content,
            LocalDateTime createdAt,
            long likeCount,
            boolean likedByMe
    ) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
    }

    public Long getId() {
        return id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }
}
