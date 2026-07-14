package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "blog.upload")
public class UploadProperties {

    private static final long DEFAULT_MAX_SIZE_BYTES = 8L * 1024 * 1024;
    private static final String DEFAULT_DIR = "./data/uploads";
    private static final String DEFAULT_PUBLIC_PATH_PREFIX = "/uploads";

    /**
     * Local storage root (relative to process cwd or absolute).
     */
    private String dir = DEFAULT_DIR;

    /**
     * Max upload size in bytes; {@code <= 0} falls back to 8 MiB.
     */
    private long maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;

    /**
     * Public URL path prefix without trailing slash.
     */
    private String publicPathPrefix = DEFAULT_PUBLIC_PATH_PREFIX;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getPublicPathPrefix() {
        return publicPathPrefix;
    }

    public void setPublicPathPrefix(String publicPathPrefix) {
        this.publicPathPrefix = publicPathPrefix;
    }

    public Path resolvedStorageRoot() {
        String raw = (dir == null || dir.isBlank()) ? DEFAULT_DIR : dir.trim();
        return Paths.get(raw).toAbsolutePath().normalize();
    }

    public long resolvedMaxSizeBytes() {
        return maxSizeBytes <= 0 ? DEFAULT_MAX_SIZE_BYTES : maxSizeBytes;
    }

    public String resolvedPublicPathPrefix() {
        String raw = (publicPathPrefix == null || publicPathPrefix.isBlank())
                ? DEFAULT_PUBLIC_PATH_PREFIX
                : publicPathPrefix.trim();
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        if (!raw.startsWith("/")) {
            raw = "/" + raw;
        }
        return raw.isEmpty() ? DEFAULT_PUBLIC_PATH_PREFIX : raw;
    }
}
