package com.example.blog.comment;

import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/articles/{articleId}/comments")
    public Result<List<CommentResponse>> list(@PathVariable Long articleId) {
        return Result.ok(commentService.listByArticle(articleId));
    }

    @PostMapping("/articles/{articleId}/comments")
    public Result<CommentResponse> create(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentRequest request
    ) {
        return Result.ok(commentService.create(articleId, request));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}
