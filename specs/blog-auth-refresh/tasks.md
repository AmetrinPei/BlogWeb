# Tasks: 刷新令牌与登出

> 基于：specs/blog-auth-refresh/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-auth-refresh/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15

## 任务列表

### Task-1 ~ Task-5：全部 Done

- Task-1：`RefreshToken` 落库 + `RefreshTokenService`；`accessExpireMinutes=30` / `refreshExpireDays=14`
- Task-2：登录双令牌；`POST /api/auth/refresh|logout`；改密 `revokeAllByUserId`
- Task-3：前端存 Refresh；401 单飞静默刷新；登出先 API 再 `clearAuth`
- Task-4：`AuthRefreshTests`；`scripts/acceptance-auth-refresh.mjs`
- Task-5：Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=AuthRefreshTests" test`；可选 `node scripts/acceptance-auth-refresh.mjs`（需后端已启动）。
