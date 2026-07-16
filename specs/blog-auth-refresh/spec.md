# Feature: 刷新令牌与登出

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-auth-refresh`；constitution.md  
> 依赖：specs/blog-auth-rbac（注册 / 登录 / JWT / 角色）；建议在 specs/blog-user-profile、specs/blog-password-change 之后（对齐「真登出」与「改密全端下线」）  
> 最后更新：2026-07-15

---

## 1. Problem Statement

标准版 `blog-auth-rbac` 仅签发**单一 Access JWT**（现网默认约 2 小时）。服务端不记住票据，因此：

- **登出是假的**：访客端/管理端「退出」只清前端 `localStorage`；若 Token 被复制，在过期前仍可调需登录接口。
- **长会话与安全互斥**：拉长 Access 过期可减少反复登录，但泄露窗口变大；缩短过期则体验变差。
- **改密不能踢其它端**：`blog-password-change` 明确接受「改密后旧 Access 在过期前仍可用」，公共设备与多端会话无法强制结束。

本期交付 **Access 短过期 + Refresh Token 续期 + 服务端可失效会话**：登录返回 access 与 refresh；提供刷新接口；登出（及改密）使 Refresh 失效，无法再换新 Access；过期 Access 不可用。不做 OAuth、邮箱验证；不引入 Redis（Refresh 落库即可）。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| Access 短过期 | Access 有效期明显短于现网「仅靠一张长寿 JWT」的心智（Plan 锁定具体分钟数） | 配置断言 + 过期后带旧 Access 请求返回 401 |
| Refresh 可续期 | 持有效 Refresh 可换得新 Access（及约定的新 Refresh 策略） | 刷新接口成功；新 Access 可访问需登录接口 |
| 登出真失效 | 登出后 Refresh 不可再用；无法继续静默续期 | 登出后再调刷新失败；前端无可用会话 |
| 改密踢会话 | 改密成功后该用户既有 Refresh 全部失效 | 改密后用旧 Refresh 刷新失败 |
| 敏感不泄露 | 日志与响应不含密码明文、Token 全文、Refresh 明文（若存哈希则响应亦不回显哈希） | 日志抽查 + 响应断言 |

---

## 3. User Stories

- 作为登录用户，我希望在 Access 过期后无需重新输密码即可继续使用站点，以便长时间阅读、评论或写稿
- 作为登录用户，我希望点击登出后服务端也不能再凭旧 Refresh 续期，以便在网吧/公共电脑上真正结束会话
- 作为刚改过密码的用户，我希望其它设备上的旧会话无法继续续期，以便泄露或换机后收口
- 作为访客，我未持有效凭证时不能刷新或冒充他人会话，以便账号安全
- 作为系统运营者，我不希望密码与 Token 全文出现在日志中，以便满足安全基线

---

## 4. Acceptance Criteria

- [x] AC-1: **登录**（含注册成功若直接发会话）响应在既有用户字段之外返回 **Access Token** 与 **Refresh Token**（字段名由 Plan 锁定；可保留兼容别名如既有 `token` = Access）；统一 JSON `{ code, message, data }`；`data` **不得**含 `password` / `passwordHash`
- [x] AC-2: **Access 短过期**：Access JWT 有效期由配置控制，默认须**短于**现网 `expire-hours: 2` 的「单票长会话」策略（建议默认分钟级，如 15～30 分钟；Plan 锁定）；过期后带该 Access 访问需认证接口返回 **401**
- [x] AC-3: 提供**刷新接口**：请求体携带有效 Refresh；成功返回新的 Access（及 Plan 约定的 Refresh 轮换结果）；Refresh 无效、过期或已吊销时返回明确业务错误或 **401**（非 5xx）
- [x] AC-4: **登出接口**：客户端提交当前 Refresh（或等价会话标识）；服务端**删除或标记失效**对应 Refresh，使其不可再刷新；成功时统一响应；幂等可接受（重复登出不 5xx）
- [x] AC-5: 登出后：用已失效 Refresh 调用刷新**必须失败**；前端须清除本地 Access / Refresh / 用户信息（对齐现有 `clearAuth` 并扩展存储字段）
- [x] AC-6: **Refresh 服务端可管**：Refresh 须持久化（如 MySQL 表），支持按 token（或哈希）查找与吊销；**禁止**仅把长寿命 JWT 当 Refresh 且服务端无记录（否则无法真登出）
- [x] AC-7: **改密对齐**：`PUT /api/me/password`（或等价）成功后，须**吊销该用户全部未失效 Refresh**（全端无法再静默续期）；本期**不要求**立刻拉黑未过期的 Access（靠短过期收口）；前端可提示「其它设备需重新登录」
- [x] AC-8: **角色与 claims 兼容**：新签发的 Access 仍须携带现网可解析的用户与角色信息（如 `userId` / `username` / `role`），不破坏 ADMIN/AUTHOR 路由守卫与 Service 层鉴权
- [x] AC-9: 访客端与管理端现有「退出登录 / 切换账号」须改为**先调服务端登出（若有 Refresh）再清本地**；无 Refresh 的旧会话至少清本地（兼容迁移）
- [x] AC-10: 密码与 **Access / Refresh 全文**禁止写入应用日志；错误响应不回显用户提交的令牌原文
- [x] AC-11: 关键路径具备可自动化验收手段（测试或脚本）：至少覆盖登录双令牌、刷新成功、登出后刷新失败、Access 过期 401、改密后旧 Refresh 失效、响应无敏感字段

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| OAuth、第三方登录、邮箱验证、找回密码 | 卡片与既有 Non-Goals；本期聚焦会话续期与吊销 |
| 引入 Redis / 分布式会话中心 | constitution 禁止无充分依据引入 Redis；Refresh 用 DB 即可 |
| Access 全局黑名单必做 | 短 Access + 吊销 Refresh 即可满足卡片；全量 Access 黑名单增加复杂度，非本期必做 |
| 滑动会话无限续期、Remember-me 独立产品 | Refresh 自身有过期上限（Plan 锁定，如 7～30 天）即可 |
| 登录失败锁定、刷新/登录专用限流中台 | 见 `blog-rate-limit`；可后续叠加 |
| 修改 RBAC 规则、资料字段、改密校验规则本身 | 已由 auth-rbac / user-profile / password-change 交付；本期只接会话吊销钩子 |
| 设备管理 UI、会话列表、远程踢单一设备（超出「当前登出 / 改密全吊销」） | 超出个人博客本期；可选后续 |

---

## 6. Constraints

- 遵守 `constitution.md`：不引入 Elasticsearch、Redis、消息队列、对象存储 SDK、SSR；业务规则在 Service 层强制执行
- 密码仅存 BCrypt；Refresh **建议存哈希**（库中不存明文 Refresh）；日志禁止输出密码、Token 全文、连接串密钥
- 统一响应格式 `{ code, message, data }`；兼容既有注册/登录路径与角色声明
- 不破坏 ADMIN 种子账号与管理端 / 访客端既有登录跳转（`blog-user-profile` redirect 规则仍有效）
- Access / Refresh 过期时间、是否 Refresh 轮换（rotation）、存储表结构由 **Plan** 锁定；Spec 要求行为可测，不绑死实现类名
- 关键路径验收与 PR 说明引用 `blog-auth-refresh` 与 Task 编号

---

## 7. 附录

### 7.1 术语表

| 术语 | 说明 |
| --- | --- |
| Access Token | 短寿命 JWT，用于调用需认证 API；过期后须刷新或重新登录 |
| Refresh Token | 较长寿命、服务端可吊销的凭证，仅用于换取新 Access（及可选新 Refresh） |
| 真登出 | 服务端使 Refresh 失效，并清除客户端会话；不仅删除浏览器本地存储 |
| 全端下线 | 吊销某用户全部未失效 Refresh，使其所有设备无法再静默续期 |
| Token 轮换 | 每次刷新签发新 Refresh 并作废旧 Refresh，降低盗用窗口（是否启用由 Plan 定） |

### 7.2 与既有 Spec 关系

- 承接 `blog-auth-rbac` Non-Goals 中「刷新令牌 / Token 黑名单」
- 对齐 `blog-user-profile`：访客端登出从「仅 `clearAuth`」升级为服务端失效 + 清本地
- 对齐 `blog-password-change`：改密成功后强制吊销 Refresh（全端无法续期）；不重做改密校验
- 与 `blog-rate-limit`：刷新/登录防刷可后续叠加，非本期必做

### 7.3 现状基线（开 Spec 时）

| 能力 | 现状 |
| --- | --- |
| 登录响应 | `LoginResponse`：单一 `token`（Access）、`expireHours`、用户与公开资料字段；无 Refresh |
| Access 过期 | `blog.jwt.expire-hours` 默认 **2** |
| 登出 | 前端 `clearAuth()` 仅删 `localStorage`；无服务端登出/吊销 API |
| 改密后会话 | 旧 Access 在过期前仍可用；无 Refresh 可吊销 |
| 存储 | 无 Refresh 表；JWT 无状态校验 |

### 7.4 Plan 已锁定（摘要）

| 项 | 锁定值 |
| --- | --- |
| Access TTL | **30 分钟**（`blog.jwt.access-expire-minutes`） |
| Refresh TTL | **14 天**（`blog.jwt.refresh-expire-days`） |
| 登录/注册 data | 保留 `token`（= Access）+ 新增 `refreshToken` + `accessExpireMinutes`；兼容保留 `expireHours` |
| 刷新 | `POST /api/auth/refresh`，body：`refreshToken`；**强制轮换** |
| 登出 | `POST /api/auth/logout`，body：`refreshToken`；`permitAll`；幂等 |
| Refresh 存储 | MySQL `refresh_tokens`；SHA-256 hex + `userId` + `expiresAt` + `revokedAt` |
| 改密 | 成功后 `revokeAllByUserId`；本机可不强制清 Access |
| Access 黑名单 | **不做** |
| 前端 | 存 Refresh；401 单飞静默刷新；登出先 API 再 `clearAuth` |

详见 `plan.md`。

### 7.5 变更记录

| 版本 | 日期 | 变更说明 | 作者 |
| --- | --- | --- | --- |
| v1.0 | 2026-07-15 | 自需求文档 §九卡片起草 Draft；锁定双令牌、刷新、登出吊销 Refresh、改密全端下线；OAuth/Redis/Access 黑名单不做 | |
| v1.1 | 2026-07-15 | Spec ↔ Plan 对齐；附录锁定 30min/14d、字段名、轮换、permitAll logout、改密 revokeAll | |
| v1.2 | 2026-07-15 | Implemented；AC 勾选；`AuthRefreshTests` + `acceptance-auth-refresh.mjs`；前端静默刷新与真登出 | |
