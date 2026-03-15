# Task016 管理端页面落地与基础联调记录

## 1. 联调范围

- 前端工程：`miduo-frontend`
- 联调页面：
  - `/manage/workflow`（工作流管理）
  - `/manage/sla`（SLA 管理）
  - `/manage/user`（用户管理）
  - `/manage/settings`（系统设置基础版）
- 对应任务：`task016-管理端页面落地与基础联调.md`

## 2. 接口联调结果

| 接口编号 | 方法 | 路径 | 页面 | 联调结果 |
|---|---|---|---|---|
| API000012 | GET | /api/workflow/list | 工作流管理 | ✅ 已打通 |
| API000013 | GET | /api/workflow/detail/{id} | 工作流管理 | ✅ 已打通 |
| API000018 | GET | /api/handler-group/list | 工作流管理 | ✅ 已打通 |
| API000019 | POST | /api/handler-group/create | 工作流管理 | ✅ 已打通 |
| API000001 | GET | /api/sla/policy/list | SLA 管理 | ✅ 已打通 |
| API000002 | POST | /api/sla/policy/create | SLA 管理 | ✅ 已打通 |
| API000003 | PUT | /api/sla/policy/update | SLA 管理 | ✅ 已打通 |
| API000404 | GET | /api/department/tree | 用户管理 | ✅ 已打通 |
| API000403 | GET | /api/user/list | 用户管理 | ✅ 已打通 |
| API000008 | GET | /api/notification/preference | 系统设置-通知偏好 | ✅ 已打通 |
| API000009 | PUT | /api/notification/preference/update | 系统设置-通知偏好 | ✅ 已打通 |

## 3. 页面能力落地清单

### 3.1 工作流管理页（/manage/workflow）

- 工作流列表（名称、模式、状态数、流转数、启用状态、时间）
- 工作流详情抽屉（状态图配置、流转规则、角色可见条件）
- 处理组管理区块（列表、关键字筛选、新建处理组弹窗）

### 3.2 SLA 管理页（/manage/sla）

- SLA 策略列表（优先级、响应时限、解决时限、预警阈值、状态）
- 新增策略弹窗
- 编辑策略弹窗
- 启用/停用切换（调用更新接口）

### 3.3 用户管理页（/manage/user）

- 左侧组织树 + 右侧用户表格
- 按部门筛选
- 关键字/状态筛选
- 用户详情抽屉（只读）

### 3.4 系统设置页（/manage/settings）

- 多 Tab 架构：基础参数、通知偏好、集成设置
- 通知偏好列表读取与批量保存
- 预留 Webhook 配置与企微群绑定容器，并保留路由状态：
  - `tab=integration&section=webhook`
  - `tab=integration&section=wecom`

## 4. UI 规范执行情况

- 表格统一使用 `BaseTable`（无边框、斑马纹、表头浅灰、居中）
- 分页统一使用 `BasePagination`（10/20/50/100，默认 20，展示 total）
- 空状态统一使用 `EmptyState`
- 反馈统一使用 `feedback.ts`（成功/警告/错误）

## 5. 构建与质量校验

在 `miduo-frontend` 执行：

- `npm run lint`：✅ 通过
- `npm run build`：✅ 通过

## 6. 问题清单与后续跟进

1. **工作流列表返回字段未提供 `updateTime`**  
   - 现状：`GET /api/workflow/list` 返回 DTO 中仅有 `createTime`。  
   - 前端处理：列表“更新时间”列临时使用 `createTime` 展示。  
   - 建议：在后端 `WorkflowListOutput` 增加 `updateTime` 并回传真实更新时间（建议归档至 Task017）。

2. **用户列表接口当前为非分页接口**  
   - 现状：`GET /api/user/list` 一次性返回列表。  
   - 前端处理：页面采用本地分页承接统一 UI 规范。  
   - 建议：后续若数据量增大，增加后端分页接口（可纳入 Task017 优化项）。
