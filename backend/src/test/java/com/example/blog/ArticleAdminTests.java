package com.example.blog;

import com.example.blog.article.ArticleRepository;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.tag.Tag;
import com.example.blog.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleAdminTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private Category category;
    private Tag tagA;
    private Tag tagB;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        articleRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();

        category = categoryRepository.save(newCategory("管理分类"));
        tagA = tagRepository.save(newTag("标签A"));
        tagB = tagRepository.save(newTag("标签B"));
        token = loginAndGetToken();
    }

    @Test
    void adminApisRequireJwt() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","content":"y","categoryId":1}
                                """))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void fullCrudFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"第一篇管理文章",
                                  "content":"正文内容",
                                  "categoryId":%d,
                                  "tagIds":[%d,%d],
                                  "publishedAt":"2026-05-01T10:00:00"
                                }
                                """.formatted(category.getId(), tagA.getId(), tagB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("第一篇管理文章"))
                .andExpect(jsonPath("$.data.content").value("正文内容"))
                .andExpect(jsonPath("$.data.category.name").value("管理分类"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        String id = createBody.replaceAll("(?s).*\"data\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mockMvc.perform(get("/api/admin/articles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("第一篇管理文章"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)));

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].content").value("正文内容"));

        mockMvc.perform(put("/api/admin/articles/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"更新后的标题",
                                  "content":"更新后的正文",
                                  "categoryId":%d,
                                  "tagIds":[%d],
                                  "publishedAt":"2026-06-01T08:30:00"
                                }
                                """.formatted(category.getId(), tagA.getId())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("更新后的标题"))
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[0].name").value("标签A"));

        mockMvc.perform(delete("/api/admin/articles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));

        assertThat(articleRepository.existsById(Long.parseLong(id))).isFalse();
        assertThat(articleRepository.countAssociationsByArticleId(Long.parseLong(id))).isZero();

        mockMvc.perform(get("/api/admin/articles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    void adminListIncludesFutureArticles() throws Exception {
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"定时发布文章",
                                  "content":"未来可见",
                                  "categoryId":%d,
                                  "tagIds":[],
                                  "publishedAt":"2099-01-01T00:00:00"
                                }
                                """.formatted(category.getId())))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("定时发布文章"));

        mockMvc.perform(get("/api/articles"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void createWithInvalidCategoryOrTagReturns400() throws Exception {
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"无效分类",
                                  "content":"正文",
                                  "categoryId":999999,
                                  "tagIds":[]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分类不存在"));

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"无效标签",
                                  "content":"正文",
                                  "categoryId":%d,
                                  "tagIds":[999999]
                                }
                                """.formatted(category.getId())))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("存在无效的标签 ID"));
    }

    @Test
    void createValidationErrorsReturn400() throws Exception {
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","content":"正文","categoryId":1}
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("title 不能为空"));
    }

    private String loginAndGetToken() throws Exception {
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

    private Category newCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }

    private Tag newTag(String name) {
        Tag t = new Tag();
        t.setName(name);
        return t;
    }
}
