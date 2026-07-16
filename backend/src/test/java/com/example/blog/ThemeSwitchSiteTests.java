package com.example.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ThemeSwitchSiteTests {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = loginResult.getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void publicSiteIncludesThemeFieldsWithDefaults() throws Exception {
        mockMvc.perform(get("/api/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.siteName").isNotEmpty())
                .andExpect(jsonPath("$.data.defaultTheme").isString())
                .andExpect(jsonPath("$.data.backgroundMode").isString())
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void adminCanUpdateColorBackgroundAndReadBack() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "test",
                                  "aboutText": "about",
                                  "socialLinks": [],
                                  "defaultTheme": "dark",
                                  "backgroundMode": "color",
                                  "backgroundColor": "#E8F6EE",
                                  "backgroundGradient": "",
                                  "backgroundImageUrl": "",
                                  "aboutAvatarUrl": "/uploads/avatar.png",
                                  "homeHeroUrl": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.defaultTheme").value("dark"))
                .andExpect(jsonPath("$.data.backgroundMode").value("color"))
                .andExpect(jsonPath("$.data.backgroundColor").value("#E8F6EE"))
                .andExpect(jsonPath("$.data.aboutAvatarUrl").value("/uploads/avatar.png"));

        mockMvc.perform(get("/api/site"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.defaultTheme").value("dark"))
                .andExpect(jsonPath("$.data.backgroundMode").value("color"))
                .andExpect(jsonPath("$.data.backgroundColor").value("#E8F6EE"));

        // restore theme mode
        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "test",
                                  "aboutText": "about",
                                  "socialLinks": [],
                                  "defaultTheme": "light",
                                  "backgroundMode": "theme",
                                  "backgroundColor": "",
                                  "backgroundGradient": "",
                                  "backgroundImageUrl": "",
                                  "aboutAvatarUrl": "",
                                  "homeHeroUrl": ""
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.backgroundMode").value("theme"))
                .andExpect(jsonPath("$.data.aboutAvatarUrl").value(nullValue()));
    }

    @Test
    void gradientAndImageModesValidate() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "backgroundMode": "gradient",
                                  "backgroundGradient": "mint-wash"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.backgroundMode").value("gradient"))
                .andExpect(jsonPath("$.data.backgroundGradient").value("mint-wash"));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "backgroundMode": "image",
                                  "backgroundImageUrl": "https://example.com/bg.jpg"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.backgroundMode").value("image"))
                .andExpect(jsonPath("$.data.backgroundImageUrl").value("https://example.com/bg.jpg"));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "backgroundMode": "theme"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void illegalThemeModeColorAndUrlRejected() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "defaultTheme": "mint"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "backgroundMode": "color",
                                  "backgroundColor": "red"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "backgroundMode": "gradient",
                                  "backgroundGradient": "neon-cyber"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "aboutAvatarUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateRequiresAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void omitNewFieldsKeepsExistingThemeValues() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "keep-me",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "defaultTheme": "dark",
                                  "backgroundMode": "color",
                                  "backgroundColor": "#ABCDEF"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客-更新",
                                  "tagline": "keep-me",
                                  "aboutText": "",
                                  "socialLinks": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.siteName").value("简博客-更新"))
                .andExpect(jsonPath("$.data.defaultTheme").value("dark"))
                .andExpect(jsonPath("$.data.backgroundMode").value("color"))
                .andExpect(jsonPath("$.data.backgroundColor").value("#ABCDEF"));

        // restore
        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "慢慢写",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "defaultTheme": "light",
                                  "backgroundMode": "theme",
                                  "backgroundColor": "",
                                  "backgroundGradient": "",
                                  "backgroundImageUrl": "",
                                  "aboutAvatarUrl": "",
                                  "homeHeroUrl": ""
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }
}
