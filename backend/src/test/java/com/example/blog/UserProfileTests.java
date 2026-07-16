package com.example.blog;

import com.example.blog.article.ArticleRepository;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.comment.CommentRepository;
import com.example.blog.like.LikeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    private Category category;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        likeRepository.deleteAll();
        commentRepository.deleteAllReplies();
        commentRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("资料验收分类");
        category = categoryRepository.save(category);
        adminToken = loginAndGetToken("admin", "admin123");
    }

    @Test
    void getMeRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getAndUpdateMeSuccess() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data").value(not(hasKey("password"))))
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordHash"))));

        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"站长小绿",
                                  "bio":"写博客",
                                  "avatarUrl":"/uploads/demo.png"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("站长小绿"))
                .andExpect(jsonPath("$.data.bio").value("写博客"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/demo.png"))
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordHash"))));

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.displayName").value("站长小绿"));
    }

    @Test
    void updateMeRejectsOverlongAndBadUrl() throws Exception {
        String overlongName = "a".repeat(33);
        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","bio":"","avatarUrl":""}
                                """.formatted(overlongName)))
                .andExpect(jsonPath("$.code").value(400));

        String overlongBio = "b".repeat(201);
        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"ok","bio":"%s","avatarUrl":""}
                                """.formatted(overlongBio)))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"ok","bio":"","avatarUrl":"javascript:alert(1)"}
                                """))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"ok","bio":"","avatarUrl":"ftp://evil.example/a.png"}
                                """))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void publicArticleAndCommentShowProfileFields() throws Exception {
        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"展示名A",
                                  "bio":"简介",
                                  "avatarUrl":"https://cdn.example/a.png"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        long articleId = createPublishedArticle("资料展示文章");

        mockMvc.perform(get("/api/articles/{id}", articleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.authorName").value("展示名A"))
                .andExpect(jsonPath("$.data.authorAvatarUrl").value("https://cdn.example/a.png"))
                .andExpect(jsonPath("$.data.authorBio").value("简介"));

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"带资料的评论"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.displayName").value("展示名A"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example/a.png"));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data[0].displayName").value("展示名A"))
                .andExpect(jsonPath("$.data[0].avatarUrl").value("https://cdn.example/a.png"));
    }

    @Test
    void loginResponseIncludesProfileFields() throws Exception {
        mockMvc.perform(put("/api/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"登录展示",
                                  "bio":"",
                                  "avatarUrl":"/uploads/x.png"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("登录展示"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/x.png"))
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordHash"))));
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

    private long createPublishedArticle(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "content":"body",
                                  "categoryId":%d,
                                  "status":"PUBLISHED",
                                  "publishedAt":"2026-06-01T10:00:00"
                                }
                                """.formatted(title, category.getId())))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }
}
