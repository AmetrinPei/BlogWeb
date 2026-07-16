package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.auth.login-rate-limit")
public class LoginRateLimitProperties {

    /**
     * Max failed login attempts per key within the window.
     * {@code <= 0} disables login rate limiting.
     */
    private int maxFailures = 5;

    /**
     * Sliding window length in seconds (default 15 minutes).
     */
    private int windowSeconds = 900;

    public int getMaxFailures() {
        return maxFailures;
    }

    public void setMaxFailures(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public boolean isEnabled() {
        return maxFailures > 0 && windowSeconds > 0;
    }
}
