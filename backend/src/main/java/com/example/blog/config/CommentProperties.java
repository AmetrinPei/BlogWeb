package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.comment")
public class CommentProperties {

    /**
     * Max comments (including replies) per user per rolling 1-minute window.
     * {@code <= 0} disables rate limiting.
     */
    private int rateLimitPerMinute = 5;

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
}
