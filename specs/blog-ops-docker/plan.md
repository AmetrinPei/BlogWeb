# Plan: Docker / 生产部署与反代

> 基于：specs/blog-ops-docker/spec.md v1.0（Implemented）  
> 状态：Implemented  
> 最后更新：2026-07-15

---

## 1. 方案概述

以 **Docker Compose** 交付可复现生产拓扑（不强制云厂商）：

1. **mysql**：官方 MySQL 8，数据卷持久化  
2. **backend**：多阶段构建 Spring Boot jar，`prod` profile，上传目录挂卷  
3. **nginx**：多阶段构建前端 `dist` + 反代后端；**HTTPS 443**（证书挂载），**80 → 443**

密钥与基址走 env（对齐 hardening）。本地用自签证书冒烟；公网替换为真证。

---

## 2. 架构设计

### 2.1 目录与模块

| 路径 | 职责 |
| --- | --- |
| `backend/Dockerfile` | JDK 构建 jar → JRE 运行 |
| `deploy/nginx/Dockerfile` | Node 构建前端 → nginx:alpine 托管 |
| `deploy/nginx/default.conf` | TLS + 反代 + SPA fallback |
| `deploy/docker-compose.yml` | 三服务编排 |
| `deploy/nginx/ssl/` | 证书挂载点（gitignore 私钥；提供 gen 脚本） |
| `deploy/scripts/gen-dev-certs.*` | 本地自签 `localhost` 证书 |
| `docx/部署方式.md` | 构建、启动、证书、冒烟 |
| `scripts/acceptance-ops-docker.mjs` | 静态检查配置 + 可选对运行中栈冒烟 |

### 2.2 反代规则（锁定）

| 路径 | 行为 |
| --- | --- |
| `/api/` | `proxy_pass http://backend:8080/api/` |
| `/uploads/` | `proxy_pass http://backend:8080/uploads/` |
| `/feed.xml` | `proxy_pass http://backend:8080/feed.xml` |
| 其它 | `try_files $uri $uri/ /index.html` |
| `client_max_body_size` | **10m**（对齐上传 8MB） |

### 2.3 环境变量（对齐 hardening）

Compose / `.env` 使用：`JWT_SECRET`、`DB_PASSWORD`、`ADMIN_PASSWORD`、`BLOG_SITE_BASE_URL`、`BLOG_PUBLIC_REGISTRATION_ENABLED`、可选 `BLOG_CORS_ALLOWED_ORIGIN_PATTERNS`。

额外：

| 变量 | 说明 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | Compose 内指向 `mysql:3306` |
| `BLOG_UPLOAD_DIR` | `/data/uploads` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | 默认 `update` 便于首启；稳定后改为 `validate` |
| `SPRING_PROFILES_ACTIVE` | `prod` |

### 2.4 HTTPS

- 证书文件名锁定：`deploy/nginx/ssl/fullchain.pem`、`privkey.pem`
- 自签：`deploy/scripts/gen-dev-certs.ps1` / `.sh`（CN/SAN=`localhost`）
- 公网：用 Let's Encrypt 或云厂商证书覆盖同名文件后 `compose up -d nginx`

### 2.5 验收

1. 仓库内配置文件存在且 nginx 含四条路径规则  
2. 可选：栈已启动时 `acceptance-ops-docker.mjs` 请求 `https://localhost/api/health`（自签需 `NODE_TLS_REJECT_UNAUTHORIZED=0`）  
3. 文档冒烟对齐第十章 §2.4  

---

## 3. 技术选型

| 决策 | 选型 | 理由 |
| --- | --- | --- |
| 编排 | Compose v2 | 个人站足够；文档可复现 |
| 边缘 | Nginx | 静态 + 反代成熟 |
| 前端托管 | 与 Nginx 同容器 | 少一服务；天然同域 |
| HTTPS | 挂载证书 + 自签脚本 | 不绑死某一 ACME 工具 |
| 首启 ddl | env 覆盖 `update` | 避免空库 `validate` 起不来 |

---

## 4. 风险与备选

| 风险 | 缓解 |
| --- | --- |
| 空库 + validate | Compose 默认 update；文档写切换 validate |
| 自签浏览器警告 | 仅本地冒烟；公网换真证 |
| Windows 路径 / 换行 | 提供 ps1 证书脚本；Compose 用相对路径 |
| 构建慢 | 多阶段缓存；文档允许预构建 |

**备选（不采用）：** 仅宝塔无 Compose；Caddy 自动 HTTPS（可作为文档附录，非主路径）。

---

## 5. Constitution 对齐

- [x] 不引入禁止技术栈  
- [x] 密钥不进仓库  
- [x] 与 hardening env 一致  
- [x] 关键路径可脚本化检查  

---

## 6. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-15 | Implemented；锁定 Compose 三服务、Nginx HTTPS、自签脚本、ddl 首启覆盖 |
