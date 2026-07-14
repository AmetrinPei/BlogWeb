package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.site")
public class BlogSiteProperties {

    private static final String DEFAULT_BASE_URL = "http://localhost:5173";
    private static final int DEFAULT_FEED_LIMIT = 20;
    private static final int MAX_FEED_LIMIT = 50;

    /**
     * Public site root URL (frontend origin), no trailing slash.
     */
    private String baseUrl = DEFAULT_BASE_URL;

    /**
     * Max items in RSS feed; clamped to {@link #MAX_FEED_LIMIT}.
     */
    private int feedLimit = DEFAULT_FEED_LIMIT;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getFeedLimit() {
        return feedLimit;
    }

    public void setFeedLimit(int feedLimit) {
        this.feedLimit = feedLimit;
    }

    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? DEFAULT_BASE_URL : trimmed;
    }

    public int resolvedFeedLimit() {
        if (feedLimit <= 0) {
            return DEFAULT_FEED_LIMIT;
        }
        return Math.min(feedLimit, MAX_FEED_LIMIT);
    }
}
