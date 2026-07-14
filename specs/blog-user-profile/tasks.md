# Tasks: 用户资料与访客端账号会话

> 基于：specs/blog-user-profile/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-user-profile/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-6：全部 Done

- Task-1：`User` 三字段；`ProfileService` / `MeController`（`GET/PUT /api/me`）；Security；`LoginResponse` 附带资料
- Task-2：`ArticleResponse.authorName` 回退展示名 + `authorAvatarUrl`；`CommentResponse.displayName` / `avatarUrl`
- Task-3：`UserProfileTests`；`scripts/acceptance-user-profile.mjs`
- Task-4：`/login` vs `/admin/login` redirect；路由守卫；注册回首页；评论登录带 redirect
- Task-5：`SiteHeader` 账号菜单；`/profile`；`api/profile.js`；会话展示字段
- Task-6：Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=UserProfileTests" test`；可选 `node scripts/acceptance-user-profile.mjs`（需后端已启动）。
