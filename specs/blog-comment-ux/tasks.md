# Tasks: 评论时间 / 楼层 / 置顶

> 基于：specs/blog-comment-ux/plan.md v1.0（Implemented）  
> 关联 Spec：specs/blog-comment-ux/spec.md v1.0（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：Spec/Plan/Tasks + 需求文档 §九卡片
- Task-2：`Comment.pinned`；列表 floorNo+排序；`PUT /api/comments/{id}/pin` + Security
- Task-3：`formatDateTime`；详情楼层/置顶 UI
- Task-4：`CommentUxTests` + acceptance 脚本；文档 Implemented

验收：`.\mvnw.cmd "-Dtest=CommentUxTests" test`；可选 `node scripts/acceptance-comment-ux.mjs`
