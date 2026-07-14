# Plan: 评论审核与反垃圾

> 基于：specs/blog-comment-moderation/spec.md v1.2（Implemented）  
> 状态：Implemented  
> 最后更新：2026-07-14

---

## 1. 方案概述

在既有评论域上叠加**审核状态 + 敏感词 + 发评限流**，不改动楼中楼深度与点赞枚举：

- `comments.status`：`PENDING` / `APPROVED` / `REJECTED`；旧数据默认 `APPROVED`
- 发表时：先限流 → 再校验父评论须为 `APPROVED`（若有楼中楼）→ 敏感词子串匹配决定初始状态（未命中 `APPROVED`，命中 `PENDING`）
- 公开列表仅组装 `APPROVED`；创建响应带回 `status`
- 新表 `sensitive_words`；ADMIN CRUD + 待审队列与状态变更
- 发评限流：按 `userId` 统计近 1 分钟创建条数，默认上限 **5**（可配置）；超限 `code=400`
- 管理端待审 / 敏感词页；访客端对待审创建给出提示

不引入 Redis、验证码、HTTP 429；登录/搜索限流仍留给 `blog-rate-limit`。

---

## 2. 架构设计

### 2.1 模块划分

| 模块 | 职责 |
| --- | --- |
| `comment.CommentStatus` | 枚举 `PENDING` / `APPROVED` / `REJECTED` |
| `comment.Comment` | 字段 `status`；`@PrePersist` 缺省 `APPROVED` |
| `comment.CommentResponse` | 增加 `status` |
| `comment.CommentRepository` | 公开列表可按文章+状态查；ADMIN 按状态分页；限流用「用户近窗创建数」计数 |
| `comment.CommentService` | 创建：限流 → 父评 APPROVED → 敏感词定态；列表仅 APPROVED 组树；日志不打正文 |
| `comment.CommentModerationService`（或并入 Service） | ADMIN 按状态列表、变更状态（幂等） |
| `comment.SensitiveWord` + Repository + Service | 词表 CRUD；命中检测（大小写不敏感子串） |
| `comment.CommentAdminController` | `/api/admin/comments` 列表与改状态 |
| `comment.SensitiveWordController` | `/api/admin/sensitive-words` |
| `config` / `application.yml` | `blog.comment.rate-limit-per-minute`（默认 5） |
| `like.LikeService` | 评论目标须存在且 `APPROVED`，否则 404 |
| 管理端 | 待审评论页、敏感词页、侧栏入口、API 封装 |
| 访客端 | 创建后若 `PENDING` 提示待审；列表依赖后端过滤 |
| 验收 | `CommentModerationTests` + `scripts/acceptance-comment-moderation.mjs` |

仍落在 constitution 已有 `comment` / `like` / `config` 包，**不**新增 domain 包名。

### 2.2 数据模型

```text
comments
├── …既有字段…
├── status              VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
└── INDEX idx_comments_status (status)
└── INDEX idx_comments_user_created (user_id, created_at)

sensitive_words
├── id                  BIGINT PK AI
├── word                VARCHAR(64) NOT NULL  UNIQUE uk_sensitive_words_word
└── created_at          DATETIME NOT NULL
```

| 决策 | 说明 |
| --- | --- |
| Schema | `ddl-auto: update`；`status` 用 `@Enumerated(STRING)` + `nullable = false`；列默认 / 启动补偿保证旧行为 `APPROVED`（AC-11） |
| 敏感词唯一 | `word` 唯一；入库前 trim；比较用小写，存储统一小写 |
| 词长上限 | `@Size(min = 1, max = 64)` |
| 限流 | **DB 计数**：近 1 分钟创建数 ≥ 上限则拒绝 |
| 默认上限 | `blog.comment.rate-limit-per-minute: 5`；≤0 关闭限流 |

### 2.3～2.7

接口、流程、前端、验收与手工走查见实现代码与 Spec AC；关键路径已由 `CommentModerationTests` 覆盖。

---

## 3. 技术选型

| 决策点 | 选型 | 理由 |
| --- | --- | --- |
| 状态存储 | 枚举列 `status` | Spec 三态 |
| 默认策略 | 未命中自动 APPROVED | Spec AC-2 |
| 命中策略 | 入库 PENDING | Spec AC-2 |
| 敏感词匹配 | 小写子串 contains | Spec AC-6 |
| 限流 | DB 近 1 分钟 count | 无 Redis |
| 超频错误码 | 400 + 明确 message | 不改 constitution |

---

## 4. 风险与备选方案

| 风险 | 缓解 |
| --- | --- |
| 旧行 status NULL | 列 DEFAULT + `CommentStatusBackfillRunner` |
| 子串误杀 | ADMIN 可过审；词表自控 |
| 多实例限流不共享 | Non-Goals；单实例足够 |

---

## 5. 与 Constitution 的对齐检查

- [x] 不引入 Elasticsearch / Redis / 消息队列 / OSS / SSR
- [x] 查询用 JPA / 参数化；限流用 Repository count
- [x] 审核与词表权限：Security `ADMIN`
- [x] 统一 JSON；超频用 400 非 429
- [x] 关键路径自动化验收

---

## 6. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | 初稿 Approved |
| v1.1 | 2026-07-14 | Implemented；测试通过 |
