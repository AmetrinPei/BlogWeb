package com.example.blog.like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    Optional<LikeEntity> findByUser_IdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetId(LikeTargetType targetType, Long targetId);

    boolean existsByUser_IdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(LikeTargetType targetType, Long targetId);
}
