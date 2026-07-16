package com.example.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FriendLinksSiteTests {

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

    private String userToken(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = loginResult.getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void publicSiteIncludesFriendAndAboutFields() throws Exception {
        mockMvc.perform(get("/api/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.friendLinks").isArray())
                .andExpect(jsonPath("$.data.aboutHighlights").isArray())
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void adminCanSaveFriendLinksInOrderAndAboutFields() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "关于正文",
                                  "socialLinks": [{"name":"GitHub","url":"https://github.com/example"}],
                                  "friendLinks": [
                                    {"name":"站B","url":"https://b.example.com","description":"第二"},
                                    {"name":"站A","url":"https://a.example.com","description":"第一"}
                                  ],
                                  "aboutTitle": "关于本站",
                                  "aboutDisplayName": "小简",
                                  "aboutHighlights": ["写代码", "写博客"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.friendLinks", hasSize(2)))
                .andExpect(jsonPath("$.data.friendLinks[0].name").value("站B"))
                .andExpect(jsonPath("$.data.friendLinks[0].sortOrder").value(0))
                .andExpect(jsonPath("$.data.friendLinks[1].name").value("站A"))
                .andExpect(jsonPath("$.data.friendLinks[1].sortOrder").value(1))
                .andExpect(jsonPath("$.data.aboutTitle").value("关于本站"))
                .andExpect(jsonPath("$.data.aboutDisplayName").value("小简"))
                .andExpect(jsonPath("$.data.aboutHighlights", hasSize(2)))
                .andExpect(jsonPath("$.data.socialLinks[0].name").value("GitHub"));

        mockMvc.perform(get("/api/site"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.friendLinks[0].name").value("站B"))
                .andExpect(jsonPath("$.data.friendLinks[1].url").value("https://a.example.com"))
                .andExpect(jsonPath("$.data.aboutTitle").value("关于本站"))
                .andExpect(jsonPath("$.data.socialLinks[0].url").value("https://github.com/example"));

        // clear friends; social remains when re-sent
        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [{"name":"GitHub","url":"https://github.com/example"}],
                                  "friendLinks": [],
                                  "aboutTitle": "",
                                  "aboutDisplayName": "",
                                  "aboutHighlights": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.friendLinks", hasSize(0)))
                .andExpect(jsonPath("$.data.socialLinks", hasSize(1)))
                .andExpect(jsonPath("$.data.aboutHighlights", hasSize(0)));
    }

    @Test
    void illegalFriendUrlAndOverLimitRejected() throws Exception {
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
                                  "friendLinks": [
                                    {"name":"坏链","url":"javascript:alert(1)","description":""}
                                  ]
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
                                  "friendLinks": [
                                    {"name":"相对","url":"/uploads/x.png","description":""}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400));

        StringBuilder many = new StringBuilder();
        many.append("""
                {
                  "siteName": "简博客",
                  "tagline": "t",
                  "aboutText": "",
                  "socialLinks": [],
                  "friendLinks": [
                """);
        for (int i = 0; i < 51; i++) {
            if (i > 0) {
                many.append(',');
            }
            many.append("{\"name\":\"n").append(i)
                    .append("\",\"url\":\"https://example.com/").append(i)
                    .append("\",\"description\":\"\"}");
        }
        many.append("]}");

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(many.toString()))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateRequiresAuthAndAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "friendLinks": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(401));

        String user = userToken("friend_user_" + System.currentTimeMillis());
        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "t",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "friendLinks": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void omitFriendFieldsKeepsExisting() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "keep",
                                  "aboutText": "keep-about",
                                  "socialLinks": [],
                                  "friendLinks": [
                                    {"name":"保留站","url":"https://keep.example.com","description":"x"}
                                  ],
                                  "aboutTitle": "保留标题",
                                  "aboutDisplayName": "保留名",
                                  "aboutHighlights": ["保留亮点"]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客-改名",
                                  "tagline": "keep",
                                  "aboutText": "keep-about",
                                  "socialLinks": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.siteName").value("简博客-改名"))
                .andExpect(jsonPath("$.data.friendLinks[0].name").value("保留站"))
                .andExpect(jsonPath("$.data.aboutTitle").value("保留标题"))
                .andExpect(jsonPath("$.data.aboutDisplayName").value("保留名"))
                .andExpect(jsonPath("$.data.aboutHighlights[0]").value("保留亮点"));

        mockMvc.perform(put("/api/admin/site")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "简博客",
                                  "tagline": "慢慢写",
                                  "aboutText": "",
                                  "socialLinks": [],
                                  "friendLinks": [],
                                  "aboutTitle": "",
                                  "aboutDisplayName": "",
                                  "aboutHighlights": []
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }
}
