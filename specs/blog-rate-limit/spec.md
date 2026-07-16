# Feature: 接口限流（Slice-A：登录失败）

> 状态：Implemented（Slice-A）；Slice-B 未做  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-rate-limit`、§十 §8.3-C；constitution.md  
> 依赖：specs/blog-auth-rbac / 现网登录；评论限流已存在可对照，本期不改评论  
> 最后更新：2026-07-15

---

## 1. Problem Statement

公开上线后登录接口可被暴力试密；现网仅评论有按用户频控，**登录无失败计数与封锁**。本期交付 **Slice-A：登录失败限流**（内存、可配置），覆盖访客登录与管理端登录共用入口。

**Slice-B（公开搜索限流）本期不做**，上线后 P1。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 防爆破 | 同一 IP 或同一用户名在窗口内失败达上限后拒绝继续尝试 | API 断言业务码 429 |
| 正常登录 | 未超限时正确密码可登录；成功后计数清零 | 登录成功用例 |
| 可配置 | 阈值与窗口可调；≤0 可关闭 | 配置 + 测试 |
| 统一响应 | 超限为 `{ code, message, data }`，不泄露密码 | 响应断言 |

---

## 3. User Stories

- 作为站点运营者，我希望登录失败过多时暂时拒绝尝试，以便降低撞库风险
- 作为正常用户，我希望偶尔输错密码后仍能登录成功，以便不被误伤
- 作为系统，我不希望在日志中打印密码，以便满足安全基线

---

## 4. Acceptance Criteria（Slice-A）

- [x] AC-1: 对 **登录失败** 按 **客户端 IP** 与 **用户名**（trim，大小写不敏感键）分别计数；任一维度达到上限则拒绝后续登录尝试
- [x] AC-2: 覆盖 `POST /api/auth/login` 与 `POST /api/admin/auth/login`（共用 Service）
- [x] AC-3: 超限返回统一 Result，**业务 `code=429`**，明确文案（如「登录尝试过于频繁，请稍后再试」）；不创建会话；**不得** 5xx
- [x] AC-4: 登录**成功**后清除该 IP 与该用户名的失败计数
- [x] AC-5: 配置项可调（最大失败次数、窗口时长）；次数 ≤0 时关闭限流
- [x] AC-6: 内存实现即可；不引入 Redis；密码不进日志
- [x] AC-7: 自动化测试覆盖：连续失败超限 → 429；成功登录清零后可再登
- [x] AC-8: 本期**不做** Slice-B 搜索限流、验证码、WAF

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| 公开搜索限流（Slice-B） | P1；另开或后续同 Spec 增补 |
| Redis 分布式限流 | 须改 constitution；单机内存足够个人站 |
| 验证码 / WAF / 全站限流中台 | 超出本期 |
| 改评论限流实现 | 已有；仅文档对照 |

---

## 6. Constraints

- 统一 `{ code, message, data }`；限流在 Service 层强制
- IP 取 `X-Forwarded-For` 首段（若有）否则 `X-Real-IP` 否则 `remoteAddr`（反代后可用）
- 错误用户名与错误密码均计为失败（与现网「用户名或密码错误」一致）

---

## 7. 附录

### 7.1 Plan 已锁定

| 项 | 锁定值 |
| --- | --- |
| 配置前缀 | `blog.auth.login-rate-limit` |
| 默认 | `max-failures=5`，`window-seconds=900`（15 分钟） |
| 超限码 | `429` |
| 存储 | 进程内 ConcurrentHashMap + 滑动时间窗 |
| IP 工具 | `ClientIpResolver` |

详见 `plan.md`。

### 7.2 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-15 | Slice-A Implemented；登录失败按 IP/用户名限流 |
