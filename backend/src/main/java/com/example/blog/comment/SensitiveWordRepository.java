package com.example.blog.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {

    List<SensitiveWord> findAllByOrderByIdAsc();

    boolean existsByWord(String word);

    Optional<SensitiveWord> findByWord(String word);
}
