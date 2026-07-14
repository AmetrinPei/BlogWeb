package com.example.blog.site;

import java.time.LocalDateTime;
import java.util.List;

public class SiteSettingsResponse {

    private final String siteName;
    private final String tagline;
    private final String aboutText;
    private final List<SiteSettingsRequest.SocialLink> socialLinks;
    private final LocalDateTime updatedAt;

    public SiteSettingsResponse(
            String siteName,
            String tagline,
            String aboutText,
            List<SiteSettingsRequest.SocialLink> socialLinks,
            LocalDateTime updatedAt
    ) {
        this.siteName = siteName;
        this.tagline = tagline;
        this.aboutText = aboutText;
        this.socialLinks = socialLinks;
        this.updatedAt = updatedAt;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getTagline() {
        return tagline;
    }

    public String getAboutText() {
        return aboutText;
    }

    public List<SiteSettingsRequest.SocialLink> getSocialLinks() {
        return socialLinks;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
