# Task019 Bug简报前端模块落地 - 联调记录

## 1. 联调范围

- 前端工程：`miduo-frontend`
- 对应任务：`doc/workflow/工单系统/task019-Bug简报前端模块落地.md`
- 核心目标：
  1. Bug简报列表、详情、创建/编辑、统计看板页面可用
  2. 与 Task009 已交付接口完成前端联调
  3. 打通“工单 -> 简报 -> 工单”页面跳转链路

---

## 2. 交付清单

| 序号 | 交付物 | 路径 | 结果 |
|---|---|---|---|
| 1 | Bug简报列表页 | `miduo-frontend/src/views/bugreport/BugReportListView.vue` | ✅ 已完成 |
| 2 | Bug简报详情页 | `miduo-frontend/src/views/bugreport/BugReportDetailView.vue` | ✅ 已完成 |
| 3 | Bug简报创建/编辑页 | `miduo-frontend/src/views/bugreport/BugReportEditView.vue` | ✅ 已完成 |
| 4 | Bug简报统计页 | `miduo-frontend/src/views/bugreport/BugReportStatisticsView.vue` | ✅ 已完成 |
| 5 | 路由接入 | `miduo-frontend/src/router/routes.ts` | ✅ 已完成 |
| 6 | 菜单入口接入 | `miduo-frontend/src/layouts/MainLayout.vue` | ✅ 已完成 |
| 7 | 状态显示工具 | `miduo-frontend/src/utils/bugreport.ts` | ✅ 已完成 |
| 8 | 字典懒加载缓存 | `miduo-frontend/src/utils/bugreport-dict-cache.ts` | ✅ 已完成 |
| 9 | 通知跳转到简报详情 | `MainLayout.vue`、`NotificationCenterView.vue` | ✅ 已完成 |
| 10 | 工单详情关联简报入口 | `miduo-frontend/src/views/ticket/TicketDetailView.vue` | ✅ 已完成 |

---

## 3. 页面能力闭环说明

### 3.1 列表页（`/bug-report`）

- 支持分页、状态/缺陷分类/审核人/责任人/创建时间筛选
- 支持排序（创建时间、更新时间、提交时间）
- 行级操作按状态控制：
  - 待填写/已退回：可编辑、可提交审核、可作废
  - 待审核：可作废
  - 已归档：仅查看
  - 已作废：仅查看

### 3.2 详情页（`/bug-report/detail/:id`）

- 展示基础信息、责任人、关联工单、附件、状态日志
- 状态与角色联合控制流程操作：
  - 提交审核（草稿/退回）
  - 审核通过/驳回（待审核 + 审核人或管理角色）
  - 作废（非已归档/已作废）
- 关联工单支持跳转工单详情

### 3.3 创建/编辑页（`/bug-report/edit/:id?`）

- 支持“保存草稿”与“保存并提交”分离按钮
- 接入逻辑归因（级联）与缺陷分类字典
- 字典采用懒加载缓存，避免重复请求
- 支持工单编号/标题远程搜索并多选关联工单
- 表单校验覆盖：必填项、日期区间、关联工单完整性

### 3.4 统计页（`/bug-report/statistics`）

- 指标卡：总数、及时提交数、及时率
- 分布数据：逻辑归因、缺陷分类、引入项目 Top、责任人统计
- 支持近 7/14/30 天快捷筛选与自定义时间范围
- 支持 CSV 导出

### 3.5 跳转链路

- 工单详情新增“关联Bug简报”区块，可跳转简报详情
- 简报详情可跳转回关联工单详情
- 通知中心与顶部通知对 `reportId` 统一跳转到简报详情页

---

## 4. 接口联调清单

| 接口编号 | 方法 | 路径 | 前端用途 | 联调结果 |
|---|---|---|---|---|
| API000020 | GET | /api/bug-report/page | 简报列表分页 | ✅ 已打通 |
| API000021 | GET | /api/bug-report/detail/{id} | 简报详情 | ✅ 已打通 |
| API000022 | POST | /api/bug-report/create | 创建草稿 | ✅ 已打通 |
| API000023 | PUT | /api/bug-report/update/{id} | 编辑草稿 | ✅ 已打通 |
| API000024 | PUT | /api/bug-report/submit/{id} | 提交审核 | ✅ 已打通 |
| API000025 | PUT | /api/bug-report/approve/{id} | 审核通过 | ✅ 已打通 |
| API000026 | PUT | /api/bug-report/reject/{id} | 审核驳回 | ✅ 已打通 |
| API000027 | PUT | /api/bug-report/void/{id} | 作废简报 | ✅ 已打通 |
| API000028 | GET | /api/bug-report/statistics | 统计看板 | ✅ 已打通 |
| API000029 | GET | /api/dict/logic-cause | 逻辑归因字典 | ✅ 已打通 |
| API000030 | GET | /api/dict/defect-category | 缺陷分类字典 | ✅ 已打通 |

---

## 5. 质量校验结果

在 `miduo-frontend` 执行：

- `npm run build`：✅ 通过
- `npm run lint`：✅ 通过

---

## 6. 风险与后续建议

1. 当前统计页以表格+指标卡为主，后续可在 Task022 后按性能预算增加图表可视化。  
2. 构建产物存在大 chunk 提示，建议后续在全局层面评估手动分包策略。  
3. 审核角色控制目前采用“审核人 + 管理角色白名单”前端兜底，最终权限仍以后端判定为准。
