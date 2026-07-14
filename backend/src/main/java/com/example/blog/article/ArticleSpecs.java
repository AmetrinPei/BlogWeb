package com.example.blog.article;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

final class ArticleSpecs {

    private ArticleSpecs() {
    }

    static Specification<Article> publiclyVisible(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ArticleStatus.PUBLISHED),
                cb.lessThanOrEqualTo(root.get("publishedAt"), now)
        );
    }

    static Specification<Article> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Article> hasTagId(Long tagId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Object, Object> tags = root.join("tags", JoinType.INNER);
            return cb.equal(tags.get("id"), tagId);
        };
    }

    static Specification<Article> titleContains(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }

    static Specification<Article> recommendedTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("recommended"));
    }

    static Specification<Article> yearMonthEquals(int year, int month) {
        return (root, query, cb) -> cb.and(
                cb.equal(cb.function("year", Integer.class, root.get("publishedAt")), year),
                cb.equal(cb.function("month", Integer.class, root.get("publishedAt")), month)
        );
    }

    static Specification<Article> statusEquals(ArticleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Article> authorIdEquals(Long authorId) {
        return (root, query, cb) -> cb.equal(root.get("author").get("id"), authorId);
    }
}
