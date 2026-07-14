package com.example.blog.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.parent
            LEFT JOIN FETCH c.user
            WHERE c.article.id = :articleId AND c.status = :status
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findByArticle_IdAndStatusOrderByCreatedAtAsc(
            @Param("articleId") Long articleId,
            @Param("status") CommentStatus status
    );

    List<Comment> findByParent_IdOrderByCreatedAtAsc(Long parentId);

    long countByUser_IdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    Page<Comment> findByStatusOrderByCreatedAtDesc(CommentStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE comments SET status = 'APPROVED' WHERE status IS NULL", nativeQuery = true)
    int approveNullStatuses();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Comment c SET c.pinned = false WHERE c.article.id = :articleId AND c.parent IS NULL AND c.pinned = true")
    int clearPinnedRootsByArticleId(@Param("articleId") Long articleId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.parent IS NOT NULL")
    void deleteAllReplies();
}
