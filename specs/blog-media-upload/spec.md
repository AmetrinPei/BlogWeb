# Feature: 媒体上传

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-media-upload`；constitution.md  
> 依赖：specs/blog-content-enhance（`coverUrl`、Markdown 正文）；specs/blog-auth-rbac（ADMIN / AUTHOR）  
> 最后更新：2026-07-14

---

## 1. Problem Statement

标准版 `blog-content-enhance` 仅允许为封面与正文填写**外部 URL**，作者无法将图片自托管到本站；管理端「封面 URL」依赖外链可用性，Markdown 插图同样只能贴第三方地址，既不稳定也不便本地联调。

本期交付**图片媒体上传（本地磁盘）**：认证的 ADMIN / AUTHOR 可上传白名单类型图片，获得可公开访问的 URL，并写入 `coverUrl` 或 Markdown 正文；超限与非法类型被拒绝；禁止路径穿越与不安全文件名。不做视频、CDN、对象存储 SDK、自动清孤儿文件。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 可上传获 URL | 认证用户上传允许类型图片后返回可访问 URL | 上传成功响应含 URL；随后 GET 该 URL 可读到图片 |
| 类型白名单 | 非白名单扩展名 / Content-Type 被拒绝 | 上传 `.exe` / 伪造类型，断言业务错误（非 5xx） |
| 大小上限 | 超过约定大小被拒绝 | 超限文件上传断言明确错误 |
| 可写入内容 | 返回 URL 可写入 `coverUrl` 或 MD，公开页可展示 | 管理端设封面或正文插图后访客可见 |
| 权限清晰 | 仅 ADMIN / AUTHOR 可上传；未登录 401；其它角色或无权限 403 | 角色与无 Token 用例 |
| 路径安全 | 文件名/路径无法穿越出约定存储根目录 | 构造 `../` 等恶意名，断言拒绝或不落盘到根外 |

---

## 3. User Stories

- 作为作者，我希望上传封面图并得到本站 URL，以便不依赖外链
- 作为作者，我希望把上传得到的 URL 插入 Markdown，以便正文插图自托管
- 作为管理员，我希望与作者一样能上传图片，以便维护全站内容
- 作为访客，我希望封面与正文中的本站图片可直接加载，以便正常阅读
- 作为系统，我希望拒绝非法类型、超大文件与路径穿越，以便降低滥用与安全风险
- 作为未登录用户，我不应成功上传文件

---

## 4. Acceptance Criteria

- [x] AC-1: 提供**需登录**的图片上传接口（multipart）；成功时统一 JSON `{ code, message, data }`，`data` 至少含可公开访问的 **`url`**（字符串）；可选含原始文件名、大小、Content-Type 等元数据（Plan 固定字段名）
- [x] AC-2: **权限**：仅 **`ADMIN`** 与 **`AUTHOR`** 可调用上传接口；未登录返回 **401**；其它身份（若存在）返回 **403**；权限在 **Service 层**校验
- [x] AC-3: **类型白名单**：至少允许常见图片格式 **`image/jpeg`、`image/png`、`image/gif`、`image/webp`**（扩展名与声明的 Content-Type 须同时落在白名单；具体校验策略由 Plan 固定，但不得仅信任客户端扩展名而不校验内容类型）；白名单外拒绝写入并返回明确业务错误（如 `code=400`）
- [x] AC-4: **大小上限**：单文件超过配置上限时拒绝；默认上限由 Plan 给出可配置值（建议起点：**≤ 8 MiB**）；错误信息须表明超限，不得 5xx
- [x] AC-5: **存储方案（本期）**：文件落在**服务端本地磁盘**约定目录；**不引入** OSS / MinIO / 其它对象存储 SDK；不因此修订 constitution 的「禁止对象存储 SDK」条款
- [x] AC-6: **返回 URL** 须为相对站点可引用的路径或绝对 URL，且可直接用于：
  - 文章 `coverUrl` 字段；
  - Markdown 正文中的图片引用（如 `![alt](url)`）；
  公开列表/详情/管理端预览加载该 URL 时能拿到图片字节（HTTP 200，Content-Type 为对应图片类型）
- [x] AC-7: **公开读**：上传成功后的媒体资源**无需登录**即可 GET（与封面/正文对访客可见一致）；不存在的路径返回 404
- [x] AC-8: **禁止路径穿越与不安全落盘**：客户端提供的文件名不得决定最终相对存储根的路径层级；须经服务端重写为安全名（如 UUID + 白名单扩展名）；任何 `..`、绝对路径、目录分隔注入不得写到存储根之外；探测性请求不得泄露存储根绝对路径给客户端
- [x] AC-9: **不覆盖文章写入契约**：上传接口**只负责**存文件并返回 URL；是否写入某篇文章的 `coverUrl` / `content` 仍走既有文章创建/更新接口；上传成功不自动改文章
- [x] AC-10: 管理端文章编辑（封面与/或正文）提供可用的上传入口：选择图片 → 调用上传 → 将返回 URL 填入 `coverUrl` 或插入 Markdown；AUTHOR 与 ADMIN 均可使用（与文章编辑权限一致的入口即可）
- [x] AC-11: 空文件、缺少文件部分、非 multipart 请求返回明确业务错误（非 5xx）；并发上传互不影响彼此已成功文件
- [x] AC-12: 关键路径具备可自动化验收手段（测试或脚本）：至少覆盖成功上传并可 GET、非法类型拒绝、超限拒绝、未登录 401、非 AUTHOR/ADMIN 403（若可构造）、路径穿越/恶意文件名不落盘到根外、返回 URL 可写入封面或 MD 后公开可见

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| 视频 / 音频 / 任意附件上传 | 卡片明确本期仅图片 |
| CDN、图片缩放/缩略图、WebP 转码流水线 | 超出本期；可后续增量 |
| OSS / MinIO / 云对象存储 SDK | **须改 constitution**；本期本地磁盘即可 |
| 删除文章后自动清孤儿文件、引用计数、媒体库 CRUD 后台 | 卡片列为后续；本期不做引用管理台 |
| 私有桶、签名 URL、按用户隔离的私有读 | 本期资源对访客公开可读 |
| 修改 `coverUrl` 字段语义为强制本站 URL（禁止外链） | 外链仍可用；上传为增量能力 |
| 富文本编辑器、拖拽图床产品化 | 保持 Markdown + 管理端简单上传 |
| 用户头像专用接口 | 见 `blog-user-profile`；可复用本期返回的 URL 模式，但不在本期做资料页 |
| 上传频率全站限流中台 | 可后续与 `blog-rate-limit` 合并；本期以类型/大小校验为主 |

---

## 6. Constraints

- 必须遵守 `constitution.md`：不引入 Elasticsearch、Redis、消息队列、**对象存储 SDK**、SSR；业务规则在 Service 层强制执行
- **本期存储 = 本地磁盘**；若未来改为 OSS/MinIO，须先修订 constitution 与本 Spec 再实现
- constitution §4 中「标准版封面仅允许 URL、不落地文件上传」约束被本增量 **显式撤销**（仅针对本 Spec 范围的上传能力）；类型与大小校验仍为硬性要求
- 统一响应格式：上传成功/业务失败走 `{ code, message, data }`；**静态文件 GET** 为原始字节流（非 JSON），与 RSS 等非 JSON 公开资源同理
- 日志禁止输出完整本地绝对路径中的敏感部署信息（若需排错可记相对 key / 文件 id）；禁止记录 Token 全文
- 兼容既有 `coverUrl` 外链与 Markdown 外链图片；上传 URL 与外链可并存
- 关键路径验收与 PR 说明引用 `blog-media-upload` 与 Task 编号

---

## 7. 附录

### 7.1 术语表

| 术语 | 说明 |
| --- | --- |
| 媒体上传 | 将图片文件经认证接口写入本站存储并返回可访问 URL |
| 类型白名单 | 允许的图片 MIME / 扩展名集合；之外一律拒绝 |
| 存储根 | 本地磁盘上约定的上传目录；所有落盘路径须落在其下 |
| 安全文件名 | 由服务端生成、不含路径分隔与 `..` 的落盘名 |
| 可写入 URL | 可直接赋给 `coverUrl` 或嵌入 Markdown 的公开访问地址 |

### 7.2 与既有 Spec 关系

- 承接并**撤销** `blog-content-enhance` Non-Goals 中「图片本地/OSS 上传；封面仅 URL」的本地上传部分；`coverUrl` 字段与外链能力保留
- 与 `blog-user-profile`：头像若用 URL，可复用本上传返回的 URL；资料页本身不在本期
- 与 `blog-ops-docker`：本地目录挂载/持久化由运维 Spec 或 Plan 说明，本期 Spec 只要求行为正确
- **不**引入对象存储，避免与 constitution 冲突

### 7.3 现状基线（开 Spec 时）

| 能力 | 现状 |
| --- | --- |
| 封面 | 文章 `coverUrl` 字符串；管理端手填外链 |
| 正文图 | Markdown 中手写外链 `![](url)` |
| 上传 API | 无 |
| 静态资源目录 | 无约定的用户上传落盘与公开映射 |
| 角色 | `ADMIN` / `AUTHOR` 已存在（`blog-auth-rbac`） |

### 7.4 建议默认值（WHAT 级；Plan 可微调并锁定）

| 项 | 建议 |
| --- | --- |
| 允许 MIME | `image/jpeg`、`image/png`、`image/gif`、`image/webp` |
| 单文件上限 | 8 MiB |
| 存储 | 本地目录（配置项，如 `blog.upload.dir`） |
| 公开路径前缀 | 由 Plan 固定（如 `/uploads/**` 或 `/api/media/**`） |

### 7.5 变更记录

| 版本 | 日期 | 变更说明 | 作者 |
| --- | --- | --- | --- |
| v1.0 | 2026-07-14 | 自需求文档 §九卡片起草 Draft；锁定本地磁盘、图片白名单、大小上限、ADMIN/AUTHOR、公开可读 URL、禁路径穿越；OSS/视频/CDN/孤儿清理不做 | |
| v1.1 | 2026-07-14 | Spec / Plan / Tasks 齐套 Approved；Plan 锁定 `/api/admin/media`、`/uploads/**`、8MiB、相对 URL、魔数双检 | |
| v1.2 | 2026-07-14 | Implemented；AC 勾选；`MediaUploadTests` + `acceptance-media-upload.mjs` | |
