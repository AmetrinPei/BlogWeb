package com.example.blog;

import com.example.blog.article.ArticleRepository;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.comment.CommentRepository;
import com.example.blog.comment.SensitiveWordRepository;
import com.example.blog.config.CommentProperties;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentModerationTests {

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

    @Autowired
    private SensitiveWordRepository sensitiveWordRepository;

    @Autowired
    private CommentProperties commentProperties;

    private Category category;
    private String adminToken;
    private long articleId;
    private int originalRateLimit;

    @BeforeEach
    void setUp() throws Exception {
        originalRateLimit = commentProperties.getRateLimitPerMinute();
        commentProperties.setRateLimitPerMinute(5);

        likeRepository.deleteAll();
        commentRepository.deleteAllReplies();
        commentRepository.deleteAll();
        sensitiveWordRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("评论审核分类");
        category = categoryRepository.save(category);
        adminToken = loginAndGetToken("admin", "admin123");
        articleId = createPublishedArticle("审核验收文章");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        commentProperties.setRateLimitPerMinute(originalRateLimit);
    }

    @Test
    void cleanCommentAutoApprovedAndListed() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"干净评论内容"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(id.intValue()))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    void sensitiveWordGoesPendingThenApproveReject() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"word":"违禁词"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult created = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"这句话含违禁词哈哈"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        Number commentId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(commentId.intValue()));

        mockMvc.perform(put("/api/admin/comments/{id}/status", commentId.longValue())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(commentId.intValue()));

        mockMvc.perform(put("/api/admin/comments/{id}/status", commentId.longValue())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void rateLimitRejectsSixthComment() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content":"限流测试 %d"}
                                    """.formatted(i)))
                    .andExpect(jsonPath("$.code").value(0));
        }

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"第6条应失败"}
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("评论过于频繁，请稍后再试"));
    }

    @Test
    void nonAdminForbiddenOnModerationApis() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user_%s","password":"pass1234"}
                                """.formatted(suffix)))
                .andExpect(jsonPath("$.code").value(0));
        String userToken = loginAndGetToken("user_" + suffix, "pass1234");

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"word":"x"}
                                """))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void cannotLikePendingComment() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"word":"spam"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult created = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"this is spam"}
                                """))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        Number commentId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/likes/toggle")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"COMMENT","targetId":%d}
                                """.formatted(commentId.longValue())))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("评论不存在"));
    }

    @Test
    void duplicateSensitiveWordConflictAndCaseInsensitiveHit() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"word":"BadWord"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.word").value("badword"));

        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"word":"BADWORD"}
                                """))
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"contains BADWORD here"}
                                """))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
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
