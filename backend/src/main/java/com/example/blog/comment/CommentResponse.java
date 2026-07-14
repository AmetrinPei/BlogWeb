package com.example.blog.comment;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponse {

    private final Long id;
    private final Long articleId;
    private final Long userId;
    private final String username;
    private final String displayName;
    private final String avatarUrl;
    private final String content;
    private final LocalDateTime createdAt;
    private final long likeCount;
    private final boolean likedByMe;
    private final Long parentId;
    private final CommentStatus status;
    private final Integer floorNo;
    private final boolean pinned;
    private final List<CommentResponse> replies;

    public CommentResponse(
            Long id,
            Long articleId,
            Long userId,
            String username,
            String displayName,
            String avatarUrl,
            String content,
            LocalDateTime createdAt,
            long likeCount,
            boolean likedByMe,
            Long parentId,
            CommentStatus status,
            Integer floorNo,
            boolean pinned,
            List<CommentResponse> replies
    ) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
        this.parentId = parentId;
        this.status = status;
        this.floorNo = floorNo;
        this.pinned = pinned;
        this.replies = replies == null ? List.of() : List.copyOf(replies);
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

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
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

    public Long getParentId() {
        return parentId;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public boolean isPinned() {
        return pinned;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }
}
