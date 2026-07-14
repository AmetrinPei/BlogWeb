package com.example.blog.user;

import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public Result<ProfileResponse> getMe() {
        return Result.ok(profileService.getMe());
    }

    @PutMapping
    public Result<ProfileResponse> updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(profileService.updateMe(request));
    }
}
