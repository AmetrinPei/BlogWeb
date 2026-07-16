# Feature: 生产配置硬化

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-prod-hardening`、§十 §2.2～2.3 / §8.3-A；constitution.md  
> 依赖：既有认证与站点配置（JWT / 管理员种子 / 注册 / RSS `base-url`）；与 `blog-ops-docker` 配置约定对齐，拓扑与反代不在本期  
> 最后更新：2026-07-15

---

## 1. Problem Statement

标准版产品能力已齐，但仓库仍以**开发默认值**运行：JWT / 管理员 / 数据库口令有明文默认、CORS 仅放行 localhost、`ddl-auto: update`、应用包 DEBUG 日志、公开注册默认开启。直接暴露公网会导致默认口令可登、跨域策略不适配真实 Origin、库结构被随意改写、日志噪音与信息泄露风险，以及开放注册带来的垃圾账号面。

本期交付 **生产向配置与行为硬化**（应用内，非 Docker 拓扑）：

1. 生产 profile（或等价）下，关键密钥须经环境变量注入；缺省或不安全默认值不可带病上线。
2. CORS 允许的 Origin 可配置，支持真实前端域名（不再写死仅 localhost）。
3. 可配置关闭公开注册；关闭后注册入口被拒，开启时行为与现网一致。
4. 生产 `ddl-auto` 不为 `update`；生产日志级别为 INFO。
5. `BLOG_SITE_BASE_URL` 等上线必填项写入部署/检查说明；提供可执行的上线前检查清单（含产品卫生与联合冒烟要点）。

与 `blog-ops-docker` 分工：本 Spec 改**应用与配置行为**及检查清单；反代 / HTTPS / Compose / 卷挂载归 ops。不做密钥轮换中台、找回密码、登录限流（后二者见独立 Spec）。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 密钥强制 | 生产 profile 下无安全 `JWT_SECRET`（及文档约定的其它必填项）时启动失败或明确拒绝带默认密钥运行 | 启动断言 / 文档硬性检查 + 本地复现 |
| CORS 可配 | 可为真实前端 Origin 放行；误配可测 | 配置真实 Origin 后跨域预检/请求成功；未列 Origin 被拒 |
| 注册可控 | 配置关闭后公开注册被拒；开启与现网一致 | API：关→拒绝；开→可注册 AUTHOR |
| 库与日志卫生 | 生产 `ddl-auto` ≠ `update`；root / `com.example.blog` 为 INFO | 读 prod 配置或等价断言 |
| 站点基址可文档化 | `BLOG_SITE_BASE_URL` 写入部署说明；RSS 等依赖该值的路径有检查项 | 检查清单 + 文档抽查 |
| 上线清单可执行 | §2.3 产品卫生与 §2.4 冒烟关键项有可勾选清单（可与 ops 共用） | 清单文件存在且覆盖约定项 |

---

## 3. User Stories

- 作为站点运营者，我希望生产环境必须注入强密钥且拒绝开发默认 JWT，以便公网不被默认口令裸奔
- 作为站点运营者，我希望 CORS 可配置为真实前端 Origin，以便前后端分域或反代场景下接口可用
- 作为站点运营者，我希望能关闭公开注册，以便个人站以邀请/手工开户为主、减少垃圾账号
- 作为站点运营者，我希望生产库表策略与日志级别适合长期运行，以便避免随意改表与 DEBUG 噪音
- 作为部署人员，我希望有一份上线前检查清单（含默认口令不可用、内容与站点配置就绪、RSS/上传可达），以便首发冒烟可重复执行
- 作为系统维护者，我不希望真实密钥写入仓库，以便与现有环境变量约定兼容且可审计

---

## 4. Acceptance Criteria

### 4.1 生产密钥与 profile

- [x] AC-1: 提供 **生产 profile**（建议名 `prod`，或 Plan 锁定的等价机制）。生产运行须通过环境变量注入至少：`JWT_SECRET`、`DB_PASSWORD`、`ADMIN_PASSWORD`（与现有 `application.yml` 占位名兼容；可增其它必填项并由 Plan 列出）
- [x] AC-2: **生产不得静默使用开发默认 JWT secret**（当前默认形如 `blog-mvp-jwt-secret-key-change-me-32bytes-min`）。缺 `JWT_SECRET` 或仍为不安全默认值时：**启动失败**，或启动时明确拒绝并阻止对外服务（二选一由 Plan 锁定；禁止「警告后仍用默认密钥继续跑」）
- [x] AC-3: 真实密钥、口令、连接串 **禁止** 写入仓库；仅允许 `.env.example` / 文档中的占位说明；开发 profile 可保留本地默认值以便日常调试

### 4.2 CORS

- [x] AC-4: CORS **允许的 Origin（或 Origin 模式列表）可配置**（配置项名由 Plan 锁定，如 `blog.cors.allowed-origin-patterns`），**不得**仅硬编码 `http://localhost:*` / `127.0.0.1` 作为生产唯一来源
- [x] AC-5: 配置中的 Origin 可放行对应前端；**未列出的 Origin** 不得被允许（可测）；开发默认可继续覆盖本地 Vite 端口，行为由 Plan 写清（dev 默认 vs prod 必配）

### 4.3 公开注册开关

- [x] AC-6: 提供配置项可 **关闭公开注册**（配置键由 Plan 锁定，如 `blog.auth.public-registration-enabled`）。关闭后，注册接口返回 **403 / 404 / 明确业务拒绝**（择一，Plan 锁定）；**不得** 5xx；不得仍创建用户
- [x] AC-7: 开关为开启（或未显式关闭）时，注册行为与现网一致（用户名唯一、密码规则、角色 AUTHOR 等）；前端注册入口：关闭时可隐藏或提交后展示后端错误（Plan 锁定一种，须避免「页面可填但无提示」的死胡同）

### 4.4 库表策略与日志

- [x] AC-8: 生产 profile 下 Hibernate / JPA **`ddl-auto` 不为 `update`**（允许 `validate`、`none`，或文档化的可控迁移方案；Plan 锁定一种）。开发 profile 可继续 `update`
- [x] AC-9: 生产 profile 下日志级别：`logging.level.root` 与 `com.example.blog` 均为 **INFO**（或更严）；不得默认 DEBUG。密码、Token 全文、连接串密钥仍禁止写入日志（沿用既有安全约定）

### 4.5 站点基址与上线检查

- [x] AC-10: **`BLOG_SITE_BASE_URL`**（及与 RSS / 绝对链接相关的既有约定）写入部署或硬化文档：生产须设为公网 HTTPS 基址；检查清单含「feed 在公网域名下条目正确」等依赖该项的步骤
- [x] AC-11: 提供 **上线前检查清单**（`docx` 专节、独立 md，和/或 `scripts` 可执行说明；可与 `blog-ops-docker` 共用）。至少覆盖第十章：
  - **§2.3 产品卫生**：默认管理员凭据已更换；有真实内容；站点名 / 简介 / 关于 / 友链已填；评论审核策略可用；封面/头像/上传图在生产域名可访问；**注册策略决策**（开放 → 后续 `blog-auth-recovery`；关闭 → 可跳过找回）
  - **§2.4 冒烟要点**（与 ops 联合验收，本 Spec 至少保证「配置侧」可勾选）：未登录访问管理接口 401；默认口令不可再用；HTTPS 下关键页与登录后台的检查项在清单中列出（拓扑由 ops 交付后勾选）

### 4.6 验收与范围边界

- [x] AC-12: 关键路径具备可自动化或可脚本化验收手段（测试、启动校验，和/或验收脚本）：至少覆盖「生产配置缺 JWT 被拒」「CORS 可配 Origin」「注册关闭被拒」「prod 日志/ddl 断言或配置文件抽查」
- [x] AC-13: 不引入 Elasticsearch、Redis、消息队列、对象存储 SDK、SSR；不做 Docker/Nginx 拓扑；不做登录限流与找回密码

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| Docker Compose / Nginx 反代 / HTTPS 证书 / 上传卷 | 归属 `blog-ops-docker` |
| 登录失败限流、搜索限流、验证码 | 归属 `blog-rate-limit` |
| 找回密码 / 邮箱验证 | 归属 `blog-auth-recovery`（且仅开放注册时需要） |
| 完整密钥管理系统、密钥自动轮换中台、HSM | 超出个人博客范围 |
| Actuator 全量暴露、APM、集中日志平台 | 归属 `blog-observability`（P1） |
| 备份策略与 CI 流水线 | 归属 `blog-ops-backup` |
| 修改 RSS / SEO / 友链等产品功能语义 | 已有独立 Spec；本期只要求基址与检查项对齐 |
| 强制所有环境禁止默认管理员种子逻辑本身 | 开发便利保留；生产靠强口令 env + 清单换密 |

---

## 6. Constraints

- 遵守 `constitution.md`：统一响应 `{ code, message, data }`；业务规则在 **Service 层**强制（注册关闭不得仅靠前端隐藏）；不引入禁止技术栈
- **真实密钥禁止进仓库**；环境变量名与现有约定兼容：`JWT_SECRET`、`DB_PASSWORD`、`ADMIN_PASSWORD`、`BLOG_SITE_BASE_URL` 等
- Spec 写 WHAT；profile 名、启动失败 vs 拒绝默认密钥的具体机制、CORS 配置键与列表格式、注册关闭时的 HTTP/业务码、`ddl-auto` 取值、前端注册入口表现、清单落盘路径由 **Plan** 锁定
- 与 `blog-ops-docker` 的 `.env.example` / 部署文档约定宜一致，避免两套 env 名；本 Spec 可先定应用侧约定，ops 跟从
- 不破坏本地开发默认路径（无 prod profile 时仍可按 `docx/启动方式.md` 启动）
- 关键路径验收与 PR 说明引用 `blog-prod-hardening` 与 Task 编号

---

## 7. 附录

### 7.1 术语表

| 术语 | 说明 |
| --- | --- |
| 生产 profile | Spring（或等价）用于公网运行的配置集，与本地开发默认隔离 |
| 配置硬化 | 关闭或约束不适合公网的开发默认值（密钥、CORS、注册、ddl、日志等） |
| 公开注册 | 访客可调用的自助注册为 AUTHOR 的能力 |
| 上线检查清单 | 首发前可勾选的产品卫生与冒烟项；可与 ops 联合使用 |
| 站点基址 `BLOG_SITE_BASE_URL` | 公网站点根 URL；RSS 等绝对链接依赖 |

### 7.2 与既有 Spec 关系

- 承接需求文档 §九 `blog-prod-hardening` 与 §十 §2.2～2.3、§8.3-A
- **先于或并行设计** `blog-ops-docker`：应用 env 与 CORS/注册约定宜先定；联合冒烟 §2.4 两边勾选
- 不替代 `blog-rate-limit` Slice-A（登录限流仍为上线 P0 另一条）
- 注册关闭时可跳过后续 `blog-auth-recovery`；开放注册则上线后应摘取找回
- 日志级别硬化与 `blog-observability` 互补：本期只定生产 INFO；健康检查与访问日志字段留给 observability

### 7.3 现状基线（开 Spec 时）

| 能力 | 现状 |
| --- | --- |
| 配置文件 | 单一 `application.yml`；无独立 `application-prod.yml` |
| JWT | `${JWT_SECRET:blog-mvp-jwt-secret-key-change-me-32bytes-min}`；无生产拒绝默认值逻辑 |
| 管理员 | `${ADMIN_PASSWORD:admin123}`；用户名默认 `admin` |
| DB | `${DB_PASSWORD:root}`；`ddl-auto: update` |
| CORS | `CorsConfig` 写死 `localhost` / `127.0.0.1` Origin 模式 |
| 注册 | `POST /api/auth/register` 常开；Security 白名单放行 |
| 日志 | `root: INFO`，`com.example.blog: DEBUG` |
| 站点基址 | `${BLOG_SITE_BASE_URL:http://localhost:5173}` |
| 上线清单 | 仅需求文档第十章叙述；仓库无独立可执行检查清单 |
| 部署文档 | `docx/启动方式.md` 仅本地开发 |

### 7.4 建议 Tasks（摘自第十章 §8.3-A，供 Plan/tasks 落地）

| Task | 描述 | 交付物 | 验收要点 |
| --- | --- | --- | --- |
| T1 | 增加 `prod`（或等价）配置：日志 INFO、ddl 策略、必填 env 说明与校验 | `application-prod.yml` 或文档 + 启动校验 | 缺/默认 `JWT_SECRET` 时启动失败或硬性拒绝 |
| T2 | CORS 改为可配置 Origin 列表/模式 | `CorsConfig` + 配置项 | 非 localhost Origin 可放行；未列 Origin 可测拒绝 |
| T3 | 公开注册开关 | 配置 + Auth/Security 行为（+ 前端表现） | 关闭后注册被拒；开启与现网一致 |
| T4 | 上线检查清单与冒烟说明（可与 ops 共用） | `docx` 或 `scripts` | 覆盖 §2.3～2.4 关键项 |

**推荐落地顺序：** T1 → T2 → T3 → T4

### 7.5 Plan 已锁定（摘要）

| 项 | 锁定值 |
| --- | --- |
| Profile | `prod`（`spring.profiles.active=prod`） |
| 缺/弱密钥 | **启动失败**（`ProdSecurityValidator` + YAML 无弱默认） |
| `ddl-auto` | 生产 **`validate`**；不引入 Flyway |
| 日志 | 生产 root / `com.example.blog` = **INFO** |
| CORS | `blog.cors.allowed-origin-patterns`；env `BLOG_CORS_ALLOWED_ORIGIN_PATTERNS` 逗号分隔 |
| 注册开关 | `blog.auth.public-registration-enabled`；生产默认 `false` |
| 注册关闭响应 | HTTP 200 + 业务 `code=403`，文案「公开注册已关闭」 |
| 开关下发 | `GET /api/site` → `publicRegistrationEnabled`；前端隐藏链接 + RegisterView 禁用 |
| 文档 | `.env.example` + `docx/上线检查清单.md` |
| 验收 | `ProdHardeningTests` + `scripts/acceptance-prod-hardening.mjs` |

详见 `plan.md`。

### 7.6 变更记录

| 版本 | 日期 | 变更说明 | 作者 |
| --- | --- | --- | --- |
| v1.0 | 2026-07-15 | 自需求文档 §九卡片与 §十 §2.2～2.3 / §8.3-A 起草 Draft；锁定应用侧硬化边界（密钥/CORS/注册/ddl/日志/清单），Docker/限流/找回为 Non-Goals | |
| v1.1 | 2026-07-15 | Spec ↔ Plan 对齐 Approved；附录锁定 prod 启动失败、validate+INFO、CORS/注册开关、清单路径；plan/tasks 齐套 | |
| v1.2 | 2026-07-15 | Implemented；AC 勾选；ProdHardeningTests + acceptance-prod-hardening.mjs；prod profile / CORS / 注册开关 / 上线清单交付 | |
