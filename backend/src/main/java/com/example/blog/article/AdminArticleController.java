package com.example.blog.article;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Result<PageResult<ArticleResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) ArticleStatus status
    ) {
        return Result.ok(articleService.listAdmin(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<ArticleResponse> detail(@PathVariable Long id) {
        return Result.ok(articleService.getAdmin(id));
    }

    @PostMapping
    public Result<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        return Result.ok(articleService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ArticleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request
    ) {
        return Result.ok(articleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.ok();
    }
}
