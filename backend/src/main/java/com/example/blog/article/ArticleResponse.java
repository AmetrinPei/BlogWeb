package com.example.blog.article;

import com.example.blog.category.CategoryResponse;
import com.example.blog.tag.TagResponse;
import com.example.blog.user.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class ArticleResponse {

    private static final int SUMMARY_FALLBACK_LEN = 120;

    private final Long id;
    private final String title;
    private final String content;
    private final String summary;
    private final String coverUrl;
    private final ArticleStatus status;
    private final long viewCount;
    private final boolean pinned;
    private final boolean recommended;
    private final Long authorId;
    private final String authorName;
    private final String authorAvatarUrl;
    private final String authorBio;
    private final LocalDateTime publishedAt;
    private final CategoryResponse category;
    private final List<TagResponse> tags;

    public ArticleResponse(
            Long id,
            String title,
            String content,
            String summary,
            String coverUrl,
            ArticleStatus status,
            long viewCount,
            boolean pinned,
            boolean recommended,
            Long authorId,
            String authorName,
            String authorAvatarUrl,
            String authorBio,
            LocalDateTime publishedAt,
            CategoryResponse category,
            List<TagResponse> tags
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.coverUrl = coverUrl;
        this.status = status;
        this.viewCount = viewCount;
        this.pinned = pinned;
        this.recommended = recommended;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.authorBio = authorBio;
        this.publishedAt = publishedAt;
        this.category = category;
        this.tags = tags;
    }

    public static ArticleResponse from(Article article) {
        return build(article, article.getContent(), resolveSummary(article));
    }

    public static ArticleResponse summary(Article article) {
        return build(article, null, resolveSummary(article));
    }

    private static ArticleResponse build(Article article, String content, String summary) {
        List<TagResponse> tags = article.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.getId() == null ? 0L : tag.getId()))
                .map(TagResponse::from)
                .toList();
        User author = article.getAuthor();
        Long authorId = author != null ? author.getId() : null;
        String authorName = author != null ? author.resolveDisplayName() : null;
        String authorAvatarUrl = author != null ? author.getAvatarUrl() : null;
        String authorBio = author != null ? author.getBio() : null;
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                content,
                summary,
                article.getCoverUrl(),
                article.getStatus(),
                article.getViewCount(),
                article.isPinned(),
                article.isRecommended(),
                authorId,
                authorName,
                authorAvatarUrl,
                authorBio,
                article.getPublishedAt(),
                CategoryResponse.from(article.getCategory()),
                tags
        );
    }

    /** Public list / RSS share the same summary fallback rules. */
    public static String resolveSummary(Article article) {
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            return article.getSummary().trim();
        }
        String content = article.getContent() == null ? "" : article.getContent().trim();
        if (content.length() <= SUMMARY_FALLBACK_LEN) {
            return content;
        }
        return content.substring(0, SUMMARY_FALLBACK_LEN) + "…";
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public long getViewCount() {
        return viewCount;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getAuthorBio() {
        return authorBio;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public CategoryResponse getCategory() {
        return category;
    }

    public List<TagResponse> getTags() {
        return tags;
    }
}
