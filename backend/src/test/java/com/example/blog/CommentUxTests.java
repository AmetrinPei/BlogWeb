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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentUxTests {

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
    private long articleId;

    @BeforeEach
    void setUp() throws Exception {
        likeRepository.deleteAll();
        commentRepository.deleteAllReplies();
        commentRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("评论UX分类");
        category = categoryRepository.save(category);
        adminToken = loginAndGetToken("admin", "admin123");
        articleId = createPublishedArticle("楼层置顶验收文章");
    }

    @Test
    void rootCommentsGetStableFloorNumbers() throws Exception {
        long c1 = createRoot("一楼");
        long c2 = createRoot("二楼");
        long c3 = createRoot("三楼");

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value((int) c1))
                .andExpect(jsonPath("$.data[0].floorNo").value(1))
                .andExpect(jsonPath("$.data[0].pinned").value(false))
                .andExpect(jsonPath("$.data[1].id").value((int) c2))
                .andExpect(jsonPath("$.data[1].floorNo").value(2))
                .andExpect(jsonPath("$.data[2].id").value((int) c3))
                .andExpect(jsonPath("$.data[2].floorNo").value(3));
    }

    @Test
    void pinMovesToFrontButKeepsFloorNoAndReplacesPrevious() throws Exception {
        long c1 = createRoot("一楼");
        long c2 = createRoot("二楼");
        long c3 = createRoot("三楼");

        mockMvc.perform(put("/api/comments/{id}/pin", c2)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned":true}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pinned").value(true))
                .andExpect(jsonPath("$.data.floorNo").value(2));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value((int) c2))
                .andExpect(jsonPath("$.data[0].floorNo").value(2))
                .andExpect(jsonPath("$.data[0].pinned").value(true))
                .andExpect(jsonPath("$.data[1].id").value((int) c1))
                .andExpect(jsonPath("$.data[1].floorNo").value(1))
                .andExpect(jsonPath("$.data[2].id").value((int) c3))
                .andExpect(jsonPath("$.data[2].floorNo").value(3));

        mockMvc.perform(put("/api/comments/{id}/pin", c3)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned":true}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data[0].id").value((int) c3))
                .andExpect(jsonPath("$.data[0].pinned").value(true))
                .andExpect(jsonPath("$.data[0].floorNo").value(3))
                .andExpect(jsonPath("$.data[1].id").value((int) c1))
                .andExpect(jsonPath("$.data[2].id").value((int) c2))
                .andExpect(jsonPath("$.data[2].pinned").value(false));
    }

    @Test
    void replyCannotBePinnedAndHasNullFloor() throws Exception {
        long rootId = createRoot("根");
        MvcResult reply = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"回复","parentId":%d}
                                """.formatted(rootId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.floorNo").value(nullValue()))
                .andReturn();
        Number replyId = JsonPath.read(reply.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/comments/{id}/pin", replyId.longValue())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned":true}
                                """))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void pinRequiresAuthAndArticleAuthorOrAdmin() throws Exception {
        long rootId = createRoot("根");

        mockMvc.perform(put("/api/comments/{id}/pin", rootId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        String outsider = registerAndLogin("outsider_" + System.currentTimeMillis());
        mockMvc.perform(put("/api/comments/{id}/pin", rootId)
                        .header("Authorization", "Bearer " + outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned":true}
                                """))
                .andExpect(jsonPath("$.code").value(403));
    }

    private long createRoot(String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"%s"}
                                """.formatted(content)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass1234"}
                                """.formatted(username)))
                .andExpect(jsonPath("$.code").value(0));
        return loginAndGetToken(username, "pass1234");
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
