package com.example.blog.like;

import com.example.blog.article.ArticleService;
import com.example.blog.auth.CurrentUserService;
import com.example.blog.comment.Comment;
import com.example.blog.comment.CommentRepository;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final CurrentUserService currentUserService;
    private final ArticleService articleService;
    private final CommentRepository commentRepository;

    public LikeService(
            LikeRepository likeRepository,
            CurrentUserService currentUserService,
            ArticleService articleService,
            CommentRepository commentRepository
    ) {
        this.likeRepository = likeRepository;
        this.currentUserService = currentUserService;
        this.articleService = articleService;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public LikeToggleResponse toggle(LikeToggleRequest request) {
        User user = currentUserService.requireUser();
        validateTarget(request.getTargetType(), request.getTargetId());

        var existing = likeRepository.findByUser_IdAndTargetTypeAndTargetId(
                user.getId(), request.getTargetType(), request.getTargetId());

        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            LikeEntity entity = new LikeEntity();
            entity.setUser(user);
            entity.setTargetType(request.getTargetType());
            entity.setTargetId(request.getTargetId());
            likeRepository.save(entity);
            liked = true;
        }

        long count = likeRepository.countByTargetTypeAndTargetId(request.getTargetType(), request.getTargetId());
        return new LikeToggleResponse(liked, count);
    }

    @Transactional(readOnly = true)
    public LikeToggleResponse status(LikeTargetType targetType, Long targetId) {
        validateTarget(targetType, targetId);
        long count = likeRepository.countByTargetTypeAndTargetId(targetType, targetId);
        boolean liked = false;
        try {
            User user = currentUserService.requireUser();
            liked = likeRepository.existsByUser_IdAndTargetTypeAndTargetId(user.getId(), targetType, targetId);
        } catch (BusinessException ignored) {
            // visitor
        }
        return new LikeToggleResponse(liked, count);
    }

    private void validateTarget(LikeTargetType type, Long targetId) {
        if (type == LikeTargetType.ARTICLE) {
            articleService.requirePublishedArticle(targetId);
        } else if (type == LikeTargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));
            articleService.requirePublishedArticle(comment.getArticle().getId());
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的点赞目标类型");
        }
    }
}
