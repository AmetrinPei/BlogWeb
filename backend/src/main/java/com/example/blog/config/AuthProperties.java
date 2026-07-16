package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.auth")
public class AuthProperties {

    /**
     * When false, {@code POST /api/auth/register} is rejected in Service layer.
     */
    private boolean publicRegistrationEnabled = true;

    public boolean isPublicRegistrationEnabled() {
        return publicRegistrationEnabled;
    }

    public void setPublicRegistrationEnabled(boolean publicRegistrationEnabled) {
        this.publicRegistrationEnabled = publicRegistrationEnabled;
    }
}
