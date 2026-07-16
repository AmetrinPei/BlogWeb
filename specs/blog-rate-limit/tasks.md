# Tasks: 接口限流 Slice-A

> 基于：specs/blog-rate-limit/plan.md v1.0  
> 关联 Spec：specs/blog-rate-limit/spec.md v1.0  
> 状态：Done（Slice-A）；Slice-B Todo  
> 最后更新：2026-07-15

## Slice-A

- [x] Task-1：`LoginRateLimitProperties` + `LoginRateLimitService` + `ClientIpResolver`；`AuthService.login` 接入；`ErrorCode.429`
- [x] Task-2：`LoginRateLimitTests`；`scripts/acceptance-login-rate-limit.mjs`；配置写入 `application.yml`；文档状态

## Slice-B（本期不做）

- [ ] Task-3：公开搜索限流
- [ ] Task-4：文档对照评论限流阈值

验收：`.\mvnw.cmd "-Dtest=LoginRateLimitTests" test`；可选 `node scripts/acceptance-login-rate-limit.mjs`。
