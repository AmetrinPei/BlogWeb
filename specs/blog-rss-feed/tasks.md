# Tasks: RSS / Atom 订阅源

> 基于：specs/blog-rss-feed/plan.md v1.2（Implemented）  
> 关联 Spec：specs/blog-rss-feed/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-6：全部 Done

- Task-1：`BlogSiteProperties` + `application.yml`（`blog.site.base-url` / `feed-limit`）
- Task-2：`feed` 包（`FeedController` / `FeedService` / `RssXml`）；`ArticleService#listPublishedForFeed`；`ArticleResponse.resolveSummary` 公开复用
- Task-3：`SecurityConfig` 显式放行 `/feed.xml`；Vite 代理 `/feed.xml`
- Task-4：`FeedPublicTests`；`scripts/acceptance-rss-feed.mjs`
- Task-5：`PublicLayout` alternate link；`SiteFooter`「RSS 订阅」
- Task-6：Spec AC-1～10 已勾选；文档状态 Implemented / Done

验收：`.\mvnw.cmd -Dtest=FeedPublicTests test`；可选 `node scripts/acceptance-rss-feed.mjs`（需后端已启动）。
