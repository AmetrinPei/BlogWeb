# Tasks: 登录密码修改

> 基于：specs/blog-password-change/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-password-change/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：`ChangePasswordRequest` / `Response` / `PasswordChangeService`；`PUT /api/me/password`（MeController）
- Task-2：`api/profile.js#changePassword`；`ProfileView`「修改密码」区块
- Task-3：`PasswordChangeTests`；`scripts/acceptance-password-change.mjs`
- Task-4：Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=PasswordChangeTests" test`；可选 `node scripts/acceptance-password-change.mjs`（需后端已启动）。
