package com.example.blog.auth;

import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String issue(Long userId) {
        String raw = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpireDays()));
        refreshTokenRepository.save(entity);
        return raw;
    }

    @Transactional
    public RefreshToken requireActive(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "refreshToken 不能为空");
        }
        RefreshToken entity = refreshTokenRepository.findByTokenHash(hash(rawToken.trim()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录"));
        if (!entity.isActive(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return entity;
    }

    @Transactional
    public String rotate(String rawToken) {
        RefreshToken current = requireActive(rawToken);
        current.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(current);
        return issue(current.getUserId());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken.trim())).ifPresent(entity -> {
            if (entity.getRevokedAt() == null) {
                entity.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(entity);
            }
        });
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
