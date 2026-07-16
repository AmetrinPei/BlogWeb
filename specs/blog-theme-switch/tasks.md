# Tasks: 主题切换与访客端视觉氛围

> 基于：specs/blog-theme-switch/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-theme-switch/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15  
> 预览对照：`specs/blog-theme-switch/效果预览.html`

## 任务列表

### Task-1 ~ Task-5：全部 Done

- Task-1：`SiteSettings` 扩展主题/背景/头像/贴图字段；`SiteService` 枚举与 URL 校验；未传保持原值
- Task-2：`useTheme.js` + `style.css` light/dark token；`SiteHeader` 切换；访客端 `#fff` → `--bg-elevated`
- Task-3：`DecorBackground` 分层氛围；`AboutAvatar`；`HeroArt`；`PublicLayout` shell-bg 四模式
- Task-4：管理端「主题与视觉」表单 + 上传；`useSiteSettings` 映射新字段
- Task-5：`ThemeSwitchSiteTests`；`scripts/acceptance-theme-switch.mjs`；Spec AC 勾选

验收：`.\mvnw.cmd "-Dtest=ThemeSwitchSiteTests" test`；可选 `node scripts/acceptance-theme-switch.mjs`（需后端已启动）；浏览器打开 `效果预览.html` 做视觉对照。
