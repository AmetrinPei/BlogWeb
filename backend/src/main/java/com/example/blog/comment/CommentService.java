package com.example.blog.comment;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleService;
import com.example.blog.auth.CurrentUserService;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.common.PageResult;
import com.example.blog.config.CommentProperties;
import com.example.blog.like.LikeRepository;
import com.example.blog.like.LikeTargetType;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final ArticleService articleService;
    private final CurrentUserService currentUserService;
    private final LikeRepository likeRepository;
    private final SensitiveWordService sensitiveWordService;
    private final CommentProperties commentProperties;

    public CommentService(
            CommentRepository commentRepository,
            ArticleService articleService,
            CurrentUserService currentUserService,
            LikeRepository likeRepository,
            SensitiveWordService sensitiveWordService,
            CommentProperties commentProperties
    ) {
        this.commentRepository = commentRepository;
        this.articleService = articleService;
        this.currentUserService = currentUserService;
        this.likeRepository = likeRepository;
        this.sensitiveWordService = sensitiveWordService;
        this.commentProperties = commentProperties;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByArticle(Long articleId) {
        articleService.requirePublishedArticle(articleId);
        Long currentUserId = currentUserIdOrNull();
        List<Comment> approved = commentRepository.findByArticle_IdAndStatusOrderByCreatedAtAsc(
                articleId, CommentStatus.APPROVED);

        Map<Long, List<Comment>> repliesByParentId = new LinkedHashMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment comment : approved) {
            if (comment.getParent() == null) {
                roots.add(comment);
            } else {
                repliesByParentId
                        .computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>())
                        .add(comment);
            }
        }

        // Floor by chronological order among APPROVED roots (stable; independent of pin).
        Map<Long, Integer> floorByRootId = new LinkedHashMap<>();
        int floor = 1;
        for (Comment root : roots) {
            floorByRootId.put(root.getId(), floor++);
        }

        List<Comment> displayRoots = new ArrayList<>(roots);
        displayRoots.sort((a, b) -> {
            if (a.isPinned() != b.isPinned()) {
                return a.isPinned() ? -1 : 1;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        Set<Long> rootIds = roots.stream().map(Comment::getId).collect(Collectors.toSet());

        return displayRoots.stream()
                .map(root -> {
                    List<Comment> replies = repliesByParentId.getOrDefault(root.getId(), List.of()).stream()
                            .filter(reply -> rootIds.contains(
                                    reply.getParent() == null ? null : reply.getParent().getId()))
                            .toList();
                    return toResponse(root, currentUserId, replies, floorByRootId.get(root.getId()));
                })
                .toList();
    }

    @Transactional
    public CommentResponse create(Long articleId, CommentRequest request) {
        User user = currentUserService.requireUser();
        Article article = articleService.requirePublishedArticle(articleId);

        assertWithinRateLimit(user.getId());

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父评论不存在"));
            if (!parent.getArticle().getId().equals(article.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父评论不属于该文章");
            }
            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "仅允许回复一级评论");
            }
            if (parent.getStatus() != CommentStatus.APPROVED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "只能回复已通过审核的评论");
            }
        }

        String content = request.getContent().trim();
        boolean hit = sensitiveWordService.hits(content);
        CommentStatus status = hit ? CommentStatus.PENDING : CommentStatus.APPROVED;

        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(user);
        comment.setParent(parent);
        comment.setContent(content);
        comment.setStatus(status);
        Comment saved = commentRepository.save(comment);

        log.info(
                "comment created id={} articleId={} userId={} status={} sensitiveHit={}",
                saved.getId(),
                article.getId(),
                user.getId(),
                status,
                hit
        );

        Integer floorNo = null;
        if (saved.getParent() == null && status == CommentStatus.APPROVED) {
            floorNo = resolveFloorNo(article.getId(), saved.getId());
        }
        return toResponse(saved, user.getId(), List.of(), floorNo);
    }

    @Transactional
    public CommentResponse pin(Long commentId, CommentPinRequest request) {
        User user = currentUserService.requireUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));

        if (comment.getParent() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅一级评论可置顶");
        }
        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过审核的评论可置顶");
        }

        Article article = comment.getArticle();
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isArticleAuthor = article.getAuthor() != null
                && article.getAuthor().getId().equals(user.getId());
        if (!isAdmin && !isArticleAuthor) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权置顶该评论");
        }

        boolean wantPinned = Boolean.TRUE.equals(request.getPinned());
        if (wantPinned) {
            commentRepository.clearPinnedRootsByArticleId(article.getId());
            comment.setPinned(true);
        } else {
            comment.setPinned(false);
        }
        Comment saved = commentRepository.save(comment);

        Integer floorNo = resolveFloorNo(article.getId(), saved.getId());
        return toResponse(saved, user.getId(), List.of(), floorNo);
    }

    private Integer resolveFloorNo(Long articleId, Long rootId) {
        List<Comment> approved = commentRepository.findByArticle_IdAndStatusOrderByCreatedAtAsc(
                articleId, CommentStatus.APPROVED);
        int floor = 1;
        for (Comment c : approved) {
            if (c.getParent() != null) {
                continue;
            }
            if (c.getId().equals(rootId)) {
                return floor;
            }
            floor++;
        }
        return null;
    }

    @Transactional
    public void delete(Long commentId) {
        User user = currentUserService.requireUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));
        if (user.getRole() != UserRole.ADMIN && !comment.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }

        if (comment.getParent() == null) {
            List<Comment> children = commentRepository.findByParent_IdOrderByCreatedAtAsc(commentId);
            for (Comment child : children) {
                likeRepository.deleteByTargetTypeAndTargetId(LikeTargetType.COMMENT, child.getId());
                commentRepository.delete(child);
            }
        }

        likeRepository.deleteByTargetTypeAndTargetId(LikeTargetType.COMMENT, commentId);
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public PageResult<AdminCommentResponse> listAdmin(CommentStatus status, Integer page, Integer size) {
        CommentStatus filter = status == null ? CommentStatus.PENDING : status;
        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);
        Page<Comment> result = commentRepository.findByStatusOrderByCreatedAtDesc(
                filter,
                PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<AdminCommentResponse> items = result.getContent().stream()
                .map(AdminCommentResponse::from)
                .toList();
        return new PageResult<>(items, pageNumber, pageSize, result.getTotalElements());
    }

    @Transactional
    public AdminCommentResponse moderate(Long commentId, CommentModerationRequest request) {
        CommentStatus target = request.getStatus();
        if (target != CommentStatus.APPROVED && target != CommentStatus.REJECTED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的审核状态");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));
        comment.setStatus(target);
        Comment saved = commentRepository.save(comment);
        log.info(
                "comment moderated id={} articleId={} userId={} status={}",
                saved.getId(),
                saved.getArticle().getId(),
                saved.getUser().getId(),
                target
        );
        return AdminCommentResponse.from(saved);
    }

    private void assertWithinRateLimit(Long userId) {
        int limit = commentProperties.getRateLimitPerMinute();
        if (limit <= 0) {
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        long recent = commentRepository.countByUser_IdAndCreatedAtAfter(userId, since);
        if (recent >= limit) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论过于频繁，请稍后再试");
        }
    }

    private CommentResponse toResponse(
            Comment comment,
            Long currentUserId,
            List<Comment> replies,
            Integer floorNo
    ) {
        long likeCount = likeRepository.countByTargetTypeAndTargetId(LikeTargetType.COMMENT, comment.getId());
        boolean likedByMe = currentUserId != null
                && likeRepository.existsByUser_IdAndTargetTypeAndTargetId(
                currentUserId, LikeTargetType.COMMENT, comment.getId());
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        List<CommentResponse> replyResponses = replies.stream()
                .filter(reply -> Objects.equals(
                        reply.getParent() == null ? null : reply.getParent().getId(),
                        comment.getId()))
                .map(reply -> toResponse(reply, currentUserId, List.of(), null))
                .toList();
        boolean pinned = comment.getParent() == null && comment.isPinned();
        return new CommentResponse(
                comment.getId(),
                comment.getArticle().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getDisplayName(),
                comment.getUser().getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                likeCount,
                likedByMe,
                parentId,
                comment.getStatus(),
                floorNo,
                pinned,
                replyResponses
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

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
