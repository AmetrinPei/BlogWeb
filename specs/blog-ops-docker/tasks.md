# Tasks: Docker / 生产部署与反代

> 基于：specs/blog-ops-docker/plan.md v1.0（Implemented）  
> 关联 Spec：specs/blog-ops-docker/spec.md v1.0（Implemented）  
> 状态：Done  
> 最后更新：2026-07-15

## 任务列表

### Task-1 ~ Task-4：全部 Done

- Task-1：`backend/Dockerfile`；Nginx 多阶段构建前端；文档中的构建说明
- Task-2：`deploy/nginx/default.conf`（API/uploads/feed/SPA/HTTPS）
- Task-3：`deploy/docker-compose.yml` + 上传卷 + MySQL
- Task-4：自签证书脚本；`docx/部署方式.md`；扩展 `.env.example`；`scripts/acceptance-ops-docker.mjs`；清单/需求文档状态

验收：配置静态检查 `node scripts/acceptance-ops-docker.mjs`；可选本机 `docker compose -f deploy/docker-compose.yml up -d --build` 后带 TLS 冒烟。
