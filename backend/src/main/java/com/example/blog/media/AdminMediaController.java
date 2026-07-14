package com.example.blog.media;

import com.example.blog.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping
    public Result<MediaUploadResponse> upload(@RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.ok(mediaService.upload(file));
    }
}
