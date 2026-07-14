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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentThreadTests {

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
    private String token;
    private long articleId;

    @BeforeEach
    void setUp() throws Exception {
        likeRepository.deleteAll();
        commentRepository.deleteAllReplies();
        commentRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("评论楼中楼分类");
        category = categoryRepository.save(category);
        token = loginAndGetToken();
        articleId = createPublishedArticle("楼中楼验收文章");
    }

    @Test
    void createReplyAndListAsTreeOrdered() throws Exception {
        long rootId = createComment(articleId, "根评论", null);
        createComment(articleId, "回复甲", rootId);
        createComment(articleId, "回复乙", rootId);

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value((int) rootId))
                .andExpect(jsonPath("$.data[0].parentId").value(nullValue()))
                .andExpect(jsonPath("$.data[0].content").value("根评论"))
                .andExpect(jsonPath("$.data[0].replies", hasSize(2)))
                .andExpect(jsonPath("$.data[0].replies[0].parentId").value((int) rootId))
                .andExpect(jsonPath("$.data[0].replies[*].content", containsInAnyOrder("回复甲", "回复乙")))
                .andExpect(jsonPath("$.data[0].replies[0].replies", hasSize(0)));
    }

    @Test
    void rejectReplyToReplyAndCrossArticleParent() throws Exception {
        long rootId = createComment(articleId, "根", null);
        long replyId = createComment(articleId, "子", rootId);

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"三层","parentId":%d}
                                """.formatted(replyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅允许回复一级评论"));

        long otherArticleId = createPublishedArticle("另一篇文章");
        mockMvc.perform(post("/api/articles/{id}/comments", otherArticleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"跨文","parentId":%d}
                                """.formatted(rootId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("父评论不属于该文章"));

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"幽灵","parentId":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void cascadeDeleteRootAndUnauthCreate401() throws Exception {
        long rootId = createComment(articleId, "待删根", null);
        long replyId = createComment(articleId, "待删子", rootId);

        mockMvc.perform(post("/api/likes/toggle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"COMMENT","targetId":%d}
                                """.formatted(replyId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(delete("/api/comments/{id}", rootId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/likes/toggle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"COMMENT","targetId":%d}
                                """.formatted(replyId)))
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"未登录"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void deleteReplyKeepsRootAndLikeOnRootWorks() throws Exception {
        long rootId = createComment(articleId, "保留根", null);
        long replyId = createComment(articleId, "只删我", rootId);

        mockMvc.perform(post("/api/likes/toggle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"COMMENT","targetId":%d}
                                """.formatted(rootId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.liked").value(true));

        mockMvc.perform(delete("/api/comments/{id}", replyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value((int) rootId))
                .andExpect(jsonPath("$.data[0].replies", hasSize(0)))
                .andExpect(jsonPath("$.data[0].likeCount").value(1))
                .andExpect(jsonPath("$.data[0].likedByMe").value(true));
    }

    @Test
    void legacyRootWithoutParentIdStillListed() throws Exception {
        long rootId = createComment(articleId, "旧一级评论", null);

        mockMvc.perform(get("/api/articles/{id}/comments", articleId))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value((int) rootId))
                .andExpect(jsonPath("$.data[0].parentId").value(nullValue()))
                .andExpect(jsonPath("$.data[0].replies", hasSize(0)));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = loginResult.getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private long createPublishedArticle(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
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
        String body = result.getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.id");
        return id.longValue();
    }

    private long createComment(long articleId, String content, Long parentId) throws Exception {
        String payload = parentId == null
                ? "{\"content\":\"%s\"}".formatted(content)
                : "{\"content\":\"%s\",\"parentId\":%d}".formatted(content, parentId);
        MvcResult result = mockMvc.perform(post("/api/articles/{id}/comments", articleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.id");
        return id.longValue();
    }
}
