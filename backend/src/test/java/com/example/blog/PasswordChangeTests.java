package com.example.blog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordChangeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void changePasswordRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"pass1234",
                                  "newPassword":"pass5678",
                                  "confirmPassword":"pass5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void changePasswordSuccessThenOldFailsNewWorks() throws Exception {
        String username = "pwd_ok_" + System.currentTimeMillis();
        register(username, "pass1234");
        String token = loginAndGetToken(username, "pass1234");

        mockMvc.perform(put("/api/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"pass1234",
                                  "newPassword":"pass5678",
                                  "confirmPassword":"pass5678"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data").value(not(hasKey("password"))))
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordHash"))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass5678"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void wrongCurrentPasswordRejectedAndUnchanged() throws Exception {
        String username = "pwd_wrong_" + System.currentTimeMillis();
        register(username, "pass1234");
        String token = loginAndGetToken(username, "pass1234");

        mockMvc.perform(put("/api/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"wrong-old",
                                  "newPassword":"pass5678",
                                  "confirmPassword":"pass5678"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("当前密码不正确"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void rejectConfirmMismatchShortAndSamePassword() throws Exception {
        String username = "pwd_bad_" + System.currentTimeMillis();
        register(username, "pass1234");
        String token = loginAndGetToken(username, "pass1234");

        mockMvc.perform(put("/api/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"pass1234",
                                  "newPassword":"pass5678",
                                  "confirmPassword":"pass9999"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("两次输入的新密码不一致"));

        mockMvc.perform(put("/api/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"pass1234",
                                  "newPassword":"123",
                                  "confirmPassword":"123"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"pass1234",
                                  "newPassword":"pass1234",
                                  "confirmPassword":"pass1234"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("新密码不能与当前密码相同"));
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");
    }
}
