# Plan: 接口限流 Slice-A（登录失败）

> 基于：specs/blog-rate-limit/spec.md v1.0  
> 状态：Implemented（Slice-A）  
> 最后更新：2026-07-15

---

## 1. 方案概述

在 `AuthService.login` 增加**登录失败**内存限流：按 IP、按用户名双维度；成功清零；超限 `code=429`。覆盖公开与管理端登录。不做 Redis、不做搜索限流。

---

## 2. 架构

| 模块 | 职责 |
| --- | --- |
| `config.LoginRateLimitProperties` | `blog.auth.login-rate-limit.max-failures` / `window-seconds` |
| `auth.LoginRateLimitService` | 滑动窗口计数、assert / recordFailure / clear |
| `common.ClientIpResolver` | 从 HttpServletRequest 解析客户端 IP |
| `auth.AuthService#login` | 接入限流；签名增加 `clientIp` |
| `PublicAuthController` / `AuthController` | 传入 IP |
| `ErrorCode.TOO_MANY_REQUESTS` | `429` |
| 验收 | `LoginRateLimitTests` + `scripts/acceptance-login-rate-limit.mjs` |

### 2.1 配置（锁定）

```yaml
blog:
  auth:
    login-rate-limit:
      max-failures: ${BLOG_LOGIN_MAX_FAILURES:5}
      window-seconds: ${BLOG_LOGIN_WINDOW_SECONDS:900}
```

`max-failures <= 0` → 关闭限流。

### 2.2 算法（锁定）

- Key：`ip:{ip}`、`user:{usernameLower}`
- 每次失败将当前时间戳加入该 key 的队列；剔除窗口外时间戳
- 请求开始时：若任一类 key 在窗口内条数 **≥ max-failures** → 429（不再验密，防计时侧信道加剧；文案统一）
- 失败（用户不存在或密码错）→ `recordFailure`
- 成功 → `clear` 两 key

### 2.3 响应

```json
{ "code": 429, "message": "登录尝试过于频繁，请稍后再试", "data": null }
```

HTTP 仍为 200（与现网 BusinessException 一致）。

---

## 3. 技术选型

| 决策 | 选型 | 理由 |
| --- | --- | --- |
| 存储 | 内存 | Spec 允许；无 Redis |
| 维度 | IP + 用户名 | 防单 IP 扫号与单账号撞库 |
| 窗口 | 滑动 900s / 5 次 | 比每分钟更适合登录爆破 |
| 入口 | Service | 两 Controller 共用 |

---

## 4. 风险

| 风险 | 缓解 |
| --- | --- |
| 多实例计数不共享 | 个人站单实例；文档注明；流量大再 Redis |
| 反代后 IP 不准 | 解析 X-Forwarded-For；Compose Nginx 已设 |
| 测试互相污染 | 测试用独立用户名 + 可配小阈值 |

---

## 5. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-15 | Slice-A Implemented |
