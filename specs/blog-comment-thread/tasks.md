# Tasks: 楼中楼评论

> 基于：specs/blog-comment-thread/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-comment-thread/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-5：全部 Done

- Task-1：`Comment.parent` / `parentId` 请求响应字段；`idx_comments_parent_id`；Repository 组树查询与级联删辅助
- Task-2：`CommentService` 深度 2 校验、树形列表、级联删；`ArticleRepository` 删文评论先子后根
- Task-3：`CommentThreadTests`；`scripts/acceptance-comment-thread.mjs`；`acceptance-standard.mjs` 树查找回归
- Task-4：`createComment` 支持 `parentId`；`ArticleDetailView` 回复 UI 与子列表
- Task-5：Spec AC 勾选；文档状态 Implemented / Done

验收：`.\mvnw.cmd -Dtest=CommentThreadTests test`；可选 `node scripts/acceptance-comment-thread.mjs`（需后端已启动）。
