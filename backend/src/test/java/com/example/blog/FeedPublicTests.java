package com.example.blog;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleRepository;
import com.example.blog.article.ArticleStatus;
import com.example.blog.category.Category;
import com.example.blog.category.CategoryRepository;
import com.example.blog.config.BlogSiteProperties;
import com.example.blog.site.SiteSettings;
import com.example.blog.site.SiteSettingsRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedPublicTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    @Autowired
    private BlogSiteProperties siteProperties;

    private Category category;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("FeedCat");
        category = categoryRepository.save(category);

        SiteSettings settings = siteSettingsRepository.findById(1L).orElseGet(() -> {
            SiteSettings created = new SiteSettings();
            created.setId(1L);
            return created;
        });
        settings.setSiteName("Feed Test Blog");
        settings.setTagline("feed tagline");
        siteSettingsRepository.save(settings);
    }

    @Test
    void feedReturnsRssHidesNonPublicAndEscapesXml() throws Exception {
        saveArticle("Draft Hidden", "draft body", null, LocalDateTime.of(2026, 6, 1, 10, 0), ArticleStatus.DRAFT);
        saveArticle("Offline Hidden", "offline body", null, LocalDateTime.of(2026, 6, 2, 10, 0), ArticleStatus.OFFLINE);
        Article published = saveArticle(
                "Tom & Jerry",
                "body with <tag>",
                "summary & more",
                LocalDateTime.of(2026, 6, 3, 10, 0),
                ArticleStatus.PUBLISHED
        );
        saveArticle("Future Hidden", "future", null, LocalDateTime.now().plusDays(2), ArticleStatus.PUBLISHED);

        MvcResult result = mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/rss+xml")))
                .andReturn();

        String xml = result.getResponse().getContentAsString();
        assertThat(xml).contains("<rss version=\"2.0\"");
        assertThat(xml).contains("<title>Feed Test Blog</title>");
        assertThat(xml).contains("<description>feed tagline</description>");
        assertThat(xml).doesNotContain("Draft Hidden");
        assertThat(xml).doesNotContain("Offline Hidden");
        assertThat(xml).doesNotContain("Future Hidden");
        assertThat(xml).contains("Tom &amp; Jerry");
        assertThat(xml).contains("summary &amp; more");
        assertThat(xml).doesNotContain("Tom & Jerry");
        assertThat(xml).contains("+0800");

        String base = siteProperties.resolvedBaseUrl();
        assertThat(xml).contains("<link>" + base + "/</link>");
        assertThat(xml).contains(base + "/feed.xml");
        assertThat(xml).contains(base + "/articles/" + published.getId());
        assertThat(countOccurrences(xml, "<item>")).isEqualTo(1);
    }

    @Test
    void feedRespectsItemLimitNewestFirst() throws Exception {
        int limit = siteProperties.resolvedFeedLimit();
        for (int i = 1; i <= limit + 3; i++) {
            saveArticle(
                    "Extra-" + i,
                    "content " + i,
                    "sum " + i,
                    LocalDateTime.of(2026, 7, 1, 10, 0).plusHours(i),
                    ArticleStatus.PUBLISHED
            );
        }

        String xml = mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(countOccurrences(xml, "<item>")).isEqualTo(limit);
        assertThat(xml).contains("<title>Extra-" + (limit + 3) + "</title>");
        assertThat(xml).doesNotContain("<title>Extra-1</title>");
    }

    @Test
    void emptyFeedStillHasChannelMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andReturn();
        String xml = result.getResponse().getContentAsString();
        assertThat(xml).contains("<channel>");
        assertThat(xml).contains("<title>Feed Test Blog</title>");
        assertThat(xml).doesNotContain("<item>");
    }

    @Test
    void channelTitleReflectsSiteSettingsUpdate() throws Exception {
        SiteSettings settings = siteSettingsRepository.findById(1L).orElseThrow();
        settings.setSiteName("Renamed Cosmos");
        siteSettingsRepository.save(settings);

        mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<title>Renamed Cosmos</title>")));
    }

    @Test
    void itemUsesSummaryFallbackFromContent() throws Exception {
        String longBody = "A".repeat(200);
        Article article = saveArticle(
                "Fallback Summary",
                longBody,
                null,
                LocalDateTime.of(2026, 6, 10, 12, 0),
                ArticleStatus.PUBLISHED
        );

        String xml = mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(xml).contains(siteProperties.resolvedBaseUrl() + "/articles/" + article.getId());
        assertThat(xml).contains("<description>" + "A".repeat(120) + "…</description>");
    }

    private Article saveArticle(
            String title,
            String content,
            String summary,
            LocalDateTime publishedAt,
            ArticleStatus status
    ) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(summary);
        article.setCategory(category);
        article.setPublishedAt(publishedAt);
        article.setStatus(status);
        return articleRepository.saveAndFlush(article);
    }

    private static int countOccurrences(String haystack, String needle) {
        Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(haystack);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
