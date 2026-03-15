# Task016：管理端页面落地与基础联调

> **业务模块**：工单系统（前端）  
> **依赖**：Task011、Task012、Task013、Task014、Task015、Task003、Task005、Task007  
> **预估工时**：3~4天  
> **对应产品文档**：5.2 主页面结构、4.4 工作流引擎、4.7 SLA 管理、4.10 用户管理

---

## 一、任务目标

将当前 `管理` 菜单下的占位页面（工作流管理、SLA管理、用户管理、系统设置）替换为可操作页面，完成首轮真实数据联调，确保“路由可访问 + 页面可查询 + 基础维护可执行”。

> 说明：本 Task 聚焦“页面从占位到可用”。系统设置中的高级集成能力（Webhook、企微群绑定）在 Task020 继续深化。

## 二、实施范围

### 2.1 工作流管理页（/manage/workflow）

- 展示工作流列表：名称、模式、状态数、流转数、启用状态、更新时间
- 支持查看工作流详情抽屉：状态图配置、流转规则、角色可见条件
- 内嵌处理组管理区块：处理组列表、新建处理组弹窗
- 复用接口：
  - `GET /api/workflow/list`（API000012）
  - `GET /api/workflow/detail/{id}`（API000013）
  - `GET /api/handler-group/list`（API000018）
  - `POST /api/handler-group/create`（API000019）

### 2.2 SLA 管理页（/manage/sla）

- 展示 SLA 策略列表：优先级、响应时限、解决时限、预警阈值、状态
- 支持新增策略和编辑策略（同弹窗或抽屉）
- 支持启用/停用切换（通过更新接口提交）
- 复用接口：
  - `GET /api/sla/policy/list`（API000001）
  - `POST /api/sla/policy/create`（API000002）
  - `PUT /api/sla/policy/update`（API000003）

### 2.3 用户管理页（/manage/user）

- 左侧组织树 + 右侧用户表格布局
- 支持按部门筛选用户，展示账号状态、角色、邮箱、手机号、职位
- 支持用户详情抽屉（只读）便于排查权限与归属
- 复用接口：
  - `GET /api/department/tree`（API000404）
  - `GET /api/user/list`（API000403）

### 2.4 系统设置页（/manage/settings）基础版

- 页面从空态改为多 Tab 配置框架（基础参数、通知偏好、集成设置）
- 本阶段至少打通通知偏好设置能力：
  - `GET /api/notification/preference`（API000008）
  - `PUT /api/notification/preference/update`（API000009）
- 为 Task020 预留“Webhook配置”“企微群绑定”子区块容器与路由状态，不再使用空白页

## 三、详细实施内容

### 3.1 页面与组件改造

| 页面 | 现状 | 本 Task 改造目标 |
|---|---|---|
| WorkflowManageView.vue | 空状态占位 | 列表+详情+处理组基础维护 |
| SlaManageView.vue | 空状态占位 | SLA策略列表+新增/编辑 |
| UserManageView.vue | 空状态占位 | 部门树+用户列表+详情 |
| SettingsView.vue | 空状态占位 | Tab化配置页+通知偏好联调 |

### 3.2 UI 规范落实（必须）

- 表格统一使用通用封装，遵循 `frontend-ui-standards.mdc`：
  - 表头浅灰背景、无边框、斑马纹、居中、操作列固定右侧（按需）
- 分页统一使用 10/20/50/100，默认 20，展示“共X条”
- 空数据、加载、提交反馈使用统一组件与反馈方法，不允许散落硬编码提示

### 3.3 鉴权与容错

- 页面级请求统一走 `request.ts`，自动附带 Token
- 接口 401 自动回登录（Task013 机制复用）
- 页面内异常要区分：
  - 查询失败：提示错误并保留当前筛选条件
  - 提交失败：弹窗不自动关闭，保留已填表单

## 四、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 工作流管理页面（可用） | `miduo-frontend/src/views/manage/WorkflowManageView.vue` |
| 2 | SLA管理页面（可用） | `miduo-frontend/src/views/manage/SlaManageView.vue` |
| 3 | 用户管理页面（可用） | `miduo-frontend/src/views/manage/UserManageView.vue` |
| 4 | 系统设置页面基础版 | `miduo-frontend/src/views/manage/SettingsView.vue` |
| 5 | 管理端接口封装（若缺失则补） | `miduo-frontend/src/api/*` |
| 6 | 管理端类型定义（若缺失则补） | `miduo-frontend/src/types/*` |
| 7 | 联调记录与问题清单 | `doc/workflow/test/` 下新增联调记录文档 |

## 五、实施步骤建议（按天拆分）

### Day 1
- 完成工作流页面列表与详情联调
- 完成处理组列表、创建弹窗联调

### Day 2
- 完成 SLA 页面列表 + 新增 + 编辑
- 完成统一校验规则与状态提示

### Day 3
- 完成用户管理页面（组织树 + 用户表）
- 完成系统设置基础版与通知偏好联调

### Day 4（缓冲）
- 统一 UI 细节、联调回归、修复问题
- 输出联调缺陷清单并分类到 Task017/Task020

## 六、验收标准

- [ ] 管理菜单下 4 个页面全部无占位文案，均可加载真实数据  
- [ ] Workflow/SLA/User 页面具备最小可用操作（查询 + 基础维护）  
- [ ] 系统设置页具备基础配置结构，通知偏好可读写  
- [ ] 所有页面符合前端表格与分页规范，视觉风格统一  
- [ ] `npm run lint`、`npm run build` 通过，联调过程无阻塞错误  

## 七、风险与回滚策略

- **风险1：后端字段与前端类型不一致**  
  处理：以 `ticket-entity` DTO 为准，补充字段映射层，不直接在页面散改。

- **风险2：页面并发请求导致加载态抖动**  
  处理：拆分局部 loading（列表、详情、提交）并避免全页锁死。

- **风险3：系统设置需求边界膨胀**  
  处理：本 Task 仅交付基础可用与通知偏好，集成配置复杂能力移交 Task020。

## 八、与后续任务衔接

- Task017：管理端 API 与类型层深化、统一封装与错误治理  
- Task018：通知中心与头部铃铛闭环  
- Task020：系统设置高级能力（Webhook、企微群绑定）完成闭环
