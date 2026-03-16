# Task011：前端工程搭建与基础架构

> **业务模块**：工单系统（前端）  
> **依赖**：无  
> **预估工时**：2天  
> **对应产品文档**：3.2 技术选型建议、5.4 设计原则；项目规范 Vue3 + Vite + TypeScript + Element Plus

---

## 一、任务目标

在 `miduo-frontend` 下搭建工单系统前端工程骨架，采用 Vue3 + Vite6 + TypeScript + Element Plus + Pinia + Vue Router，配置目录结构、构建与代理、环境变量、代码规范，并与后端联调基础（代理、CORS）就绪。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 工单前端子应用/模块 | `miduo-frontend/src/views/ticket/` 或独立子工程（按现有项目约定） |
| 2 | Vite 配置 | 工单相关代理、构建入口、环境变量（VITE_API_BASE_URL 等） |
| 3 | 目录结构 | api/ticket、views/ticket、router/ticket、stores/ticket、types/ticket 等 |
| 4 | 依赖与脚本 | package.json 中工单所需依赖（axios、element-plus、pinia、vue-router 等），npm run dev 可启动 |
| 5 | 代码规范 | ESLint、Prettier 配置与脚本，与项目规范一致 |
| 6 | 环境配置 | .env.development / .env.production 中 API 基地址与后端联调地址 |
| 7 | README 或说明 | 工单前端本地启动、代理配置、环境变量说明 |

## 三、技术规范

- **框架**：Vue 3 + Vite 6 + TypeScript  
- **UI**：Element Plus  
- **状态**：Pinia  
- **路由**：Vue Router 4  
- **请求**：axios（封装与 baseURL 配置，为 Task013 鉴权预留）  
- **规范**：遵循 `.cursor/rules/frontend-ui-standards.mdc`，主色调 #1675d1  

## 四、验收标准

- [ ] `npm install` 与 `npm run dev` 可正常启动开发环境  
- [ ] 配置代理后能访问后端接口（可先用健康检查或占位接口验证）  
- [ ] 目录结构清晰，符合项目现有约定（若为 monorepo 则与现有模块风格一致）  
- [ ] ESLint/Prettier 通过，无阻塞性报错  

## 五、产出说明

本 Task 完成后，前端工程可独立运行并与后端联调，为 Task012（布局与路由）、Task013（认证与状态）、Task014（通用组件）提供基础。
