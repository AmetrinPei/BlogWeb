# Tasks: 友链与关于页可配

> 基于：specs/blog-friend-links/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-friend-links/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：`SiteSettings` 扩展友链/关于字段；`SiteService` 校验与 `sortOrder` 归一化；`null` 保持原值
- Task-2：管理端「关于页」+「友情链接」表单（上下移排序）
- Task-3：`useSiteSettings` 映射；`AboutView` 可配文案 + 友链区块
- Task-4：`FriendLinksSiteTests`；`scripts/acceptance-friend-links.mjs`；Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=FriendLinksSiteTests" test`；可选 `node scripts/acceptance-friend-links.mjs`（需后端已启动）；前端 `npm run build`。
