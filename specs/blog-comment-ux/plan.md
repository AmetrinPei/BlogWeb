# Plan: 评论时间 / 楼层 / 置顶

> 基于：specs/blog-comment-ux/spec.md v1.0（Implemented）  
> 状态：Implemented  
> 最后更新：2026-07-14

---

## 1. 方案概述

- `comments.pinned` boolean 默认 false
- 列表：先按 createdAt 给根评论 `floorNo`，再 `pinned` 优先排序
- `PUT /api/comments/{id}/pin`；文章作者或 ADMIN
- 前端 `formatDateTime` + 楼层/置顶 UI

## 2. 接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| PUT | `/api/comments/{id}/pin` | 登录 | Body `{ "pinned": boolean }` |

`CommentResponse` 增：`floorNo`（Integer|null）、`pinned`（boolean）

## 3. 排序与楼层

1. APPROVED 根按 createdAt ASC → floorNo = 1..n  
2. 展示：pinned 根置前，其余 createdAt ASC  
3. 回复 floorNo=null，pinned=false（不可置顶）

## 4. 验收

`CommentUxTests` + `scripts/acceptance-comment-ux.mjs`
