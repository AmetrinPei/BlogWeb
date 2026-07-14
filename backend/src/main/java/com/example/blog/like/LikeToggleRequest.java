package com.example.blog.like;

import jakarta.validation.constraints.NotNull;

public class LikeToggleRequest {

    @NotNull(message = "targetType 不能为空")
    private LikeTargetType targetType;

    @NotNull(message = "targetId 不能为空")
    private Long targetId;

    public LikeTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(LikeTargetType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
