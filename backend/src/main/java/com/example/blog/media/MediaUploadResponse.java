package com.example.blog.media;

public class MediaUploadResponse {

    private final String url;
    private final String originalFilename;
    private final long size;
    private final String contentType;

    public MediaUploadResponse(String url, String originalFilename, long size, String contentType) {
        this.url = url;
        this.originalFilename = originalFilename;
        this.size = size;
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }
}
