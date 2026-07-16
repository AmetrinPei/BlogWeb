package com.example.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS from {@code blog.cors.allowed-origin-patterns},
 * or env {@code BLOG_CORS_ALLOWED_ORIGIN_PATTERNS} (comma-separated) when set.
 */
@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;
    private final Environment environment;

    public CorsConfig(CorsProperties corsProperties, Environment environment) {
        this.corsProperties = corsProperties;
        this.environment = environment;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        List<String> patterns = resolvePatterns();
        if (!patterns.isEmpty()) {
            config.setAllowedOriginPatterns(patterns);
        }
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    public List<String> resolvePatterns() {
        String csv = environment.getProperty("BLOG_CORS_ALLOWED_ORIGIN_PATTERNS");
        if (csv != null && !csv.isBlank()) {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        List<String> configured = corsProperties.getAllowedOriginPatterns();
        return configured == null ? List.of() : configured;
    }
}
