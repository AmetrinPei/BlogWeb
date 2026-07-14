# Feature: 站点体验（标准版）

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §2 标准版·体验  
> 依赖：blog-content-enhance（recommended/pinned）  
> 最后更新：2026-07-14

---

## 1. Problem Statement

MVP 首页仅为最新文章列表，关于页与站点名写死在前端配置，缺少归档与可后台维护的站点信息，体验未达「接近真实博客」。

本期：首页精选（推荐/置顶）；按年月归档；站点配置（站名、简介、社交链接）可后台维护并供前台读取。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 精选区 | 首页展示 recommended 或 pinned 的已发布文章 | 标记推荐后首页可见 |
| 归档 | 按年-月分组返回文章数量或列表入口 | API + 归档页 |
| 站点配置 | 修改站名后前台顶栏/页脚反映新值 | 管理保存后刷新前台 |

---

## 3. User Stories

- 作为访客，我希望在首页看到精选文章，以便发现重点内容
- 作为访客，我希望按年月归档浏览，以便回溯历史
- 作为管理员，我希望配置站点名、简介与社交链接，以便品牌展示

---

## 4. Acceptance Criteria

- [x] AC-1: 公开接口返回精选文章列表（recommended=true 的已发布；可含 pinned 策略见 plan）
- [x] AC-2: 公开归档接口按 `yyyy-MM` 聚合已发布文章数量
- [x] AC-3: 归档页可进入并筛选某月文章（或链到列表带时间参数）
- [x] AC-4: 站点配置可读；ADMIN 可更新 siteName、tagline、socialLinks（JSON 或多字段）
- [x] AC-5: 前台顶栏 Logo 文案、页脚社交链接读取站点配置（有降级默认值）

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| 真·轮播复杂动效库 | 精选列表/简单横滑即可 |
| 多主题切换 | 进阶 |
| Newsletter / RSS | 进阶 |

---

## 6. Constraints

- 遵守 constitution.md
- 站点配置单例（一行或 key-value）即可

---

## 7. 附录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | Approved |
| v1.1 | 2026-07-14 | Implemented；acceptance-standard 验收通过 |
