package com.example.blog.tag;

import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<TagResponse>> list() {
        return Result.ok(tagService.list());
    }

    @PostMapping
    public Result<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return Result.ok(tagService.create(request));
    }

    @PutMapping("/{id}")
    public Result<TagResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request
    ) {
        return Result.ok(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
