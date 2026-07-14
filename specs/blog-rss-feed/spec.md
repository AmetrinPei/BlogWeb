# Feature: RSS / Atom 订阅源

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-rss-feed`；constitution.md  
> 依赖：specs/personal-blog（MVP）；specs/blog-content-enhance（三态可见性、`summary`、公开仅已发布）；specs/blog-site-experience（`siteName` / `tagline`）  
> 最后更新：2026-07-14

---

## 1. Problem Statement

读者无法用阅读器（Feedly、Inoreader 等）订阅本站更新，只能反复打开站点查看是否有新文。`blog-site-experience` 已将 Newsletter / RSS 列为 Non-Goals，站点名与简介可后台维护，但尚无机器可读的文章订阅源。

本期交付**公开 RSS 2.0 订阅源**：合法 XML feed；条目为最近 N 篇公开可见的已发布文章，含标题、链接、摘要、发布时间；频道级站点名/简介取自站点配置。不引入邮件推送。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 格式合法 | 输出合法 **RSS 2.0** | 用标准阅读器或校验器可解析；自动化断言 Content-Type 与根元素 |
| 条目覆盖 | feed 含最近 N 篇已发布文章的标题、绝对链接、摘要、发布时间 | 造 ≥N+1 篇已发布样本，断言条数上限与字段齐全 |
| 可见性 | 草稿、已下架不出现在 feed | 同题草稿 + 已发布各 1 篇，feed 仅含已发布 |
| 站点元数据 | 频道/Feed 标题与简介来自站点配置 | 修改 `siteName` / `tagline` 后重新请求 feed 可见新值 |
| 公开可订阅 | 未登录即可 `GET` feed | 无 Token 请求返回 200 与 XML 正文 |

---

## 3. User Stories

- 作为访客，我希望用阅读器订阅本站，以便在站外收到新文章更新
- 作为访客，我希望订阅源只包含已发布文章，以便不看到草稿或下架内容
- 作为管理员，我希望 feed 中的站点名与简介与后台站点配置一致，以便品牌展示统一
- 作为访客，我希望在站点页能发现订阅入口（如页头 `alternate` 链接或页脚入口），以便一键复制/添加 feed URL

---

## 4. Acceptance Criteria

- [x] AC-1: 提供公开 `GET` feed 端点（路径由 Plan 固定，如 `/feed.xml` 或 `/atom.xml`）；**无需登录**；成功时返回 **XML**（非统一 JSON `{ code, message, data }`），HTTP 200
- [x] AC-2: 输出合法 **RSS 2.0**（本期不做 Atom）；`Content-Type` 为 `application/rss+xml`（charset=UTF-8）；文档须可被常见阅读器解析
- [x] AC-3: feed 条目仅包含公开可见的已发布文章（与现有公开列表可见性规则一致，含状态与发布时间约束）；`DRAFT` / `OFFLINE` 及不可见文章**不出现**
- [x] AC-4: 条目按发布时间倒序；条数上限为最近 **N** 篇（默认 **20**，硬上限 **50**，见 Plan）；无已发布文章时返回合法空 feed（频道元数据仍在，条目为空）
- [x] AC-5: 每条至少包含：**标题**、**文章详情绝对 URL**（`{baseUrl}/articles/{id}`）、**摘要**（优先 `summary`；空则与公开列表一致截断正文）、**发布时间**（`publishedAt`，Asia/Shanghai，RFC-822）
- [x] AC-6: 频道/Feed 级 **站点名**、**简介** 取自站点配置（`siteName`、`tagline`）；缺省时有明确降级默认值（与实体/前台降级一致）
- [x] AC-7: 文章链接与 feed 相关链接均为**绝对 URL**；公开站点根 URL 来自配置 `blog.site.base-url`（环境变量可覆盖）；空白时回退文档化本地默认，**禁止**相对 item link
- [x] AC-8: XML 内容编码为 **UTF-8**；标题/摘要中的特殊字符正确转义，不因 `&` `<` 等破坏 XML 合法性
- [x] AC-9: 访客端提供订阅发现：公开页 `<head>` 增加 `rel="alternate"` 指向 feed，**且**页脚展示 RSS 入口；不要求内嵌完整阅读器 UI
- [x] AC-10: 关键路径具备可自动化验收手段（测试或脚本）：断言仅已发布、条数上限、站点名来自配置、响应为合法 XML

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| Newsletter / 邮件推送 | 另开 `blog-newsletter` |
| 同时维护 RSS + Atom 双源（除非 Plan 证明零成本） | 本期一种合法格式即可 |
| 全文 HTML 正文进 feed | 摘要足够；全文增大体积与 XSS 面 |
| 按分类/标签的独立子 feed | 超出本期 |
| `sitemap.xml` / Open Graph / SSR | 留给 `blog-seo` |
| feed 阅读量统计、订阅者管理 | 超出本期 |
| Redis 缓存 feed | 须改 constitution；非本期 |
| 修改文章状态机或公开列表 API 契约 | 仅新增订阅输出 |

---

## 6. Constraints

- 必须遵守 `constitution.md`：不引入 Elasticsearch、Redis、消息队列；业务查询用 JPA / 参数化，禁止 SQL 拼接
- feed 端点是 constitution「REST + JSON」的**显式例外**：该路径返回 XML；其它 API 仍为统一 JSON
- 公开可见性不得弱于 `blog-content-enhance`（已发布且按现有发布时间可见性）
- 站点名/简介依赖 `blog-site-experience` 已有站点配置；**站点绝对 URL** 本期必须在 Plan 中落地配置方案（现有 `site_settings` 无 base URL 字段时允许扩展或使用 `application` 配置）
- Markdown / HTML 正文**不**要求进入 feed；若摘要含需转义字符，须在服务端正确 XML escape（不等同于页面 XSS 消毒，但不得输出残缺 XML）
- 关键路径须有可自动化验收手段；PR / 提交说明引用 `blog-rss-feed` 与 Task 编号

---

## 7. 附录

### 7.1 术语表

| 术语 | 说明 |
| --- | --- |
| feed | 供阅读器订阅的 RSS 或 Atom XML 文档 |
| 公开可见 | 对未登录访客可通过公开列表/详情访问的已发布文章（含现有 `publishedAt` 约束） |
| N | feed 最多收录的最近已发布文章数 |
| 站点绝对 URL / base URL | 用于拼接文章详情与 feed 自链接的公开站点根地址（含协议与主机，无尾斜杠或约定统一） |

### 7.2 与既有 Spec 关系

- 承接 `blog-site-experience` Non-Goals 中的 RSS；复用其 `siteName` / `tagline`
- 复用 `blog-content-enhance` 的三态与 `summary`、公开可见性规则
- 与 `blog-seo`（sitemap / OG）互补，不互相替代
- 与 `blog-newsletter` 隔离：本期仅被动拉取的 XML 订阅源

### 7.3 变更记录

| 版本 | 日期 | 变更说明 | 作者 |
| --- | --- | --- | --- |
| v1.0 | 2026-07-14 | 自需求文档 §九卡片起草 Draft | |
| v1.1 | 2026-07-14 | Spec / Plan / Tasks 齐套；锁定 RSS 2.0、`/feed.xml`；Approved | |
| v1.2 | 2026-07-14 | Implemented；AC 勾选；`FeedPublicTests` + `acceptance-rss-feed.mjs` | |
