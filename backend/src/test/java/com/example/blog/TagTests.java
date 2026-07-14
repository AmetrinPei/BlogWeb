package com.example.blog;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleRepository;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.tag.Tag;
import com.example.blog.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TagTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void publicListTags() throws Exception {
        tagRepository.save(newTag("公开标签A"));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.name=='公开标签A')]").exists());
    }

    @Test
    void adminCrudRequiresAuthAndWorksWithToken() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/admin/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        MvcResult createResult = mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Spring"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Spring"))
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        String id = createBody.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mockMvc.perform(put("/api/admin/tags/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Spring Boot"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Spring Boot"));

        mockMvc.perform(get("/api/admin/tags")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.name=='Spring Boot')]").exists());

        mockMvc.perform(delete("/api/admin/tags/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='Spring Boot')]").doesNotExist());
    }

    @Test
    void deleteTagCascadesArticleTags() throws Exception {
        String token = loginAndGetToken();
        Category category = categoryRepository.save(newCategory("标签测试分类"));
        Tag tagA = tagRepository.save(newTag("待删标签"));
        Tag tagB = tagRepository.save(newTag("保留标签"));

        Article article = new Article();
        article.setTitle("多标签文章");
        article.setContent("正文");
        article.setCategory(category);
        article.setPublishedAt(LocalDateTime.now());
        article.setTags(Set.of(tagA, tagB));
        Article saved = articleRepository.saveAndFlush(article);

        mockMvc.perform(delete("/api/admin/tags/" + tagA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(articleRepository.countTagAssociations(tagA.getId())).isZero();
        assertThat(articleRepository.countAssociationsByArticleId(saved.getId())).isEqualTo(1);
        assertThat(tagRepository.existsById(tagA.getId())).isFalse();
        assertThat(tagRepository.existsById(tagB.getId())).isTrue();
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        String token = loginAndGetToken();
        tagRepository.save(newTag("已存在标签"));

        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"已存在标签"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("标签名称已存在"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("name 不能为空"));
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

    private Tag newTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tag;
    }

    private Category newCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}
