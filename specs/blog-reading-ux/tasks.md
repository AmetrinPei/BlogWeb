# Tasks: 阅读体验打磨

> 基于：specs/blog-reading-ux/plan.md v1.1（Approved）  
> 关联 Spec：specs/blog-reading-ux/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-6：全部 Done

- Task-1：`markdownSlug.js` + `markdown.js`（marked → DOMPurify → h2/h3 id + toc）；`scripts/acceptance-reading-ux-md.mjs`
- Task-2：`ArticleToc.vue`；`ArticleDetailView` 文首 TOC + `scroll-margin-top`
- Task-3：`highlight.js` core 动态 import；`useCodeHighlight.js`；无 `pre code` 不加载
- Task-4：`ArticleListSkeleton` / `ArticleDetailSkeleton`；替换列表/详情「加载中…」
- Task-5：`ArticlesView` 搜索空态 vs 通用空态；清空关键词 / 清空筛选
- Task-6：验收脚本 4/4；`npm run build` 通过；Spec AC 勾选

验收：`node scripts/acceptance-reading-ux-md.mjs`；前端 `npm run build`；Plan §2.7 手工走查。
