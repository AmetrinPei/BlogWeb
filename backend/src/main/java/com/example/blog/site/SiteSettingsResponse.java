package com.example.blog.site;

import java.time.LocalDateTime;
import java.util.List;

public class SiteSettingsResponse {

    private final String siteName;
    private final String tagline;
    private final String aboutText;
    private final List<SiteSettingsRequest.SocialLink> socialLinks;
    private final List<SiteSettingsRequest.FriendLink> friendLinks;
    private final String aboutTitle;
    private final String aboutDisplayName;
    private final List<String> aboutHighlights;
    private final String defaultTheme;
    private final String backgroundMode;
    private final String backgroundColor;
    private final String backgroundGradient;
    private final String backgroundImageUrl;
    private final String aboutAvatarUrl;
    private final String homeHeroUrl;
    private final boolean publicRegistrationEnabled;
    private final LocalDateTime updatedAt;

    public SiteSettingsResponse(
            String siteName,
            String tagline,
            String aboutText,
            List<SiteSettingsRequest.SocialLink> socialLinks,
            List<SiteSettingsRequest.FriendLink> friendLinks,
            String aboutTitle,
            String aboutDisplayName,
            List<String> aboutHighlights,
            String defaultTheme,
            String backgroundMode,
            String backgroundColor,
            String backgroundGradient,
            String backgroundImageUrl,
            String aboutAvatarUrl,
            String homeHeroUrl,
            boolean publicRegistrationEnabled,
            LocalDateTime updatedAt
    ) {
        this.siteName = siteName;
        this.tagline = tagline;
        this.aboutText = aboutText;
        this.socialLinks = socialLinks;
        this.friendLinks = friendLinks;
        this.aboutTitle = aboutTitle;
        this.aboutDisplayName = aboutDisplayName;
        this.aboutHighlights = aboutHighlights;
        this.defaultTheme = defaultTheme;
        this.backgroundMode = backgroundMode;
        this.backgroundColor = backgroundColor;
        this.backgroundGradient = backgroundGradient;
        this.backgroundImageUrl = backgroundImageUrl;
        this.aboutAvatarUrl = aboutAvatarUrl;
        this.homeHeroUrl = homeHeroUrl;
        this.publicRegistrationEnabled = publicRegistrationEnabled;
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

    public List<SiteSettingsRequest.FriendLink> getFriendLinks() {
        return friendLinks;
    }

    public String getAboutTitle() {
        return aboutTitle;
    }

    public String getAboutDisplayName() {
        return aboutDisplayName;
    }

    public List<String> getAboutHighlights() {
        return aboutHighlights;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public String getBackgroundMode() {
        return backgroundMode;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getBackgroundGradient() {
        return backgroundGradient;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public String getAboutAvatarUrl() {
        return aboutAvatarUrl;
    }

    public String getHomeHeroUrl() {
        return homeHeroUrl;
    }

    public boolean isPublicRegistrationEnabled() {
        return publicRegistrationEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
