# Tasks: 搜索增强

> 基于：specs/blog-search-enhance/plan.md v1.1（Approved）  
> 关联 Spec：specs/blog-search-enhance/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：`ArticleSpecs.keywordMatches`（title OR summary OR content）；`listPublished` 挂接；管理端未加 keyword  
- Task-2：`ArticlePublicTests#keywordMatchesTitleSummaryOrContentAndHidesDraft`；`scripts/acceptance-search-enhance.mjs`  
- Task-3：`ArticlesView.vue` 文案与 placeholder  
- Task-4：Spec AC-1～8 已勾选；文档状态 Updated  

验收：`.\mvnw.cmd -Dtest=ArticlePublicTests test`；可选 `node scripts/acceptance-search-enhance.mjs`（需后端已启动）。
