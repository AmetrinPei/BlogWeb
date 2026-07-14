# Plan: 站点体验（标准版）

> 基于：spec.md v1.0  
> 状态：Approved

## 1. 方案概述

新增 `site_settings` 单例表；公开 `GET /api/site`；管理 `PUT /api/admin/site`；`GET /api/articles/featured` 返回 recommended 已发布文章；`GET /api/articles/archive` 返回年月计数；首页精选区 + `/archive` 页；前端 layout 拉取站点配置。

## 2. 数据模型

```text
site_settings
├── id (固定 1)
├── site_name, tagline, about_text
├── social_links TEXT (JSON 数组 [{name,url}])
├── updated_at
```

## 3. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/site` | 公开站点配置 |
| PUT | `/api/admin/site` | ADMIN 更新 |
| GET | `/api/articles/featured` | 精选 |
| GET | `/api/articles/archive` | [{yearMonth, count}] |
| GET | `/api/articles?yearMonth=yyyy-MM` | 可选筛选 |

## 4. 变更记录

| 版本 | 日期 | 变更说明 |
| --- | --- | --- |
| v1.0 | 2026-07-14 | Approved |
