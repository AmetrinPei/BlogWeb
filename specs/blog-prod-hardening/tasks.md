# Tasks: 生产配置硬化

> 基于：specs/blog-prod-hardening/plan.md v1.1（Implemented）  
> 关联 Spec：specs/blog-prod-hardening/spec.md v1.2（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：`application-prod.yml` + `ProdSecurityValidator` + `.env.example`
- Task-2：`CorsProperties` / `CorsConfig`（env CSV 可覆盖）
- Task-3：注册开关 + `publicRegistrationEnabled` 下发；Header / Login / Register 前端
- Task-4：`docx/上线检查清单.md`；`启动方式.md` 生产短节；`ProdHardeningTests`；`scripts/acceptance-prod-hardening.mjs`；Spec AC 勾选

验收：`.\mvnw.cmd "-Dtest=ProdHardeningTests,ProdHardeningRegistrationClosedTests" test`；可选 `node scripts/acceptance-prod-hardening.mjs`（需后端已用新代码启动）；前端 `npm run build`。
