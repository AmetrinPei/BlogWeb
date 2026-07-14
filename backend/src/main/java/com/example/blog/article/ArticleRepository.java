package com.example.blog.article;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    boolean existsByCategory_Id(Long categoryId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM article_tags WHERE tag_id = :tagId", nativeQuery = true)
    void deleteTagAssociations(@Param("tagId") Long tagId);

    @Query(value = "SELECT COUNT(*) FROM article_tags WHERE tag_id = :tagId", nativeQuery = true)
    long countTagAssociations(@Param("tagId") Long tagId);

    @Query(value = "SELECT COUNT(*) FROM article_tags WHERE article_id = :articleId", nativeQuery = true)
    long countAssociationsByArticleId(@Param("articleId") Long articleId);

    @EntityGraph(attributePaths = {"category", "tags", "author"})
    @Query("""
            SELECT a FROM Article a
            WHERE a.id = :id
              AND a.status = com.example.blog.article.ArticleStatus.PUBLISHED
              AND a.publishedAt <= :now
            """)
    Optional<Article> findPublishedById(@Param("id") Long id, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"category", "tags", "author"})
    @Query("SELECT a FROM Article a WHERE a.id = :id")
    Optional<Article> findByIdWithDetails(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query(value = """
            SELECT DATE_FORMAT(published_at, '%Y-%m') AS yearMonth, COUNT(*) AS cnt
            FROM articles
            WHERE status = 'PUBLISHED' AND published_at <= NOW()
            GROUP BY DATE_FORMAT(published_at, '%Y-%m')
            ORDER BY yearMonth DESC
            """, nativeQuery = true)
    List<Object[]> countPublishedGroupedByYearMonth();

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comments WHERE article_id = :articleId AND parent_id IS NOT NULL", nativeQuery = true)
    void deleteReplyCommentsByArticleId(@Param("articleId") Long articleId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM comments WHERE article_id = :articleId AND parent_id IS NULL", nativeQuery = true)
    void deleteRootCommentsByArticleId(@Param("articleId") Long articleId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM likes
            WHERE (target_type = 'ARTICLE' AND target_id = :articleId)
               OR (target_type = 'COMMENT' AND target_id IN (
                    SELECT id FROM comments WHERE article_id = :articleId
               ))
            """, nativeQuery = true)
    void deleteLikesByArticleId(@Param("articleId") Long articleId);
}
