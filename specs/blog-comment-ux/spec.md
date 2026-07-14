# Feature: 评论时间 / 楼层 / 置顶

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-comment-ux`；constitution.md  
> 依赖：specs/blog-comment-thread（楼中楼）；specs/blog-comment-moderation（APPROVED 可见）；specs/blog-auth-rbac  
> 最后更新：2026-07-14

---

## 1. Problem Statement

评论区时间仅到日，读者难辨先后；一级评论无楼层标识，讨论不便引用；优质根评论无法置顶，重要讨论易被淹没。

本期交付：评论时间精细到分钟；一级（根）评论稳定楼层号；每文最多一条根评论置顶（文章作者或 ADMIN），再置顶则替换。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 时间到分钟 | 访客端评论时间显示含时分 | UI / 手工 |
| 楼层稳定 | 根评论有 `#n`；按 createdAt 正序编号；置顶不改楼层号 | API 断言 floorNo |
| 单文单置顶 | 再置顶取消旧置顶；置顶根排列表最前 | 置顶替换用例 |
| 权限正确 | 仅文章作者或 ADMIN 可置顶；回复不可置顶 | 403 / 400 |

---

## 3. User Stories

- 作为读者，我希望看到评论精确到分钟与楼层号，以便引用讨论
- 作为文章作者/管理员，我希望置顶一条优质根评论，以便突出重点
- 作为访客，我希望置顶后原楼层号不变，以免引用错乱

---

## 4. Acceptance Criteria

- [x] AC-1: 访客端评论（根与回复）时间展示为 `YYYY-MM-DD HH:mm`（本地）；文章发布日等其它处可仍用仅日期
- [x] AC-2: 公开评论列表中每个 **APPROVED 根评论** 带 `floorNo`（从 1 起，按该文 APPROVED 根评论 `createdAt` 正序）；**回复** `floorNo` 为 null
- [x] AC-3: 响应含 `pinned`（boolean）；列表展示顺序：置顶根（若有）→ 其余根按 `createdAt` 正序；子回复仍挂父下正序
- [x] AC-4: `PUT /api/comments/{id}/pin` Body `{ "pinned": true|false }`；需登录；仅 **文章作者** 或 **ADMIN**；目标须为 **APPROVED 根评论**
- [x] AC-5: 每文最多 1 条置顶根评论；`pinned=true` 时先取消同文其它根置顶再设置；回复或非通过评论置顶 → 400；无权 → 403；未登录 → 401
- [x] AC-6: 置顶不改变 `floorNo` 赋值规则
- [x] AC-7: 访客端根评论展示楼层与置顶角标；文章作者/ADMIN 可见置顶/取消置顶操作
- [x] AC-8: 自动化验收覆盖楼层、置顶替换、权限、回复不可置顶、列表顺序

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| 子回复楼层（如 #1-1） | 本期仅根评论 |
| 多条置顶 | 锁定每文 1 条 |
| 评论作者本人置顶（非文章作者） | 权限限定文章作者/ADMIN |
| 改审核/楼中楼深度规则 | 既有 Spec 不变 |

---

## 6. Constraints

- 遵守 constitution；权限在 Service 层
- 统一 `{ code, message, data }`
- 兼容既有评论数据（`pinned` 默认 false）

---

## 7. 附录

### 7.1 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | Draft→Implemented；楼层/单置顶/时间到分钟 |
