package com.example.blog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsAccessAndRefresh() throws Exception {
        String username = "refresh_login_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.accessExpireMinutes").value(30))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void refreshRotatesAndInvalidatesOldRefresh() throws Exception {
        String username = "refresh_rot_" + System.currentTimeMillis();
        MvcResult login = register(username, "pass1234");
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken").value(not(refresh)))
                .andReturn();

        String newRefresh = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.data.refreshToken");
        org.junit.jupiter.api.Assertions.assertNotEquals(refresh, newRefresh);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已失效，请重新登录"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(newRefresh)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value(notNullValue()));
    }

    @Test
    void logoutRevokesRefresh() throws Exception {
        String username = "refresh_out_" + System.currentTimeMillis();
        MvcResult login = register(username, "pass1234");
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void changePasswordRevokesAllRefreshTokens() throws Exception {
        String username = "refresh_pwd_" + System.currentTimeMillis();
        MvcResult login = register(username, "pass1234");
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

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
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass5678"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refreshToken").value(not(blankOrNullString())));
    }

    @Test
    void blankRefreshTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":""}
                                """))
                .andExpect(jsonPath("$.code").value(400));
    }

    private MvcResult register(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }
}
