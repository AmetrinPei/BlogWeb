package com.example.blog.site;

import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping("/api/site")
    public Result<SiteSettingsResponse> get() {
        return Result.ok(siteService.get());
    }

    @PutMapping("/api/admin/site")
    public Result<SiteSettingsResponse> update(@Valid @RequestBody SiteSettingsRequest request) {
        return Result.ok(siteService.update(request));
    }
}
