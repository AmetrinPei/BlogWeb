package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "blog.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = new ArrayList<>();

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    /**
     * Accepts a YAML list, or a single comma-separated string from env
     * {@code BLOG_CORS_ALLOWED_ORIGIN_PATTERNS}.
     */
    public void setAllowedOriginPatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            this.allowedOriginPatterns = List.of();
            return;
        }
        if (patterns.size() == 1 && patterns.get(0) != null && patterns.get(0).contains(",")) {
            this.allowedOriginPatterns = splitComma(patterns.get(0));
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        this.allowedOriginPatterns = List.copyOf(normalized);
    }

    private static List<String> splitComma(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Objects::requireNonNull)
                .toList();
    }
}
