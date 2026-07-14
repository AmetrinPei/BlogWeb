# Plan: 评论与点赞（标准版）

> 基于：specs/blog-comment-like/spec.md v1.0  
> 状态：Approved

## 1. 方案概述

新增 `comments` 与 `likes` 表；公开读、登录写；点赞用唯一索引 `(user_id, target_type, target_id)` 实现幂等 toggle。

## 2. 数据模型

```text
comments
├── id, article_id, user_id, content VARCHAR(1000), created_at

likes
├── id, user_id, target_type (ARTICLE|COMMENT), target_id, created_at
└── UNIQUE(user_id, target_type, target_id)
```

## 3. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/articles/{id}/comments` | 评论列表 |
| POST | `/api/articles/{id}/comments` | 发表评论（登录） |
| DELETE | `/api/comments/{id}` | 删除（作者或 ADMIN） |
| POST | `/api/likes/toggle` | Body: targetType, targetId；返回 liked + count |

## 4. 前端

详情页评论列表与表单；点赞按钮（文章与评论）。

## 5. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | Approved |
