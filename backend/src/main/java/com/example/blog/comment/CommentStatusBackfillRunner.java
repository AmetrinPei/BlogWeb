package com.example.blog.comment;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures legacy rows without status become APPROVED after schema add.
 */
@Component
public class CommentStatusBackfillRunner implements ApplicationRunner {

    private final CommentRepository commentRepository;

    public CommentStatusBackfillRunner(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        commentRepository.approveNullStatuses();
    }
}
