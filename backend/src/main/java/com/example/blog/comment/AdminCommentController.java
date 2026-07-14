package com.example.blog.comment;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Result<PageResult<AdminCommentResponse>> list(
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return Result.ok(commentService.listAdmin(status, page, size));
    }

    @PutMapping("/{id}/status")
    public Result<AdminCommentResponse> moderate(
            @PathVariable Long id,
            @Valid @RequestBody CommentModerationRequest request
    ) {
        return Result.ok(commentService.moderate(id, request));
    }
}
