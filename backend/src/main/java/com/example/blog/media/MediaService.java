package com.example.blog.media;

import com.example.blog.auth.CurrentUserService;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.config.UploadProperties;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int ORIGINAL_NAME_MAX = 200;

    private static final Map<String, String> EXT_TO_MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private static final Set<String> JPEG_MIME_ALIASES = Set.of("image/jpeg", "image/jpg");

    private final UploadProperties uploadProperties;
    private final CurrentUserService currentUserService;

    public MediaService(UploadProperties uploadProperties, CurrentUserService currentUserService) {
        this.uploadProperties = uploadProperties;
        this.currentUserService = currentUserService;
    }

    public MediaUploadResponse upload(MultipartFile file) {
        User user = currentUserService.requireUser();
        requireUploader(user);

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        long maxSize = uploadProperties.resolvedMaxSizeBytes();
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过 8MB");
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extKey = resolveExtensionKey(originalFilename);
        if (extKey == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的图片类型");
        }

        String expectedMime = EXT_TO_MIME.get(extKey);
        String declaredMime = normalizeContentType(file.getContentType());
        if (!mimeMatches(extKey, declaredMime)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的图片类型");
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(16);
        } catch (IOException e) {
            log.warn("Failed to read upload header, relativeKey pending");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求格式错误");
        }
        if (!magicMatches(extKey, header)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的图片类型");
        }

        String storedExt = "jpeg".equals(extKey) ? "jpg" : extKey;
        LocalDate today = LocalDate.now(ZONE);
        String relativeKey = String.format(
                "%04d/%02d/%s.%s",
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                storedExt
        );

        Path root = uploadProperties.resolvedStorageRoot();
        Path target = root.resolve(relativeKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的图片类型");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("Failed to store upload relativeKey={}", relativeKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
        }

        String url = uploadProperties.resolvedPublicPathPrefix() + "/" + relativeKey.replace('\\', '/');
        log.debug("Uploaded media relativeKey={} size={} userId={}", relativeKey, file.getSize(), user.getId());
        return new MediaUploadResponse(url, originalFilename, file.getSize(), expectedMime);
    }

    private static void requireUploader(User user) {
        UserRole role = user.getRole();
        if (role != UserRole.ADMIN && role != UserRole.AUTHOR) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要作者或管理员权限");
        }
    }

    static String sanitizeOriginalFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.length() > ORIGINAL_NAME_MAX) {
            name = name.substring(0, ORIGINAL_NAME_MAX);
        }
        return name;
    }

    static String resolveExtensionKey(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXT_TO_MIME.containsKey(ext) ? ext : null;
    }

    static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String mime = contentType.trim().toLowerCase(Locale.ROOT);
        int semi = mime.indexOf(';');
        if (semi >= 0) {
            mime = mime.substring(0, semi).trim();
        }
        return mime;
    }

    static boolean mimeMatches(String extKey, String declaredMime) {
        if (declaredMime == null || declaredMime.isEmpty()) {
            return false;
        }
        if ("jpg".equals(extKey) || "jpeg".equals(extKey)) {
            return JPEG_MIME_ALIASES.contains(declaredMime);
        }
        String expected = EXT_TO_MIME.get(extKey);
        return expected != null && expected.equals(declaredMime);
    }

    static boolean magicMatches(String extKey, byte[] header) {
        if (header == null || header.length < 3) {
            return false;
        }
        return switch (extKey) {
            case "jpg", "jpeg" -> header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
            case "png" -> header.length >= 8
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
            case "gif" -> header.length >= 6
                    && header[0] == 'G'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9')
                    && header[5] == 'a';
            case "webp" -> header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
            default -> false;
        };
    }
}
