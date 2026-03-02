# Task010：数据看板、报表与开放能力

> **业务模块**：工单系统  
> **依赖**：Task004、Task007、Task009  
> **预估工时**：5天  
> **对应产品文档**：4.9 数据看板与报表、4.11 开放能力、5.3 看板视图

---

## 一、任务目标

实现工单概览仪表盘、多维度统计报表、看板视图（拖拽变更状态）；开放 RESTful API 与 Webhook；完善前端整体布局、响应式设计与移动端适配。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 工单概览仪表盘 | 待受理/处理中/已挂起/已完成/SLA 超时数量；工单趋势、分类分布、处理效率、SLA 达成率、人员工作量 TOP10 |
| 2 | 多维度报表 | 工单趋势、分类统计、处理效率、SLA 达成、人员工作量、超时工单 |
| 3 | 看板视图 | 按状态分列，支持拖拽卡片变更状态 |
| 4 | 报表导出 | Excel 导出 |
| 5 | RESTful API | /api/tickets CRUD、/api/tickets/{id}/status、/api/tickets/{id}/comments、/api/tickets/{id}/assign、/api/statistics/overview |
| 6 | Webhook 推送 | 工单事件配置 Webhook URL，HTTP 回调 |
| 7 | API 文档 | OpenAPI 3.0 规范 |
| 8 | 前端主框架 | 顶部导航、侧边栏、面包屑、三列布局 |
| 9 | 前端规范 | 主色调 #1675d1、表格样式、分页组件 |
| 10 | 移动端适配 | 企微 H5 工作台响应式（可选） |

## 三、接口清单（需填接口编号）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 概览仪表盘 | GET | /api/dashboard/overview | 工单概览数据 |
| 工单趋势 | GET | /api/dashboard/trend | 新建/关闭/积压趋势 |
| 分类分布 | GET | /api/dashboard/category-distribution | 各分类占比 |
| 处理效率 | GET | /api/dashboard/efficiency | 平均响应/解决时间 |
| SLA 达成 | GET | /api/dashboard/sla-achievement | 达成率统计 |
| 人员工作量 | GET | /api/dashboard/workload | 处理量 TOP10 |
| 看板数据 | GET | /api/ticket/kanban | 按状态分组工单 |
| 看板拖拽 | PUT | /api/ticket/kanban/move | 拖拽变更状态 |
| 开放 API-创建 | POST | /api/v1/tickets | 开放创建工单 |
| 开放 API-详情 | GET | /api/v1/tickets/{id} | 开放获取详情 |
| 开放 API-列表 | GET | /api/v1/tickets | 开放列表查询 |
| 开放 API-统计 | GET | /api/v1/statistics/overview | 开放统计概览 |
| Webhook 配置 | CRUD | /api/webhook/config | Webhook 配置管理 |

## 四、验收标准

- [ ] 仪表盘数据正确，图表可正常展示  
- [ ] 看板视图支持拖拽变更状态  
- [ ] 开放 API 可对外调用（含鉴权）  
- [ ] Webhook 事件正确推送  
- [ ] 前端整体布局与规范符合产品 5.2、5.3  

## 五、产出说明

本 task 完成后，工单系统核心功能闭环，可对外集成与数据分析支撑管理决策。
