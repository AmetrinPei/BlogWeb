package com.example.blog.comment;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleService;
import com.example.blog.auth.CurrentUserService;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.like.LikeRepository;
import com.example.blog.like.LikeTargetType;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleService articleService;
    private final CurrentUserService currentUserService;
    private final LikeRepository likeRepository;

    public CommentService(
            CommentRepository commentRepository,
            ArticleService articleService,
            CurrentUserService currentUserService,
            LikeRepository likeRepository
    ) {
        this.commentRepository = commentRepository;
        this.articleService = articleService;
        this.currentUserService = currentUserService;
        this.likeRepository = likeRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByArticle(Long articleId) {
        articleService.requirePublishedArticle(articleId);
        Long currentUserId = currentUserIdOrNull();
        return commentRepository.findByArticle_IdOrderByCreatedAtAsc(articleId).stream()
                .map(c -> toResponse(c, currentUserId))
                .toList();
    }

    @Transactional
    public CommentResponse create(Long articleId, CommentRequest request) {
        User user = currentUserService.requireUser();
        Article article = articleService.requirePublishedArticle(articleId);
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(user);
        comment.setContent(request.getContent().trim());
        Comment saved = commentRepository.save(comment);
        return toResponse(saved, user.getId());
    }

    @Transactional
    public void delete(Long commentId) {
        User user = currentUserService.requireUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));
        if (user.getRole() != UserRole.ADMIN && !comment.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }
        likeRepository.deleteByTargetTypeAndTargetId(LikeTargetType.COMMENT, commentId);
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment, Long currentUserId) {
        long likeCount = likeRepository.countByTargetTypeAndTargetId(LikeTargetType.COMMENT, comment.getId());
        boolean likedByMe = currentUserId != null
                && likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                currentUserId, LikeTargetType.COMMENT, comment.getId());
        return new CommentResponse(
                comment.getId(),
                comment.getArticle().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getCreatedAt(),
                likeCount,
                likedByMe
        );
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return currentUserService.requireUser().getId();
        } catch (BusinessException ex) {
            return null;
        }
    }
}
