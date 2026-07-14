package com.example.blog.user;

import com.example.blog.auth.CurrentUserService;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ProfileService(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    public ProfileResponse getMe() {
        return ProfileResponse.from(currentUserService.requireUser());
    }

    @Transactional
    public ProfileResponse updateMe(ProfileUpdateRequest request) {
        User user = currentUserService.requireUser();
        user.setDisplayName(normalizeDisplayName(request.getDisplayName()));
        user.setBio(normalizeBio(request.getBio()));
        user.setAvatarUrl(normalizeAvatarUrl(request.getAvatarUrl()));
        userRepository.save(user);
        return ProfileResponse.from(user);
    }

    private String normalizeDisplayName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "展示名不能超过 32 个字符");
        }
        return trimmed;
    }

    private String normalizeBio(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 200) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "简介不能超过 200 个字符");
        }
        return trimmed;
    }

    private String normalizeAvatarUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 512) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像 URL 不能超过 512 个字符");
        }
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("\n")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像 URL 格式不正确");
        }
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像 URL 格式不正确");
        }
        boolean ok = lower.startsWith("http://")
                || lower.startsWith("https://")
                || trimmed.startsWith("/");
        if (!ok) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像 URL 须为 http(s) 或站点相对路径");
        }
        return trimmed;
    }
}
