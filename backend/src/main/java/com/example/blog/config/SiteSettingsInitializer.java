package com.example.blog.config;

import com.example.blog.site.SiteService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class SiteSettingsInitializer implements ApplicationRunner {

    private final SiteService siteService;

    public SiteSettingsInitializer(SiteService siteService) {
        this.siteService = siteService;
    }

    @Override
    public void run(ApplicationArguments args) {
        siteService.requireSettings();
    }
}
