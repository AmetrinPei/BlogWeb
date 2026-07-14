package com.example.blog.article;

import com.example.blog.auth.CurrentUserService;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.common.PageResult;
import com.example.blog.tag.Tag;
import com.example.blog.tag.TagRepository;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArticleService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;

    public ArticleService(
            ArticleRepository articleRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            CurrentUserService currentUserService
    ) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleResponse> listPublished(
            Integer page,
            Integer size,
            Long categoryId,
            Long tagId,
            String keyword,
            String yearMonth
    ) {
        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);

        Specification<Article> spec = ArticleSpecs.publiclyVisible(LocalDateTime.now());
        if (categoryId != null) {
            spec = spec.and(ArticleSpecs.categoryIdEquals(categoryId));
        }
        if (tagId != null) {
            spec = spec.and(ArticleSpecs.hasTagId(tagId));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ArticleSpecs.titleContains(keyword.trim()));
        }
        if (yearMonth != null && !yearMonth.isBlank()) {
            spec = spec.and(parseYearMonthSpec(yearMonth.trim()));
        }

        Page<Article> result = articleRepository.findAll(spec, publicPageRequest(pageNumber, pageSize));
        return new PageResult<>(
                result.getContent().stream().map(ArticleResponse::summary).toList(),
                pageNumber,
                pageSize,
                result.getTotalElements()
        );
    }

    @Transactional
    public ArticleResponse getPublished(Long id) {
        Article article = articleRepository.findPublishedById(id, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
        articleRepository.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);
        return ArticleResponse.from(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> listFeatured(Integer size) {
        int pageSize = size == null || size < 1 ? 5 : Math.min(size, MAX_SIZE);
        Specification<Article> spec = ArticleSpecs.publiclyVisible(LocalDateTime.now())
                .and(ArticleSpecs.recommendedTrue());
        Page<Article> result = articleRepository.findAll(spec, publicPageRequest(1, pageSize));
        return result.getContent().stream().map(ArticleResponse::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<ArchiveMonthResponse> listArchive() {
        List<Object[]> rows = articleRepository.countPublishedGroupedByYearMonth();
        List<ArchiveMonthResponse> items = new ArrayList<>();
        for (Object[] row : rows) {
            String yearMonth = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            items.add(new ArchiveMonthResponse(yearMonth, count));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleResponse> listAdmin(Integer page, Integer size, ArticleStatus status) {
        User user = currentUserService.requireUser();
        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<Article> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and(ArticleSpecs.statusEquals(status));
        }
        // AUTHOR 仅可见自己的文章；ADMIN 可见全部
        if (user.getRole() != UserRole.ADMIN) {
            spec = spec.and(ArticleSpecs.authorIdEquals(user.getId()));
        }
        Page<Article> result = articleRepository.findAll(spec, adminPageRequest(pageNumber, pageSize));
        return new PageResult<>(
                result.getContent().stream().map(ArticleResponse::from).toList(),
                pageNumber,
                pageSize,
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ArticleResponse getAdmin(Long id) {
        User user = currentUserService.requireUser();
        Article article = articleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
        assertCanModify(user, article);
        return ArticleResponse.from(article);
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request) {
        User user = currentUserService.requireUser();
        Article article = new Article();
        article.setAuthor(user);
        applyRequest(article, request, true);
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        User user = currentUserService.requireUser();
        Article article = articleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
        assertCanModify(user, article);
        applyRequest(article, request, false);
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.requireUser();
        Article article = articleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
        assertCanModify(user, article);
        articleRepository.deleteLikesByArticleId(id);
        articleRepository.deleteCommentsByArticleId(id);
        article.getTags().clear();
        articleRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public Article requirePublishedArticle(Long id) {
        return articleRepository.findPublishedById(id, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
    }

    private void assertCanModify(User user, Article article) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (article.getAuthor() == null || !article.getAuthor().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改他人文章");
        }
    }

    private void applyRequest(Article article, ArticleRequest request, boolean creating) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent());
        article.setCategory(requireCategory(request.getCategoryId()));
        Set<Tag> tags = resolveTags(request.getTagIds());
        article.getTags().clear();
        article.getTags().addAll(tags);

        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        } else if (creating) {
            article.setStatus(ArticleStatus.DRAFT);
        }

        if (request.getCoverUrl() != null) {
            String cover = request.getCoverUrl().trim();
            article.setCoverUrl(cover.isEmpty() ? null : cover);
        }
        if (request.getSummary() != null) {
            String summary = request.getSummary().trim();
            article.setSummary(summary.isEmpty() ? null : summary);
        }
        if (request.getPinned() != null) {
            article.setPinned(request.getPinned());
        }
        if (request.getRecommended() != null) {
            article.setRecommended(request.getRecommended());
        }

        if (request.getPublishedAt() != null) {
            article.setPublishedAt(request.getPublishedAt());
        } else if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        if (article.getStatus() == ArticleStatus.PUBLISHED && request.getPublishedAt() == null
                && creating) {
            article.setPublishedAt(LocalDateTime.now());
        }
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "分类不存在"));
    }

    private Set<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> distinctIds = tagIds.stream().distinct().toList();
        List<Tag> tags = tagRepository.findAllById(distinctIds);
        if (tags.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在无效的标签 ID");
        }
        return new HashSet<>(tags);
    }

    private static Specification<Article> parseYearMonthSpec(String yearMonth) {
        String[] parts = yearMonth.split("-");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "yearMonth 格式应为 yyyy-MM");
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                throw new NumberFormatException("month");
            }
            return ArticleSpecs.yearMonthEquals(year, month);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "yearMonth 格式应为 yyyy-MM");
        }
    }

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private static int normalizeSize(Integer size) {
        return size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    private static PageRequest publicPageRequest(int pageNumber, int pageSize) {
        return PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("publishedAt"))
        );
    }

    private static PageRequest adminPageRequest(int pageNumber, int pageSize) {
        return PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
