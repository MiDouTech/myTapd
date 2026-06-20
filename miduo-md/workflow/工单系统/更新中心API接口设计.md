# 更新中心 API 接口设计

> **版本**：v1.0  
> **日期**：2026-06-14  
> **关联任务**：Task061  
> **关联产品章节**：管理 / 更新中心

---

## 1. 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 | 说明 |
|---|---|---|---|---|
| API000521 | 查询待发布更新 | GET | /api/update-center/current-week | 基于 `changelogs/*.md` 返回待发布更新碎片 |
| API000522 | 查询已发布更新 | GET | /api/update-center/releases | 基于 `CHANGELOG.md` 返回版本更新记录 |
| API000523 | 查询指定版本更新详情 | GET | /api/update-center/releases/detail/{version} | 返回单个版本的完整更新条目 |
| API000524 | 查询Git提交日志 | GET | /api/update-center/github-logs | 返回本地 Git 提交记录 |
| API000525 | 查询周报列表 | GET | /api/update-center/weekly-reports | 基于 `doc/report.*.md` 返回周报列表 |
| API000526 | 查询周报详情 | GET | /api/update-center/weekly-reports/detail/{fileName} | 返回指定周报 Markdown 正文 |

---

## 2. 接口详情

### 2.1 查询待发布更新（API000521）

请求示例：

```http
GET /api/update-center/current-week?daysLimit=4&daysOffset=0
```

参数说明：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| daysLimit | Integer | 否 | 本次返回多少个日期组，默认 4 |
| daysOffset | Integer | 否 | 跳过多少个日期组，用于加载更多，默认 0 |
| force | Boolean | 否 | 是否强制刷新，当前为兼容参数 |

响应 `data` 示例：

```json
{
  "weekStart": "2026-06-14",
  "weekEnd": "2026-06-14",
  "dataSourceAvailable": true,
  "source": "local",
  "fetchedAt": "2026-06-14T10:40:00Z",
  "totalDays": 1,
  "totalEntries": 1,
  "daysOffset": 0,
  "hasMore": false,
  "fragments": [
    {
      "fileName": "2026-06-14_update-center.md",
      "date": "2026-06-14",
      "entries": [
        {
          "type": "feat",
          "module": "更新中心",
          "description": "新增工单系统管理端更新中心"
        }
      ]
    }
  ]
}
```

### 2.2 查询已发布更新（API000522）

请求示例：

```http
GET /api/update-center/releases?limit=8&summary=true
```

参数说明：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| limit | Integer | 否 | 返回版本数量，默认 8，最大 100 |
| summary | Boolean | 否 | 是否只返回概要；为 true 时可再调用详情接口 |
| force | Boolean | 否 | 是否强制刷新，当前为兼容参数 |

### 2.3 查询指定版本更新详情（API000523）

请求示例：

```http
GET /api/update-center/releases/detail/v1.0.0
```

参数说明：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version | String | 是 | 版本号，如 `v1.0.0` |
| force | Boolean | 否 | 是否强制刷新，当前为兼容参数 |

### 2.4 查询Git提交日志（API000524）

请求示例：

```http
GET /api/update-center/github-logs?limit=80
```

参数说明：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| limit | Integer | 否 | 返回提交数量，默认 80，最大 1000 |
| before | String | 否 | 游标 SHA，用于加载更早提交 |
| force | Boolean | 否 | 是否强制刷新，当前为兼容参数 |

响应 `data.logs[]` 示例：

```json
{
  "sha": "abcdef123456",
  "shortSha": "abcdef1",
  "message": "feat: add update center",
  "authorName": "miduo",
  "authorAvatarUrl": null,
  "commitTimeUtc": "2026-06-14T10:40:00Z",
  "htmlUrl": "https://github.com/MiDouTech/myTapd/commit/abcdef123456"
}
```

### 2.5 查询周报列表（API000525）

请求示例：

```http
GET /api/update-center/weekly-reports
```

响应 `data` 示例：

```json
{
  "dataSourceAvailable": true,
  "source": "github",
  "fetchedAt": "2026-06-20T06:40:00Z",
  "totalReports": 1,
  "reports": [
    {
      "fileName": "report.2026-W24.md",
      "title": "周报 2026-W24",
      "reportWeek": "2026-W24",
      "period": "2026-06-08 ~ 2026-06-14",
      "updatedAt": null
    }
  ]
}
```

### 2.6 查询周报详情（API000526）

请求示例：

```http
GET /api/update-center/weekly-reports/detail/report.2026-W24.md
```

响应 `data` 示例：

```json
{
  "fileName": "report.2026-W24.md",
  "title": "周报 2026-W24",
  "reportWeek": "2026-W24",
  "period": "2026-06-08 ~ 2026-06-14",
  "content": "# 周报 2026-W24 ...",
  "dataSourceAvailable": true,
  "source": "github",
  "fetchedAt": "2026-06-20T06:40:00Z"
}
```

---

## 3. 通用返回结构

接口统一使用 `ApiResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1781433600000
}
```

---

## 4. 说明

1. 本次接口均为只读接口，不新增数据库表。
2. `source=local` 表示来自本地仓库文件或本地 Git；`source=none` 表示数据源不存在。
3. 前端请求路径不写 `/api` 前缀，由 `VITE_API_BASE_URL` 统一补齐。
4. 周报文件命名必须符合 `report.YYYY-Wxx.md`，例如 `report.2026-W24.md`。
