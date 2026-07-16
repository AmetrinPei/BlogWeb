# Feature: Docker / 生产部署与反代

> 状态：Implemented  
> 关联：docx/个人博客-应用场景需求与进阶开发.md §九 `blog-ops-docker`、§十 §2.1 / §2.4 / §8.3-B；constitution.md  
> 依赖：specs/blog-prod-hardening（env 名、prod profile、上线清单）；不依赖限流 / SEO  
> 最后更新：2026-07-15

---

## 1. Problem Statement

标准版与 `blog-prod-hardening` 已就绪，但仓库**无生产运行拓扑**：开发依赖 Vite 代理 `/api`、`/uploads`、`/feed.xml`，直接上线会断；上传目录无持久卷约定；无 Nginx（或同类）反代与 HTTPS 终结说明。

本期交付**可复现的生产运行形态**（Docker Compose 为主路径）：

1. Compose（或等价文档）拉起 MySQL + 后端 + Nginx（静态前端 + 反代）
2. Nginx 反代 `/api`、`/uploads`、`/feed.xml`，SPA `history` 用 `try_files`
3. 上传目录卷持久化；环境变量与 hardening 约定一致
4. 反代层 HTTPS（本地可用自签证书冒烟；公网文档说明证书替换）
5. 部署文档可复现；联合冒烟 §2.4 可勾选

不做 K8s、GitOps、CI/CD、应用内 CORS/注册开关（已由 hardening 交付）。

---

## 2. Success Metrics

| 指标 | 目标值 | 度量方式 |
| --- | --- | --- |
| 可复现拉起 | 按文档一条路径可 `compose up`（或等价） | 文档步骤 + 本地复现 |
| 反代正确 | `/api`、`/uploads`、`/feed.xml`、SPA 刷新不 404 | curl / 浏览器 |
| 持久化 | 上传卷重启后文件仍在 | 上传 → 重启 → URL 仍可开 |
| HTTPS | 反代终结 TLS；本地自签可冒烟 | https 访问首页 |
| env 对齐 | 与 hardening `.env.example` 变量名一致 | 抽查 |

---

## 3. User Stories

- 作为部署人员，我希望用 Compose 一键拉起 MySQL/后端/Nginx，以便减少手工多进程启动
- 作为访客，我希望在公网域名下打开站点与文章、图片、RSS，以便功能与开发环境一致
- 作为运营者，我希望上传文件落在持久卷，以便容器重建不丢封面/头像
- 作为安全意识用户，我希望登录与接口经 HTTPS，以便不在明文信道传密

---

## 4. Acceptance Criteria

- [x] AC-1: 提供 **Docker Compose**（或文档化的等价拓扑）含 **MySQL + 后端 + 前端/Nginx**
- [x] AC-2: Nginx（或同类）反代 **`/api`、`/uploads`、`/feed.xml`**，并对 SPA 配置 **`try_files` history fallback**
- [x] AC-3: **上传目录**挂持久卷（或文档等价挂载）；公网路径仍为 `/uploads/...`
- [x] AC-4: 环境变量注入与 `blog-prod-hardening` 约定一致（至少 `JWT_SECRET`、`DB_PASSWORD`、`ADMIN_PASSWORD`、`BLOG_SITE_BASE_URL`）；真实密钥不进仓库
- [x] AC-5: 提供可复现 **部署文档**（构建 jar/`npm run build`、Compose 启动、证书、冒烟）
- [x] AC-6: 联合冒烟 §2.4 中 ops 相关项可在文档/清单中勾选（HTTPS 首页、上传、feed）
- [x] AC-7: 不引入 ES / Redis / MQ / OSS SDK / SSR；不做 K8s / CI

---

## 5. Non-Goals

| 不做 | 原因 |
| --- | --- |
| K8s、多环境 GitOps | 超出个人站本期 |
| 应用内 CORS/注册/ddl 硬化 | 已由 `blog-prod-hardening` |
| 完整 CI/CD、备份 | `blog-ops-backup` |
| 商业 CA 自动签发中台 | 文档说明 Let's Encrypt / 手工挂证即可 |
| OSS / CDN | 可后置 |

---

## 6. Constraints

- 遵守 constitution；密钥不进仓库
- 同域反代时 CORS 可留空（hardening 约定）
- Spec 写 WHAT；镜像分层、端口、证书路径由 Plan 锁定
- PR 引用 `blog-ops-docker` 与 Task 编号

---

## 7. 附录

### 7.1 Plan 已锁定（摘要）

| 项 | 锁定值 |
| --- | --- |
| 拓扑 | `deploy/docker-compose.yml`：`mysql` + `backend` + `nginx` |
| 前端 | Nginx 镜像多阶段构建 `frontend` dist，同域托管 |
| 反代 | `deploy/nginx/default.conf`：`/api` `/uploads` `/feed.xml` + `try_files` |
| HTTPS | 443 + 挂载 `deploy/nginx/ssl/`；提供自签脚本；80→443 |
| 上传卷 | named volume → `/data/uploads`；`BLOG_UPLOAD_DIR` |
| 首次库表 | Compose 默认可 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 覆盖，稳定后改 `validate` |
| 文档 | `docx/部署方式.md` |

详见 `plan.md`。

### 7.2 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-15 | Draft→Implemented；Compose + Nginx HTTPS 反代 + 部署文档 |
