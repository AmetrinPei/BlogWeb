package com.example.blog.article;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Result<PageResult<ArticleResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String yearMonth
    ) {
        return Result.ok(articleService.listPublished(page, size, categoryId, tagId, keyword, yearMonth));
    }

    @GetMapping("/featured")
    public Result<List<ArticleResponse>> featured(@RequestParam(required = false) Integer size) {
        return Result.ok(articleService.listFeatured(size));
    }

    @GetMapping("/archive")
    public Result<List<ArchiveMonthResponse>> archive() {
        return Result.ok(articleService.listArchive());
    }

    @GetMapping("/{id}")
    public Result<ArticleResponse> detail(@PathVariable Long id) {
        return Result.ok(articleService.getPublished(id));
    }
}
