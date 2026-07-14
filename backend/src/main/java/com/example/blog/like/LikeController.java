package com.example.blog.like;

import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/toggle")
    public Result<LikeToggleResponse> toggle(@Valid @RequestBody LikeToggleRequest request) {
        return Result.ok(likeService.toggle(request));
    }

    @GetMapping("/status")
    public Result<LikeToggleResponse> status(
            @RequestParam LikeTargetType targetType,
            @RequestParam Long targetId
    ) {
        return Result.ok(likeService.status(targetType, targetId));
    }
}
