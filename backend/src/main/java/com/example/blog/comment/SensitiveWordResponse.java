package com.example.blog.comment;

import java.time.LocalDateTime;

public class SensitiveWordResponse {

    private final Long id;
    private final String word;
    private final LocalDateTime createdAt;

    public SensitiveWordResponse(Long id, String word, LocalDateTime createdAt) {
        this.id = id;
        this.word = word;
        this.createdAt = createdAt;
    }

    public static SensitiveWordResponse from(SensitiveWord entity) {
        return new SensitiveWordResponse(entity.getId(), entity.getWord(), entity.getCreatedAt());
    }

    public Long getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
