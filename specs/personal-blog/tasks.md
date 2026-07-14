# Tasks: 个人博客（基础版 MVP）

> 基于：specs/personal-blog/plan.md v1.2  
> 关联 Spec：specs/personal-blog/spec.md v1.0（Approved）  
> 状态：Done  
> 最后更新：2026-07-13

---

## 任务列表

### Task-1: 后端工程骨架与通用层

- **描述**：初始化 Spring Boot 3 工程；实现统一响应
  `Result(code/message/data)`、业务异常、全局异常处理、参数校验与
  CORS；约定错误码 0/400/401/404/409/500。
- **交付物**：后端工程骨架；`common` 包（Result、异常、全局处理器）；
  基础配置（application.yml、CORS）
- **验收标准**：任意 Controller 返回统一 JSON 结构；非法参数返回可读
  `message` 且不暴露堆栈（AC-11、AC-12、AC-13）
- **依赖**：无
- **状态**：[ ] Todo  [x] Done

### Task-2: 数据模型与持久化

- **描述**：按 plan 建立 `users` / `categories` / `tags` / `articles` /
  `article_tags` 实体与 Repository；配置必要索引；提供启动时管理员
  种子数据（BCrypt 密码）。
- **交付物**：JPA Entity、Repository、DDL/自动建表配置、管理员种子
- **验收标准**：应用启动可连 MySQL 并建表；存在可用管理员账号
- **依赖**：Task-1
- **状态**：[ ] Todo  [x] Done

### Task-3: 认证模块（登录 + JWT）

- **描述**：实现 `POST /api/admin/auth/login`；Spring Security + JWT；
  管理路径需 Bearer Token；公开路径放行。
- **交付物**：`auth` 包；Security/JWT 配置；登录接口
- **验收标准**：正确账号返回 token；错误密码失败；未带/无效 token 访问
  管理接口返回 401（AC-6、AC-10）
- **依赖**：Task-2
- **状态**：[ ] Todo  [x] Done

### Task-4: 分类模块 API

- **描述**：公开 `GET /api/categories`；管理端分类 CRUD；删除时若仍有
  文章引用则返回业务冲突（建议 code 409）与明确提示。
- **交付物**：`category` 包（Controller/Service/DTO）
- **验收标准**：分类增删改查可用；被引用分类不可删且提示明确（AC-8；
  支撑 AC-3）
- **依赖**：Task-3
- **状态**：[ ] Todo  [x] Done

### Task-5: 标签模块 API

- **描述**：公开 `GET /api/tags`；管理端标签 CRUD；删除标签时级联清理
  `article_tags`。
- **交付物**：`tag` 包（Controller/Service/DTO）
- **验收标准**：标签增删改查可用；文章可关联多标签（AC-9；支撑 AC-3）
- **依赖**：Task-3
- **状态**：[ ] Todo  [x] Done

### Task-6: 文章公开接口

- **描述**：实现 `GET /api/articles`（默认每页 10、按 `published_at`
  倒序；支持 `categoryId` / `tagId` / `keyword` 标题 LIKE）与
  `GET /api/articles/{id}`；仅返回已发布内容；详情含分类与标签。
- **交付物**：`article` 公开 Controller/Service/查询逻辑
- **验收标准**：满足 AC-1～AC-5；无结果返回空列表不报错；不存在返回
  明确错误（建议 404）
- **依赖**：Task-4、Task-5
- **状态**：[ ] Todo  [x] Done

### Task-7: 文章管理接口

- **描述**：实现管理端文章列表/详情/创建/更新/删除；字段含标题、正文、
  分类、标签、发布时间；删除级联 `article_tags`。
- **交付物**：`/api/admin/articles` 全套接口
- **验收标准**：文章 CRUD 全部可用且需 JWT（AC-7、AC-10）
- **依赖**：Task-6
- **状态**：[ ] Todo  [x] Done

### Task-8: 前端工程骨架

- **描述**：初始化 Vue 3 + Vite；配置路由分区（访客 `/` 与管理
  `/admin/*`）；Axios 实例、Token 拦截器、统一错误处理；开发期代理
  `/api` → `8080`。
- **交付物**：前端工程；`router`、`api` 客户端、环境/代理配置
- **验收标准**：`npm run dev` 可启动；代理后可打通后端健康/公开接口
  （AC-11）
- **依赖**：Task-1（可与 Task-2～7 部分并行，联调需后端可用）
- **状态**：[ ] Todo  [x] Done

### Task-9: 访客端布局与设计系统

- **描述**：落地 CSS 变量配色、Quicksand/Inter 字体、顶栏（Logo +
  胶囊导航 + 头像）、页脚与装饰元素；内容区宽度按 plan（列表约
  1360px、详情约 1040px）。
- **交付物**：全局样式、访客 Layout、顶栏/页脚组件、静态资源占位
- **验收标准**：访客页具备统一治愈系视觉骨架；窄屏自适应；管理端不强制
  套用卡通样式
- **依赖**：Task-8
- **状态**：[ ] Todo  [x] Done

### Task-10: 访客首页与关于页

- **描述**：实现 `/`（Hero + 最新文章单列卡片）与 `/about`（静态简介，
  文案写在前端配置）。
- **交付物**：Home、About 页面；文章卡片组件初版
- **验收标准**：首页可展示最新文章入口；关于页可访问且无新增后端接口
- **依赖**：Task-9、Task-6
- **状态**：[ ] Todo  [x] Done

### Task-11: 访客文章列表与详情

- **描述**：实现 `/articles`（筛选条：分类/标签/关键词 + 分页 + 单列
  卡片）与 `/articles/:id`（大圆角内容区）；查询参数驱动筛选。
- **交付物**：Articles、ArticleDetail 页面；筛选/分页交互
- **验收标准**：手工走查列表/详情/筛选/搜索；对应 AC-1～AC-5 前端表现
- **依赖**：Task-9、Task-6
- **状态**：[ ] Todo  [x] Done

### Task-12: 管理端登录页

- **描述**：实现 `/admin/login`；登录成功存储 JWT 并进入管理区；未登录
  访问管理路由跳转登录。
- **交付物**：Login 页；路由守卫；Token 持久化
- **验收标准**：正确登录可进后台；未登录无法访问管理页（AC-6、AC-10）
- **依赖**：Task-8、Task-3
- **状态**：[ ] Todo  [x] Done

### Task-13: 管理端文章管理

- **描述**：实现 `/admin/articles`：列表、创建/编辑表单（标题、正文、
  分类、多标签、发布时间）、删除；使用 Element Plus。
- **交付物**：文章管理页面与对应 API 封装
- **验收标准**：管理端文章 CRUD 全流程可用（AC-7）
- **依赖**：Task-12、Task-7
- **状态**：[ ] Todo  [x] Done

### Task-14: 管理端分类与标签管理

- **描述**：实现 `/admin/categories`、`/admin/tags` 的列表与增删改；
  删除被引用分类时展示后端明确错误提示。
- **交付物**：分类管理页、标签管理页
- **验收标准**：分类/标签 CRUD 可用；引用冲突有可读提示（AC-8、AC-9）
- **依赖**：Task-12、Task-4、Task-5
- **状态**：[ ] Todo  [x] Done

### Task-15: 联调与 Spec 验收

- **描述**：按 Success Metrics 与全部 AC 做端到端走查；准备 ≥15 条文章
  验证分页；抽查 ≥5 个接口响应格式；确认未实现 Non-Goals。
- **交付物**：验收记录（可写在本文件末尾或 PR 描述）；必要的修复提交
- **验收标准**：AC-1～AC-13 全部勾选通过；Success Metrics 达标
- **依赖**：Task-10、Task-11、Task-13、Task-14
- **状态**：[ ] Todo  [x] Done

---

## 执行顺序

```text
Task-1
  → Task-2
  → Task-3
  → Task-4 ⫽ Task-5
  → Task-6
  → Task-7

Task-8（可在 Task-1 后启动）
  → Task-9
  → Task-10 ⫽ Task-11   （需 Task-6）
  → Task-12             （需 Task-3）
  → Task-13 ⫽ Task-14   （需 Task-7 / Task-4、5）
  → Task-15
```

> `⫽` 表示可并行。建议：**一次会话 / 一个 PR 只做 1 个 Task**，完成后按该
> Task 验收标准检查，再开下一个。

---

## 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-13 | 基于 Approved plan v1.2 初稿 |
| v1.1 | 2026-07-13 | Task-1 Done：后端骨架与通用层 |
| v1.2 | 2026-07-13 | Task-2 Done：实体、Repository、管理员种子 |
| v1.3 | 2026-07-13 | Task-3 Done：登录与 JWT 鉴权 |
| v1.4 | 2026-07-13 | Task-4 Done：分类模块公开列表与管理端 CRUD |
| v1.5 | 2026-07-13 | Task-5 Done：标签模块公开列表与管理端 CRUD |
| v1.6 | 2026-07-13 | Task-6 Done：文章公开列表与详情接口 |
| v1.7 | 2026-07-13 | Task-7 Done：文章管理端 CRUD 接口 |
| v1.8 | 2026-07-13 | Task-8 Done：前端工程骨架（Vue3/Vite/路由/Axios/代理） |
| v1.9 | 2026-07-13 | Task-9 Done：访客端布局与设计系统 |
| v1.10 | 2026-07-13 | Task-10 Done：首页 Hero/最新文章与关于页 |
| v1.11 | 2026-07-13 | Task-11 Done：访客文章列表筛选分页与详情 |
| v1.12 | 2026-07-13 | Task-12 Done：管理端登录、路由守卫与 Token |
| v1.13 | 2026-07-13 | Task-13 Done：管理端文章 CRUD（Element Plus） |
| v1.14 | 2026-07-13 | Task-14 Done：管理端分类与标签 CRUD |
| v1.15 | 2026-07-13 | Task-15 Done：联调验收；AC-1～13 全部通过 |

---

## 验收记录（Task-15）

> 日期：2026-07-13  
> 方式：`node scripts/acceptance-check.mjs` + 前端路由 HTTP 抽查  
> 环境：后端 `8080`，前端 Vite `5173`（`/api` 代理）

### Success Metrics

| 指标 | 结果 | 说明 |
| --- | --- | --- |
| 核心页面可用 | 通过 | `/` `/articles` `/articles/:id` `/about` 及管理页均返回 200 |
| 管理功能完整 | 通过 | 文章/分类/标签 CRUD API 用例全部通过 |
| 列表分页 | 通过 | 公开文章 total≥15；page=1 size=10；page=2 有余量；按 publishedAt 倒序 |
| 标题搜索 | 通过 | `keyword=组件` 命中均为标题包含「组件」；无结果返回空列表 |
| API 一致性 | 通过 | 抽查 health/articles/categories/tags/admin/categories 均为 code/message/data |
| 未登录保护 | 通过 | 无 Token / 无效 Token 访问管理接口返回 401 |

### Acceptance Criteria

| AC | 结果 | 备注 |
| --- | --- | --- |
| AC-1 | 通过 | 默认每页 10，倒序分页 |
| AC-2 | 通过 | 详情含标题/正文/时间；不存在返回 404 |
| AC-3 | 通过 | categoryId / tagId 筛选 |
| AC-4 | 通过 | 标题 LIKE；空结果不报错 |
| AC-5 | 通过 | 未来发布时间文章不对公开接口可见 |
| AC-6 | 通过 | admin/admin123 登录拿 Token |
| AC-7 | 通过 | 文章 CRUD + 多标签 |
| AC-8 | 通过 | 引用中分类删除返回 409「该分类下仍有文章，无法删除」 |
| AC-9 | 通过 | 标签 CRUD；文章多标签 |
| AC-10 | 通过 | 管理接口未授权拒绝 |
| AC-11 | 通过 | REST + JSON |
| AC-12 | 通过 | 统一 Result 结构 |
| AC-13 | 通过 | 校验错误可读，无堆栈 |

### Non-Goals 抽查

未实现：用户注册、评论点赞、富文本编辑器、草稿状态机、封面/阅读量、ES 全文搜、图片上传、SSR/SEO、Redis。本期仅标题关键词搜索与直接发布。

### 复现命令

```bash
node scripts/acceptance-check.mjs
```
