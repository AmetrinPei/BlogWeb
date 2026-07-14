package com.example.blog;

import com.example.blog.user.User;
import com.example.blog.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersistenceSeedTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminUserIsSeededWithBcryptPassword() {
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("admin user should be seeded"));

        assertThat(admin.getPasswordHash()).isNotBlank();
        assertThat(new BCryptPasswordEncoder().matches("admin123", admin.getPasswordHash())).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
    }
}
