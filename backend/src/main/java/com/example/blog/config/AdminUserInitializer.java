package com.example.blog.config;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleRepository;
import com.example.blog.article.ArticleStatus;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;
import com.example.blog.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(
            UserRepository userRepository,
            ArticleRepository articleRepository,
            AdminProperties adminProperties,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.adminProperties = adminProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User admin = ensureAdmin();
        backfillRoles();
        backfillArticles(admin);
    }

    private User ensureAdmin() {
        String username = adminProperties.getUsername();
        return userRepository.findByUsername(username).map(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                userRepository.save(existing);
            }
            log.info("Admin user '{}' already exists", username);
            return existing;
        }).orElseGet(() -> {
            User admin = new User();
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
            log.info("Seeded admin user '{}'", username);
            return admin;
        });
    }

    private void backfillRoles() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRole() == null) {
                user.setRole(UserRole.AUTHOR);
                userRepository.save(user);
            }
        }
    }

    private void backfillArticles(User admin) {
        List<Article> articles = articleRepository.findAll();
        for (Article article : articles) {
            boolean changed = false;
            if (article.getStatus() == null) {
                article.setStatus(ArticleStatus.PUBLISHED);
                changed = true;
            }
            if (article.getAuthor() == null) {
                article.setAuthor(admin);
                changed = true;
            }
            if (changed) {
                articleRepository.save(article);
            }
        }
    }
}
