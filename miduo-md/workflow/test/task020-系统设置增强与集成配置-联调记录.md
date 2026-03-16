# Task020 系统设置增强与集成配置 - 联调记录

## 1. 联调范围

- 前端工程：`miduo-frontend`
- 后端工程：`ticket-platform`
- 对应任务：`doc/workflow/工单系统/task020-系统设置增强与集成配置.md`
- 核心目标：
  1. 系统设置页“集成配置”从占位升级为可用能力
  2. Webhook 配置完成分页、筛选、CRUD 闭环
  3. 企微群绑定完成列表、分类筛选、新增/编辑闭环
  4. 提供统一校验、提交反馈、最近变更高亮

---

## 2. 交付清单

| 序号 | 交付物 | 路径 | 结果 |
|---|---|---|---|
| 1 | 系统设置主页面集成配置收口 | `miduo-frontend/src/views/manage/SettingsView.vue` | ✅ 已完成 |
| 2 | Webhook 管理子组件 | `miduo-frontend/src/views/manage/components/WebhookConfigPanel.vue` | ✅ 已完成 |
| 3 | 企微群绑定子组件 | `miduo-frontend/src/views/manage/components/WecomGroupBindingPanel.vue` | ✅ 已完成 |
| 4 | Webhook 类型补充（变更人字段兼容） | `miduo-frontend/src/types/webhook.ts` | ✅ 已完成 |
| 5 | 企微绑定类型补充（变更人字段兼容） | `miduo-frontend/src/types/wecom.ts` | ✅ 已完成 |
| 6 | 表格行高亮能力透传 | `miduo-frontend/src/components/common/BaseTable.vue` | ✅ 已完成 |

---

## 3. 能力闭环说明

### 3.1 系统设置页结构收口

- 保留“基础参数 / 通知偏好 / 集成设置”三分组结构
- 在“集成设置”中接入：
  - `Webhook配置`
  - `企微群绑定`
- 保留路由状态（`tab=integration&section=webhook|wecom`）支持直达与刷新恢复

### 3.2 Webhook 配置管理

- 列表能力：
  - 分页展示
  - 关键字筛选（URL/描述）
  - 事件类型筛选
  - 状态筛选
  - 列排序（前端当前页排序）
- 表单能力：
  - 新增/编辑弹窗
  - URL 校验（仅允许 `http/https`）
  - 事件类型至少选择一项
  - 超时与重试次数数值区间校验
- 变更能力：
  - 删除二次确认
  - 保存后刷新列表
  - 最近变更行高亮 + 最近变更时间/变更人展示

### 3.3 企微群绑定管理

- 列表能力：
  - 群ID、群名称、默认分类、模板、状态、更新时间展示
  - 关键字筛选
  - 按分类筛选
  - 按状态筛选
  - 列排序（前端排序）
- 表单能力：
  - 新增/编辑弹窗
  - 分类下拉 + 模板下拉（模板驱动分类可选范围）
  - 群ID唯一性前置提示（本地列表预检）
- 变更能力：
  - 保存后刷新列表
  - 最近变更行高亮 + 最近变更时间/变更人展示

---

## 4. 接口联调清单

| 接口编号 | 方法 | 路径 | 用途 | 联调结果 |
|---|---|---|---|---|
| API000417 | GET | /api/webhook/config/page | Webhook配置分页查询 | ✅ 已打通 |
| API000418 | GET | /api/webhook/config/detail/{id} | Webhook配置详情 | ✅ 已打通 |
| API000419 | POST | /api/webhook/config/create | Webhook配置创建 | ✅ 已打通 |
| API000420 | PUT | /api/webhook/config/update/{id} | Webhook配置更新 | ✅ 已打通 |
| API000421 | DELETE | /api/webhook/config/delete/{id} | Webhook配置删除 | ✅ 已打通 |
| API000021 | GET | /api/wecom/group-binding/list | 企微群绑定列表 | ✅ 已打通 |
| API000022 | POST | /api/wecom/group-binding/create | 企微群绑定创建 | ✅ 已打通 |
| API000023 | PUT | /api/wecom/group-binding/update/{id} | 企微群绑定更新 | ✅ 已打通 |

> 注：API000021~API000023 的历史编号重复问题按 Task021 统一治理。

---

## 5. 质量校验结果

### 5.1 前端（miduo-frontend）

- `npm run lint`：✅ 通过
- `npm run build`：✅ 通过

### 5.2 后端（ticket-platform）

- 本次未修改后端代码，沿用 Task017/Task018 已打通接口，无新增编译变更

---

## 6. 风险与后续建议

1. 企微群绑定接口当前未返回模板字段，前端模板列基于分类模板关系进行推导展示；若后续存在“分类与模板解绑”场景，建议后端补充显式模板字段。  
2. Webhook/企微配置属于高风险运维项，建议后续在 Task022 增加“配置变更审计与回滚演练”检查项。  
3. 当前最近变更“变更人”依赖接口返回字段（`updateBy/createBy`）；后端若未返回则展示默认值“系统”。
