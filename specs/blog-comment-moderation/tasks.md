# Tasks: 评论审核与反垃圾

> 基于：specs/blog-comment-moderation/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-comment-moderation/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-7：全部 Done

- Task-1：`CommentStatus` / `status` 字段；公开列表仅 `APPROVED`；响应含 `status`
- Task-2：`sensitive_words` + 命中 → `PENDING`；父评须 `APPROVED`；日志不打正文
- Task-3：`blog.comment.rate-limit-per-minute`（默认 5）；点赞仅 `APPROVED`
- Task-4：`/api/admin/comments`、`/api/admin/sensitive-words`
- Task-5：`CommentModerationTests`；`scripts/acceptance-comment-moderation.mjs`
- Task-6：管理端评论审核 / 敏感词页；访客端待审提示
- Task-7：Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=CommentModerationTests,CommentThreadTests" test`；可选 `node scripts/acceptance-comment-moderation.mjs`（需后端已启动）。
