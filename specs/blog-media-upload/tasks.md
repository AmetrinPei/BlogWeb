# Tasks: 媒体上传

> 基于：specs/blog-media-upload/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-media-upload/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-14

## 任务列表

### Task-1 ~ Task-6：全部 Done

- Task-1：`UploadProperties` + `application.yml`（dir / 8MiB / prefix）+ `data/uploads` gitignore
- Task-2：`media` 包（`MediaService` / `AdminMediaController` / Response）；魔数与白名单；`WebMvcConfig` `/uploads/**`
- Task-3：`SecurityConfig` ADMIN/AUTHOR；`MaxUploadSizeExceededException` → 400；Vite 代理 `/uploads`
- Task-4：`MediaUploadTests`；`scripts/acceptance-media-upload.mjs`
- Task-5：`adminMedia.js`；`ArticlesView` 封面上传 + 正文插入图片
- Task-6：Spec AC 勾选；文档 Implemented / Done

验收：`.\mvnw.cmd "-Dtest=MediaUploadTests" test`；可选 `node scripts/acceptance-media-upload.mjs`（需后端已启动）。
