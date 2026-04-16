# 米多工单系统前端（Task011-Task015）

## 技术栈

- Vue 3 + TypeScript + Vite
- Element Plus
- Pinia
- Vue Router 4
- Axios

## 本地启动

```bash
cd miduo-frontend
npm install
npm run dev
```

默认开发端口：`5173`

## 环境变量

- `.env.development`
  - `VITE_API_BASE_URL=/api`
  - `VITE_USE_PROXY=true`
  - `VITE_API_PROXY_TARGET=http://localhost:8080`
  - `VITE_WECOM_OAUTH_URL=`（企微授权地址）
- `.env.production`
  - 默认关闭代理，按部署环境配置 `VITE_API_BASE_URL`

## 代理联调

开发环境使用 Vite 代理将 `/api` 转发到后端服务地址（默认 `http://localhost:8080`），可直接联调以下模块：

- 认证：`/api/auth/*`
- 用户：`/api/user/*`
- 分类：`/api/category/*`
- 模板：`/api/template/*`
- 工单：`/api/ticket/*`

## 代码规范

```bash
npm run lint
npm run format
```

## 页面与模块说明

- 布局与路由：顶部导航、侧边栏、面包屑、占位页（Task012）
- 认证与状态：企微登录页、Token 管理、路由守卫、401 处理（Task013）
- 设计系统：标准表格、标准分页、空状态、消息反馈（Task014）
- 工单模块：列表、详情、创建、分类管理与后端接口联调（Task015）
  - 工单描述：企微自然语言建单若把「商户编号：… 公司名称：…」拼成一行，前端会用 `formatTicketDescriptionForDisplay` 自动分段换行；已是富文本（含段落/表格等）时原样展示
