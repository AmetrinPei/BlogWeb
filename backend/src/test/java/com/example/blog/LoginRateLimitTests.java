package com.example.blog;

import com.example.blog.auth.LoginRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "blog.auth.login-rate-limit.max-failures=3",
        "blog.auth.login-rate-limit.window-seconds=900"
})
class LoginRateLimitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginRateLimitService loginRateLimitService;

    @BeforeEach
    void resetCounters() {
        loginRateLimitService.clearAll();
    }

    @Test
    void publicLoginBlockedAfterMaxFailuresThenSucceedsAfterClearViaSuccess() throws Exception {
        String username = "rate_user_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "203.0.113.10")
                            .content("""
                                    {"username":"%s","password":"wrong-pass"}
                                    """.formatted(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("""
                                {"username":"%s","password":"wrong-pass"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("登录尝试过于频繁，请稍后再试"));

        // Different IP still blocked by username dimension
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.99")
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(429));

        loginRateLimitService.clearAll();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void successfulLoginClearsFailureCount() throws Exception {
        String username = "rate_ok_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.7")
                        .content("""
                                {"username":"%s","password":"wrong"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.7")
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));

        // After success, can fail again up to max without immediate 429
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "198.51.100.7")
                            .content("""
                                    {"username":"%s","password":"wrong"}
                                    """.formatted(username)))
                    .andExpect(jsonPath("$.code").value(401));
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.7")
                        .content("""
                                {"username":"%s","password":"wrong"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void adminLoginSharesSameLimiter() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "192.0.2.55")
                            .content("""
                                    {"username":"admin","password":"not-admin123"}
                                    """))
                    .andExpect(jsonPath("$.code").value(401));
        }
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.0.2.55")
                        .content("""
                                {"username":"admin","password":"not-admin123"}
                                """))
                .andExpect(jsonPath("$.code").value(429));
    }
}
