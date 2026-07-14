package com.example.blog.config;

import com.example.blog.auth.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AdminProperties.class, JwtProperties.class, BlogSiteProperties.class})
public class AppConfig {
}
