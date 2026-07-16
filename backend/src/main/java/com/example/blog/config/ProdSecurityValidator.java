package com.example.blog.config;

import com.example.blog.auth.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fails fast on insecure production defaults (blog-prod-hardening).
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProdSecurityValidator implements ApplicationRunner {

    public static final String INSECURE_DEFAULT_JWT_SECRET =
            "blog-mvp-jwt-secret-key-change-me-32bytes-min";

    private final JwtProperties jwtProperties;
    private final AdminProperties adminProperties;
    private final BlogSiteProperties siteProperties;
    private final String dbPassword;

    public ProdSecurityValidator(
            JwtProperties jwtProperties,
            AdminProperties adminProperties,
            BlogSiteProperties siteProperties,
            @Value("${spring.datasource.password:}") String dbPassword
    ) {
        this.jwtProperties = jwtProperties;
        this.adminProperties = adminProperties;
        this.siteProperties = siteProperties;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateJwtSecret(jwtProperties.getSecret());
        validateAdminPassword(adminProperties.getPassword());
        validateDbPassword(dbPassword);
        validateSiteBaseUrl(siteProperties.getBaseUrl());
    }

    public static void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "prod profile requires JWT_SECRET (non-blank, >= 32 chars, not the dev default)");
        }
        if (INSECURE_DEFAULT_JWT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "prod profile rejects the development default JWT_SECRET; set a strong secret via env");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("prod profile requires JWT_SECRET length >= 32");
        }
    }

    public static void validateAdminPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("prod profile requires ADMIN_PASSWORD (non-blank)");
        }
        if ("admin123".equals(password)) {
            throw new IllegalStateException(
                    "prod profile rejects default ADMIN_PASSWORD admin123; set a strong password via env");
        }
    }

    public static void validateDbPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("prod profile requires DB_PASSWORD (non-blank)");
        }
        if ("root".equals(password)) {
            throw new IllegalStateException(
                    "prod profile rejects default DB_PASSWORD root; set a strong password via env");
        }
    }

    public static void validateSiteBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("prod profile requires BLOG_SITE_BASE_URL");
        }
        String lower = baseUrl.trim().toLowerCase();
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "prod profile rejects localhost BLOG_SITE_BASE_URL; set the public HTTPS origin");
        }
    }
}
