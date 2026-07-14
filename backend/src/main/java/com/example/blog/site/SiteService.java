package com.example.blog.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SiteService {

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;

    public SiteService(SiteSettingsRepository siteSettingsRepository, ObjectMapper objectMapper) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.objectMapper = objectMapper;
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
        settings.setAboutText(request.getAboutText() == null ? "" : request.getAboutText());
        try {
            settings.setSocialLinks(objectMapper.writeValueAsString(
                    request.getSocialLinks() == null ? List.of() : request.getSocialLinks()));
        } catch (Exception ex) {
            settings.setSocialLinks("[]");
        }
        return toResponse(siteSettingsRepository.save(settings));
    }

    @Transactional
    public SiteSettings requireSettings() {
        return siteSettingsRepository.findById(1L).orElseGet(() -> {
            SiteSettings created = new SiteSettings();
            created.setId(1L);
            return siteSettingsRepository.save(created);
        });
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        List<SiteSettingsRequest.SocialLink> links = parseLinks(settings.getSocialLinks());
        return new SiteSettingsResponse(
                settings.getSiteName(),
                settings.getTagline(),
                settings.getAboutText(),
                links,
                settings.getUpdatedAt()
        );
    }

    private List<SiteSettingsRequest.SocialLink> parseLinks(String json) {
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
}
