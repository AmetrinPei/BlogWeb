package com.example.blog.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class SiteSettingsRequest {

    @NotBlank(message = "siteName 不能为空")
    @Size(max = 100, message = "siteName 长度不能超过 100")
    private String siteName;

    @Size(max = 200, message = "tagline 长度不能超过 200")
    private String tagline;

    private String aboutText;

    private List<SocialLink> socialLinks = new ArrayList<>();

    /** Optional; null keeps existing value. */
    private List<FriendLink> friendLinks;

    /** Optional; null keeps existing value. */
    @Size(max = 100, message = "aboutTitle 长度不能超过 100")
    private String aboutTitle;

    /** Optional; null keeps existing value. */
    @Size(max = 50, message = "aboutDisplayName 长度不能超过 50")
    private String aboutDisplayName;

    /** Optional; null keeps existing value. */
    private List<String> aboutHighlights;

    /** Optional; null keeps existing value. */
    @Size(max = 16)
    private String defaultTheme;

    /** Optional; null keeps existing value. */
    @Size(max = 16)
    private String backgroundMode;

    @Size(max = 32)
    private String backgroundColor;

    @Size(max = 32)
    private String backgroundGradient;

    @Size(max = 512, message = "backgroundImageUrl 长度不能超过 512")
    private String backgroundImageUrl;

    @Size(max = 512, message = "aboutAvatarUrl 长度不能超过 512")
    private String aboutAvatarUrl;

    @Size(max = 512, message = "homeHeroUrl 长度不能超过 512")
    private String homeHeroUrl;

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

    public List<SocialLink> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLink> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public List<FriendLink> getFriendLinks() {
        return friendLinks;
    }

    public void setFriendLinks(List<FriendLink> friendLinks) {
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

    public List<String> getAboutHighlights() {
        return aboutHighlights;
    }

    public void setAboutHighlights(List<String> aboutHighlights) {
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

    public static class SocialLink {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class FriendLink {
        private String name;
        private String url;
        private String description;
        private Integer sortOrder;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
