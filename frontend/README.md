# 个人博客 · 前端

Vue 3 + Vite 单应用，路由分区访客端与管理端。

## 开发

```bash
npm install
npm run dev
```

默认 `http://localhost:5173`。开发期将 `/api` 代理到后端 `http://localhost:8080`。

## 目录要点

| 路径 | 说明 |
| --- | --- |
| `src/router` | 访客 `/` 与管理 `/admin/*` 路由 |
| `src/api` | Axios 客户端与接口封装 |
| `src/utils/auth.js` | JWT Token 存取 |
| `.env.development` | `VITE_API_BASE_URL=/api` |
