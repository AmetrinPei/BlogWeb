package com.example.blog;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleRepository;
import com.example.blog.article.ArticleStatus;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.tag.Tag;
import com.example.blog.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArticlePublicTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private Category categoryJava;
    private Category categoryLife;
    private Tag tagSpring;
    private Tag tagVue;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();

        categoryJava = categoryRepository.save(newCategory("Java"));
        categoryLife = categoryRepository.save(newCategory("Life"));
        tagSpring = tagRepository.save(newTag("Spring"));
        tagVue = tagRepository.save(newTag("Vue"));
    }

    @Test
    void listDefaultsToPageSize10OrderedByPublishedAtDesc() throws Exception {
        for (int i = 1; i <= 15; i++) {
            saveArticle(
                    "文章-" + i,
                    "内容" + i,
                    null,
                    categoryJava,
                    Set.of(tagSpring),
                    LocalDateTime.of(2026, 1, i, 10, 0),
                    ArticleStatus.PUBLISHED
            );
        }

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.items[0].title").value("文章-15"))
                .andExpect(jsonPath("$.data.items[9].title").value("文章-6"))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist());
    }

    @Test
    void listSupportsPagination() throws Exception {
        for (int i = 1; i <= 12; i++) {
            saveArticle(
                    "分页-" + i,
                    "内容",
                    null,
                    categoryJava,
                    Set.of(),
                    LocalDateTime.of(2026, 2, i, 10, 0),
                    ArticleStatus.PUBLISHED
            );
        }

        mockMvc.perform(get("/api/articles").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.items", hasSize(5)))
                .andExpect(jsonPath("$.data.items[0].title").value("分页-7"));
    }

    @Test
    void listFiltersByCategoryTagAndKeyword() throws Exception {
        saveArticle("Spring Boot 入门", "a", null, categoryJava, Set.of(tagSpring), LocalDateTime.of(2026, 3, 1, 10, 0), ArticleStatus.PUBLISHED);
        saveArticle("Vue 组件设计", "b", null, categoryJava, Set.of(tagVue), LocalDateTime.of(2026, 3, 2, 10, 0), ArticleStatus.PUBLISHED);
        saveArticle("生活随笔", "c", null, categoryLife, Set.of(tagVue), LocalDateTime.of(2026, 3, 3, 10, 0), ArticleStatus.PUBLISHED);

        mockMvc.perform(get("/api/articles").param("categoryId", categoryJava.getId().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/articles").param("tagId", tagSpring.getId().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Boot 入门"));

        mockMvc.perform(get("/api/articles").param("keyword", "组件"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Vue 组件设计"));

        mockMvc.perform(get("/api/articles").param("keyword", "不存在的关键词xyz"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void keywordMatchesTitleSummaryOrContentAndHidesDraft() throws Exception {
        saveArticle(
                "AlphaOnlyTitle article",
                "plain body without marker",
                null,
                categoryJava,
                Set.of(),
                LocalDateTime.of(2026, 5, 1, 10, 0),
                ArticleStatus.PUBLISHED
        );
        saveArticle(
                "summary-only title",
                "plain body",
                "contains BetaOnlySummary here",
                categoryJava,
                Set.of(tagSpring),
                LocalDateTime.of(2026, 5, 2, 10, 0),
                ArticleStatus.PUBLISHED
        );
        saveArticle(
                "body-only title",
                "markdown with GammaOnlyBody token",
                null,
                categoryJava,
                Set.of(tagVue),
                LocalDateTime.of(2026, 5, 3, 10, 0),
                ArticleStatus.PUBLISHED
        );
        saveArticle(
                "draft with GammaOnlyBody",
                "GammaOnlyBody in draft",
                null,
                categoryJava,
                Set.of(),
                LocalDateTime.of(2026, 5, 4, 10, 0),
                ArticleStatus.DRAFT
        );

        mockMvc.perform(get("/api/articles").param("keyword", "AlphaOnlyTitle"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("AlphaOnlyTitle article"));

        mockMvc.perform(get("/api/articles").param("keyword", "BetaOnlySummary"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("summary-only title"));

        mockMvc.perform(get("/api/articles").param("keyword", "GammaOnlyBody"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("body-only title"));

        mockMvc.perform(get("/api/articles").param("keyword", "NoMatchZZZ"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "BetaOnlySummary")
                        .param("tagId", tagSpring.getId().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "BetaOnlySummary")
                        .param("tagId", tagVue.getId().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void listExcludesFuturePublishedArticles() throws Exception {
        saveArticle("已发布", "now", null, categoryJava, Set.of(), LocalDateTime.now().minusDays(1), ArticleStatus.PUBLISHED);
        saveArticle("未来发布", "future", null, categoryJava, Set.of(), LocalDateTime.now().plusDays(3), ArticleStatus.PUBLISHED);

        mockMvc.perform(get("/api/articles"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("已发布"));
    }

    @Test
    void detailReturnsCategoryAndTags() throws Exception {
        Article article = saveArticle(
                "详情文章",
                "完整正文内容",
                null,
                categoryJava,
                Set.of(tagSpring, tagVue),
                LocalDateTime.of(2026, 4, 1, 12, 0),
                ArticleStatus.PUBLISHED
        );

        mockMvc.perform(get("/api/articles/" + article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("详情文章"))
                .andExpect(jsonPath("$.data.content").value("完整正文内容"))
                .andExpect(jsonPath("$.data.category.name").value("Java"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)));
    }

    @Test
    void detailMissingOrFutureReturns404() throws Exception {
        Article future = saveArticle(
                "未到发布时间",
                "hidden",
                null,
                categoryJava,
                Set.of(),
                LocalDateTime.now().plusDays(1),
                ArticleStatus.PUBLISHED
        );

        mockMvc.perform(get("/api/articles/" + future.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文章不存在"));

        mockMvc.perform(get("/api/articles/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    private Article saveArticle(
            String title,
            String content,
            String summary,
            Category category,
            Set<Tag> tags,
            LocalDateTime publishedAt,
            ArticleStatus status
    ) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(summary);
        article.setCategory(category);
        article.setTags(tags);
        article.setPublishedAt(publishedAt);
        article.setStatus(status);
        return articleRepository.saveAndFlush(article);
    }

    private Category newCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    private Tag newTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tag;
    }
}