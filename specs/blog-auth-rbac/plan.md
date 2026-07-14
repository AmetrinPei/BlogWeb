# Plan: 用户注册与 RBAC（标准版）

> 基于：specs/blog-auth-rbac/spec.md v1.0  
> 状态：Approved  
> 最后更新：2026-07-14

---

## 1. 方案概述

扩展 `users.role`（ADMIN/AUTHOR）；新增公开注册与统一登录；JWT claims 含 userId、username、role；`articles.author_id` 外键；管理端文章写操作按角色校验归属。前端增加注册页；路由守卫：`/admin/**` 需 ADMIN；作者使用同一文章管理页但后端强制归属（或允许 AUTHOR 访问文章管理子路由）。

**拍板**：AUTHOR 可访问 `/admin/articles` 进行创作，不可访问分类/标签/站点配置等仅 ADMIN 路由；后端对分类标签等仍要求 ADMIN。AUTHOR 的文章管理列表/详情**仅返回自己的文章**；ADMIN 可见全部。

---

## 2. 数据模型

```text
users 增加：
├── role VARCHAR(20) NOT NULL  -- ADMIN | AUTHOR

articles 增加：
├── author_id BIGINT NULL FK → users.id  -- 历史数据可挂到种子 admin
```

---

## 3. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册，Body: username, password |
| POST | `/api/auth/login` | 统一登录，返回 token、role、username |
| POST | `/api/admin/auth/login` | 保留，内部复用同一登录逻辑（兼容 MVP） |

Security：`/api/auth/**` permitAll；`/api/admin/categories/**`、`/api/admin/tags/**`、`/api/admin/site/**` 需 ADMIN；`/api/admin/articles/**` 需认证（ADMIN 或 AUTHOR）。

---

## 4. 技术选型

| 决策 | 选型 |
| --- | --- |
| 角色 | Enum UserRole |
| 鉴权 | JWT + Security filter；方法内校验归属 |
| 403 | BusinessException FORBIDDEN |

---

## 5. 与 Constitution 对齐

- [x] BCrypt、JWT、Service 层授权
- [x] 统一响应含 403

---

## 6. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | 初稿 Approved |
