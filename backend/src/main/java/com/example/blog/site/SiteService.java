package com.example.blog.site;

import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.config.AuthProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SiteService {

    private static final int MAX_FRIEND_LINKS = 50;
    private static final int MAX_HIGHLIGHTS = 10;
    private static final int MAX_HIGHLIGHT_LEN = 100;
    private static final int MAX_FRIEND_NAME = 50;
    private static final int MAX_FRIEND_URL = 512;
    private static final int MAX_FRIEND_DESC = 200;
    private static final int MAX_ABOUT_TEXT = 5000;
    private static final int MAX_ABOUT_TITLE = 100;
    private static final int MAX_ABOUT_DISPLAY_NAME = 50;

    private static final Set<String> THEMES = Set.of("light", "dark");
    private static final Set<String> BACKGROUND_MODES = Set.of("theme", "color", "gradient", "image");
    private static final Set<String> GRADIENTS = Set.of("mint-wash", "lilac-mist", "peach-glow");
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public SiteService(
            SiteSettingsRepository siteSettingsRepository,
            ObjectMapper objectMapper,
            AuthProperties authProperties
    ) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
    }

    @Transactional(readOnly = true)
    public SiteSettingsResponse get() {
        return toResponse(requireSettings());
    }

    @Transactional
    public SiteSettingsResponse update(SiteSettingsRequest request) {
        SiteSettings settings = requireSettings();
        settings.setSiteName(request.getSiteName().trim());
        settings.setTagline(request.getTagline() == null ? "" : request.getTagline().trim());

        String aboutText = request.getAboutText() == null ? "" : request.getAboutText();
        if (aboutText.length() > MAX_ABOUT_TEXT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "aboutText 长度不能超过 " + MAX_ABOUT_TEXT);
        }
        settings.setAboutText(aboutText);

        try {
            settings.setSocialLinks(objectMapper.writeValueAsString(
                    request.getSocialLinks() == null ? List.of() : request.getSocialLinks()));
        } catch (Exception ex) {
            settings.setSocialLinks("[]");
        }

        if (request.getFriendLinks() != null) {
            List<SiteSettingsRequest.FriendLink> normalized = normalizeFriendLinks(request.getFriendLinks());
            try {
                settings.setFriendLinks(objectMapper.writeValueAsString(normalized));
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "friendLinks 无法序列化");
            }
        }

        if (request.getAboutTitle() != null) {
            String title = request.getAboutTitle().trim();
            if (title.length() > MAX_ABOUT_TITLE) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "aboutTitle 长度不能超过 " + MAX_ABOUT_TITLE);
            }
            settings.setAboutTitle(title.isEmpty() ? "" : title);
        }

        if (request.getAboutDisplayName() != null) {
            String displayName = request.getAboutDisplayName().trim();
            if (displayName.length() > MAX_ABOUT_DISPLAY_NAME) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "aboutDisplayName 长度不能超过 " + MAX_ABOUT_DISPLAY_NAME);
            }
            settings.setAboutDisplayName(displayName.isEmpty() ? "" : displayName);
        }

        if (request.getAboutHighlights() != null) {
            List<String> highlights = normalizeHighlights(request.getAboutHighlights());
            try {
                settings.setAboutHighlights(objectMapper.writeValueAsString(highlights));
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "aboutHighlights 无法序列化");
            }
        }

        if (request.getDefaultTheme() != null) {
            settings.setDefaultTheme(normalizeTheme(request.getDefaultTheme()));
        }
        if (request.getBackgroundMode() != null) {
            settings.setBackgroundMode(normalizeBackgroundMode(request.getBackgroundMode()));
        }
        if (request.getBackgroundColor() != null) {
            String color = request.getBackgroundColor().trim();
            settings.setBackgroundColor(color.isEmpty() ? null : normalizeColor(color));
        }
        if (request.getBackgroundGradient() != null) {
            String gradient = request.getBackgroundGradient().trim();
            settings.setBackgroundGradient(gradient.isEmpty() ? null : normalizeGradient(gradient));
        }
        if (request.getBackgroundImageUrl() != null) {
            settings.setBackgroundImageUrl(normalizeOptionalUrl(request.getBackgroundImageUrl(), "backgroundImageUrl"));
        }
        if (request.getAboutAvatarUrl() != null) {
            settings.setAboutAvatarUrl(normalizeOptionalUrl(request.getAboutAvatarUrl(), "aboutAvatarUrl"));
        }
        if (request.getHomeHeroUrl() != null) {
            settings.setHomeHeroUrl(normalizeOptionalUrl(request.getHomeHeroUrl(), "homeHeroUrl"));
        }

        validateBackgroundConsistency(settings);
        return toResponse(siteSettingsRepository.save(settings));
    }

    @Transactional
    public SiteSettings requireSettings() {
        return siteSettingsRepository.findById(1L).orElseGet(() -> {
            SiteSettings created = new SiteSettings();
            created.setId(1L);
            created.setDefaultTheme("light");
            created.setBackgroundMode("theme");
            created.setFriendLinks("[]");
            created.setAboutHighlights("[]");
            return siteSettingsRepository.save(created);
        });
    }

    private List<SiteSettingsRequest.FriendLink> normalizeFriendLinks(
            List<SiteSettingsRequest.FriendLink> raw
    ) {
        if (raw.size() > MAX_FRIEND_LINKS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "友链最多 " + MAX_FRIEND_LINKS + " 条");
        }
        List<SiteSettingsRequest.FriendLink> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            SiteSettingsRequest.FriendLink item = raw.get(i);
            if (item == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "friendLinks 含无效项");
            }
            String name = item.getName() == null ? "" : item.getName().trim();
            if (name.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "友链名称不能为空");
            }
            if (name.length() > MAX_FRIEND_NAME) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "友链名称长度不能超过 " + MAX_FRIEND_NAME);
            }
            String url = normalizeFriendUrl(item.getUrl());
            String description = item.getDescription() == null ? "" : item.getDescription().trim();
            if (description.length() > MAX_FRIEND_DESC) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "友链简介长度不能超过 " + MAX_FRIEND_DESC);
            }

            SiteSettingsRequest.FriendLink normalized = new SiteSettingsRequest.FriendLink();
            normalized.setName(name);
            normalized.setUrl(url);
            normalized.setDescription(description);
            normalized.setSortOrder(i);
            result.add(normalized);
        }
        return result;
    }

    private String normalizeFriendUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "友链 URL 不能为空");
        }
        String url = raw.trim();
        if (url.length() > MAX_FRIEND_URL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "友链 URL 长度不能超过 " + MAX_FRIEND_URL);
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "友链 URL 含有不支持的 scheme");
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return url;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "友链 URL 须为 http(s) 地址");
    }

    private List<String> normalizeHighlights(List<String> raw) {
        List<String> filtered = new ArrayList<>();
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String text = item.trim();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_HIGHLIGHT_LEN) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "亮点单条长度不能超过 " + MAX_HIGHLIGHT_LEN);
            }
            filtered.add(text);
        }
        if (filtered.size() > MAX_HIGHLIGHTS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "亮点最多 " + MAX_HIGHLIGHTS + " 条");
        }
        return filtered;
    }

    private void validateBackgroundConsistency(SiteSettings settings) {
        String mode = settings.getBackgroundMode() == null ? "theme" : settings.getBackgroundMode();
        switch (mode) {
            case "color" -> {
                if (settings.getBackgroundColor() == null || settings.getBackgroundColor().isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "color 模式须提供 backgroundColor");
                }
                normalizeColor(settings.getBackgroundColor());
            }
            case "gradient" -> {
                if (settings.getBackgroundGradient() == null || settings.getBackgroundGradient().isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "gradient 模式须提供 backgroundGradient");
                }
                normalizeGradient(settings.getBackgroundGradient());
            }
            case "image" -> {
                if (settings.getBackgroundImageUrl() == null || settings.getBackgroundImageUrl().isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "image 模式须提供 backgroundImageUrl");
                }
                normalizeOptionalUrl(settings.getBackgroundImageUrl(), "backgroundImageUrl");
            }
            default -> {
                // theme: no extra resource required
            }
        }
    }

    private String normalizeTheme(String raw) {
        String theme = raw.trim().toLowerCase(Locale.ROOT);
        if (!THEMES.contains(theme)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "defaultTheme 仅支持 light 或 dark");
        }
        return theme;
    }

    private String normalizeBackgroundMode(String raw) {
        String mode = raw.trim().toLowerCase(Locale.ROOT);
        if (!BACKGROUND_MODES.contains(mode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "backgroundMode 不合法");
        }
        return mode;
    }

    private String normalizeColor(String raw) {
        String color = raw.trim();
        if (!HEX_COLOR.matcher(color).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "backgroundColor 须为 #RRGGBB");
        }
        return color.toUpperCase(Locale.ROOT);
    }

    private String normalizeGradient(String raw) {
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!GRADIENTS.contains(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "backgroundGradient 不在允许的预设列表中");
        }
        return id;
    }

    /**
     * Empty string clears the URL (stores null). Non-empty must be /uploads/... or http(s).
     */
    private String normalizeOptionalUrl(String raw, String field) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (url.length() > 512) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 长度不能超过 512");
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 含有不支持的 URL scheme");
        }
        if (url.startsWith("/")) {
            return url;
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return url;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 须为相对路径或 http(s) URL");
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        List<SiteSettingsRequest.SocialLink> links = parseSocialLinks(settings.getSocialLinks());
        List<SiteSettingsRequest.FriendLink> friends = parseFriendLinks(settings.getFriendLinks());
        List<String> highlights = parseHighlights(settings.getAboutHighlights());
        return new SiteSettingsResponse(
                settings.getSiteName(),
                settings.getTagline(),
                settings.getAboutText() == null ? "" : settings.getAboutText(),
                links,
                friends,
                settings.getAboutTitle(),
                settings.getAboutDisplayName(),
                highlights,
                settings.getDefaultTheme() == null ? "light" : settings.getDefaultTheme(),
                settings.getBackgroundMode() == null ? "theme" : settings.getBackgroundMode(),
                settings.getBackgroundColor(),
                settings.getBackgroundGradient(),
                settings.getBackgroundImageUrl(),
                settings.getAboutAvatarUrl(),
                settings.getHomeHeroUrl(),
                authProperties.isPublicRegistrationEnabled(),
                settings.getUpdatedAt()
        );
    }

    private List<SiteSettingsRequest.SocialLink> parseSocialLinks(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<SiteSettingsRequest.FriendLink> parseFriendLinks(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<SiteSettingsRequest.FriendLink> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            list.sort(Comparator
                    .comparing((SiteSettingsRequest.FriendLink f) -> f.getSortOrder() == null ? 0 : f.getSortOrder())
                    .thenComparing(f -> f.getName() == null ? "" : f.getName()));
            return list;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<String> parseHighlights(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
