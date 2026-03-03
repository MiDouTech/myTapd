# Task017 管理端接口封装与联调深化 - 治理记录

## 1. 治理目标

基于 Task016 已可用页面，进一步完成以下治理：

1. API 模块标准化：页面不直写 URL，统一走 `src/api/*`
2. 类型层标准化：页面对象可追踪到 `src/types/*`，避免 `any` 透传
3. 页面数据流标准化：统一 loading/空态/失败反馈与筛选分页排序行为
4. 接口注释补齐：方法注释包含接口编号与产品功能点

---

## 2. 本次新增/调整清单

### 2.1 API 模块

- 新增：`src/api/department.ts`
- 新增：`src/api/webhook.ts`
- 新增：`src/api/wecom.ts`
- 扩展：`src/api/notification.ts`（补齐 page/read/all/unread/preference）
- 调整：`src/api/user.ts`（移除部门树接口，聚焦 user 模块）
- 调整：`src/api/workflow.ts`（增加字段归一化映射层）

### 2.2 类型模块

- 新增：`src/types/department.ts`
- 新增：`src/types/webhook.ts`
- 新增：`src/types/wecom.ts`
- 扩展：`src/types/notification.ts`（补齐通知分页与未读数类型）
- 调整：`src/types/workflow.ts`（补充 `updateTime` 等字段）
- 调整：`src/types/user.ts`（移除部门树类型，职责下沉到 department）

### 2.3 页面层

- `WorkflowManageView.vue`
  - 接入统一排序（本地排序 + 分页）
  - 更新时间统一优先读取 `updateTime`
- `SlaManageView.vue`
  - 接入统一排序（本地排序 + 分页）
- `UserManageView.vue`
  - 部门树接口切换为 `api/department.ts`
  - 接入统一排序（本地排序 + 分页）
- `SettingsView.vue`
  - 维持通知偏好读写链路，错误处理策略与其他管理页一致

---

## 3. 页面 → API → DTO 映射表

| 页面 | API 方法 | 接口路径 | DTO 类型 |
|---|---|---|---|
| /manage/workflow | `getWorkflowList` | GET `/api/workflow/list` | `WorkflowListOutput[]` |
| /manage/workflow | `getWorkflowDetail` | GET `/api/workflow/detail/{id}` | `WorkflowDetailOutput` |
| /manage/workflow | `getHandlerGroupList` | GET `/api/handler-group/list` | `HandlerGroupListOutput[]` |
| /manage/workflow | `createHandlerGroup` | POST `/api/handler-group/create` | `HandlerGroupCreateInput` |
| /manage/sla | `getSlaPolicyList` | GET `/api/sla/policy/list` | `SlaPolicyOutput[]` |
| /manage/sla | `createSlaPolicy` | POST `/api/sla/policy/create` | `SlaPolicyCreateInput` |
| /manage/sla | `updateSlaPolicy` | PUT `/api/sla/policy/update` | `SlaPolicyUpdateInput` |
| /manage/user | `getDepartmentTree` | GET `/api/department/tree` | `DepartmentTreeOutput[]` |
| /manage/user | `getUserList` | GET `/api/user/list` | `UserListOutput[]` |
| /manage/settings | `getNotificationPreferences` | GET `/api/notification/preference` | `NotificationPreferenceOutput[]` |
| /manage/settings | `updateNotificationPreferences` | PUT `/api/notification/preference/update` | `NotificationPreferenceUpdateInput` |

---

## 4. 接口编号校对记录（前端侧）

### 4.1 本次已补齐编号注释

- 通知中心：API000004~API000009
- Webhook 配置：API000417~API000421
- 组织与用户：API000402~API000404
- 工作流与处理组：API000012、013、018、019

### 4.2 已识别历史重复编号（待 Task021 统一治理）

1. **企微群绑定接口 API000021~API000023 与 Bug简报模块 API000021~API000023 重复**  
   - 现状：后端控制器与前端封装均沿用历史编号。  
   - 当前处理：前端注释已明确“待 Task021 统一治理”。  
   - 责任归属：接口编号治理专项（Task021）。

---

## 5. 联调异常与差异清单

| 编号 | 问题描述 | 影响范围 | 当前处理 | 责任归属 |
|---|---|---|---|---|
| I-017-01 | `workflow/list` 未稳定提供 `updateTime` 字段 | 工作流列表“更新时间”展示 | API 层映射默认回落到 `createTime` | 后端 DTO 演进（Workflow） |
| I-017-02 | 用户列表为非分页接口，前端采用本地分页 | 用户管理大数据量场景 | 页面已统一分页交互，后续可升级后端分页 | 后端用户查询能力 |
| I-017-03 | 历史接口编号重复（Wecom/BugReport） | 文档一致性与审计追踪 | 注释标记待治理，避免新增重复 | 编号治理专项（Task021） |

---

## 6. 质量验证

在 `miduo-frontend` 执行：

- `npm run lint`：✅ 通过
- `npm run build`：✅ 通过

结论：Task017 范围内“接口封装与联调深化”已形成可扩展基线，可支撑 Task018/Task019/Task020 后续扩展。
