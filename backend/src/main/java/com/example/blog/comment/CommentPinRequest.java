package com.example.blog.comment;

import jakarta.validation.constraints.NotNull;

public class CommentPinRequest {

    @NotNull(message = "pinned 不能为空")
    private Boolean pinned;

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
}
