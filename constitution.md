# Project Constitution

> 个人博客项目不可违背的全局约束。所有 `specs/*/spec.md` 与实现必须遵守。  
> 最后更新：2026-07-14

---

## 1. 技术栈约束

- **后端**：Java 17+、Spring Boot 3.x、Spring Web、Spring Data JPA、Spring Security、MySQL 8.x
- **认证**：JWT（无状态）+ BCrypt 密码哈希
- **前端**：Vue 3 + Vite + Axios；管理端 Element Plus；访客端自写 CSS（治愈系设计系统）
- **禁止**：无充分 Spec 依据时引入 Elasticsearch、Redis、消息队列、对象存储 SDK、SSR 框架（Nuxt/Next）
- **禁止**：在业务代码中手写 SQL 字符串拼接；必须使用 JPA / 参数化查询

## 2. 架构原则

- 前后端分离；通信仅通过 REST API + JSON
- 统一响应结构：`{ "code", "message", "data" }`；错误码约定 0 / 400 / 401 / 403 / 404 / 409 / 500
- 后端按 domain 分包：`auth` / `article` / `category` / `tag` / `user` / `comment` / `like` / `site` / `common` / `config`
- 依赖方向：Controller → Service → Repository；禁止跨层逆向依赖
- 访客公开接口与管理/需登录接口路径分区清晰（如 `/api/...` 公开，`/api/admin/...` 管理）

## 3. 代码规范

- 命名：Java 类 PascalCase；包名小写；前端组件 PascalCase；API 模块 camelCase 文件名
- 实体变更须同步 Spec/Plan 中的数据模型说明
- 业务规则（权限、状态可见性、引用校验）必须在 Service 层强制执行，不可仅依赖前端
- 优先复用现有 `Result` / `BusinessException` / `PageResult`，避免平行抽象
- 关键路径（鉴权、状态机、点赞幂等）应有可自动化验收手段（脚本或测试）

## 4. 安全约束

- 必须使用参数化查询 / ORM，禁止 SQL 拼接
- 密码仅存 BCrypt 哈希；日志禁止输出密码、Token 全文、连接串密钥
- 写操作与敏感读操作必须经身份认证；细粒度授权（角色、资源归属）在 Service 校验
- Markdown 渲染为 HTML 时必须做 XSS 消毒（禁用危险标签/属性）
- 上传类能力（若后续引入）必须校验类型与大小；标准版封面仅允许 URL 字段，不落地文件上传

## 5. 质量门禁

- 重大变更合并前：对应 Spec AC 可勾选通过；公开接口抽查统一响应格式
- PR / 提交说明须引用 Spec 模块名与 Task 编号（如 `blog-content-enhance Task-3`）
- 不得将堆栈或内部实现细节返回给客户端
- Code Review 关注：Spec 符合度、权限遗漏、Non-Goals 越界

## 6. 文档与 Spec 规范

- 规格目录：`specs/{module-name}/spec.md`、`plan.md`、`tasks.md`
- MVP 基线冻结于 `specs/personal-blog/`；标准版及后续增量使用独立模块目录
- **重大变更**（新表/新接口/状态机/角色/跨模块行为）：必须 Spec → Plan → Tasks → 实现 → 验证
- **微小变更**（文案、纯样式、不影响行为）：可直接改代码
- Spec 只写 WHAT；HOW 写在 `plan.md`
- 发现需求缺口或 Bug：先更新 Spec（或补 AC），再改代码

---

*本文档为项目宪法。变更须谨慎，并通知所有进行中的 Spec。*
