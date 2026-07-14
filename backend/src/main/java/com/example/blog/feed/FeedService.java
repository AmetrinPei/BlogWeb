package com.example.blog.feed;

import com.example.blog.article.Article;
import com.example.blog.article.ArticleResponse;
import com.example.blog.article.ArticleService;
import com.example.blog.config.BlogSiteProperties;
import com.example.blog.site.SiteService;
import com.example.blog.site.SiteSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class FeedService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter RFC822 = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final ArticleService articleService;
    private final SiteService siteService;
    private final BlogSiteProperties siteProperties;

    public FeedService(
            ArticleService articleService,
            SiteService siteService,
            BlogSiteProperties siteProperties
    ) {
        this.articleService = articleService;
        this.siteService = siteService;
        this.siteProperties = siteProperties;
    }

    @Transactional(readOnly = true)
    public String buildRss() {
        SiteSettings settings = siteService.requireSettings();
        String baseUrl = siteProperties.resolvedBaseUrl();
        int limit = siteProperties.resolvedFeedLimit();

        List<Article> articles = articleService.listPublishedForFeed(limit);

        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("<channel>\n");
        xml.append("<title>").append(RssXml.escape(settings.getSiteName())).append("</title>\n");
        xml.append("<link>").append(RssXml.escape(baseUrl + "/")).append("</link>\n");
        xml.append("<description>").append(RssXml.escape(nullToEmpty(settings.getTagline()))).append("</description>\n");
        xml.append("<atom:link href=\"")
                .append(RssXml.escape(baseUrl + "/feed.xml"))
                .append("\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        LocalDateTime lastBuild = articles.isEmpty()
                ? LocalDateTime.now()
                : articles.get(0).getPublishedAt();
        xml.append("<lastBuildDate>").append(formatRfc822(lastBuild)).append("</lastBuildDate>\n");

        for (Article article : articles) {
            String itemLink = baseUrl + "/articles/" + article.getId();
            String description = ArticleResponse.resolveSummary(article);
            xml.append("<item>\n");
            xml.append("<title>").append(RssXml.escape(article.getTitle())).append("</title>\n");
            xml.append("<link>").append(RssXml.escape(itemLink)).append("</link>\n");
            xml.append("<guid isPermaLink=\"true\">").append(RssXml.escape(itemLink)).append("</guid>\n");
            xml.append("<description>").append(RssXml.escape(description)).append("</description>\n");
            xml.append("<pubDate>").append(formatRfc822(article.getPublishedAt())).append("</pubDate>\n");
            xml.append("</item>\n");
        }

        xml.append("</channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    private static String formatRfc822(LocalDateTime dateTime) {
        ZonedDateTime zoned = dateTime.atZone(ZONE);
        return RFC822.format(zoned);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
