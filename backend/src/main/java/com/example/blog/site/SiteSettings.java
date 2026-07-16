package com.example.blog.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
public class SiteSettings {

    @Id
    private Long id = 1L;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName = "My Blog";

    @Column(nullable = false, length = 200)
    private String tagline = "欢迎来到我的小宇宙";

    @Column(name = "about_text", columnDefinition = "TEXT")
    private String aboutText = "";

    @Column(name = "social_links", columnDefinition = "TEXT")
    private String socialLinks = "[]";

    @Column(name = "friend_links", columnDefinition = "TEXT")
    private String friendLinks = "[]";

    @Column(name = "about_title", length = 100)
    private String aboutTitle;

    @Column(name = "about_display_name", length = 50)
    private String aboutDisplayName;

    @Column(name = "about_highlights", columnDefinition = "TEXT")
    private String aboutHighlights = "[]";

    @Column(name = "default_theme", length = 16)
    private String defaultTheme = "light";

    @Column(name = "background_mode", length = 16)
    private String backgroundMode = "theme";

    @Column(name = "background_color", length = 32)
    private String backgroundColor;

    @Column(name = "background_gradient", length = 32)
    private String backgroundGradient;

    @Column(name = "background_image_url", length = 512)
    private String backgroundImageUrl;

    @Column(name = "about_avatar_url", length = 512)
    private String aboutAvatarUrl;

    @Column(name = "home_hero_url", length = 512)
    private String homeHeroUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getAboutText() {
        return aboutText;
    }

    public void setAboutText(String aboutText) {
        this.aboutText = aboutText;
    }

    public String getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(String socialLinks) {
        this.socialLinks = socialLinks;
    }

    public String getFriendLinks() {
        return friendLinks;
    }

    public void setFriendLinks(String friendLinks) {
        this.friendLinks = friendLinks;
    }

    public String getAboutTitle() {
        return aboutTitle;
    }

    public void setAboutTitle(String aboutTitle) {
        this.aboutTitle = aboutTitle;
    }

    public String getAboutDisplayName() {
        return aboutDisplayName;
    }

    public void setAboutDisplayName(String aboutDisplayName) {
        this.aboutDisplayName = aboutDisplayName;
    }

    public String getAboutHighlights() {
        return aboutHighlights;
    }

    public void setAboutHighlights(String aboutHighlights) {
        this.aboutHighlights = aboutHighlights;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public void setDefaultTheme(String defaultTheme) {
        this.defaultTheme = defaultTheme;
    }

    public String getBackgroundMode() {
        return backgroundMode;
    }

    public void setBackgroundMode(String backgroundMode) {
        this.backgroundMode = backgroundMode;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundGradient() {
        return backgroundGradient;
    }

    public void setBackgroundGradient(String backgroundGradient) {
        this.backgroundGradient = backgroundGradient;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public String getAboutAvatarUrl() {
        return aboutAvatarUrl;
    }

    public void setAboutAvatarUrl(String aboutAvatarUrl) {
        this.aboutAvatarUrl = aboutAvatarUrl;
    }

    public String getHomeHeroUrl() {
        return homeHeroUrl;
    }

    public void setHomeHeroUrl(String homeHeroUrl) {
        this.homeHeroUrl = homeHeroUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
