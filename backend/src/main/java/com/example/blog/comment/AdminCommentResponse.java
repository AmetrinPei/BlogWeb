package com.example.blog.comment;

import java.time.LocalDateTime;

public class AdminCommentResponse {

    private final Long id;
    private final Long articleId;
    private final Long userId;
    private final String username;
    private final String content;
    private final CommentStatus status;
    private final Long parentId;
    private final LocalDateTime createdAt;

    public AdminCommentResponse(
            Long id,
            Long articleId,
            Long userId,
            String username,
            String content,
            CommentStatus status,
            Long parentId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.status = status;
        this.parentId = parentId;
        this.createdAt = createdAt;
    }

    public static AdminCommentResponse from(Comment comment) {
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        return new AdminCommentResponse(
                comment.getId(),
                comment.getArticle().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getStatus(),
                parentId,
                comment.getCreatedAt()
        );
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

    public CommentStatus getStatus() {
        return status;
    }

    public Long getParentId() {
        return parentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
