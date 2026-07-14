package com.example.blog.comment;

import jakarta.validation.constraints.NotNull;

public class CommentModerationRequest {

    @NotNull(message = "status 不能为空")
    private CommentStatus status;

    public CommentStatus getStatus() {
        return status;
    }

    public void setStatus(CommentStatus status) {
        this.status = status;
    }
}
