# 缺陷管理模块 - 开发任务清单

> 版本：v1.0  
> 日期：2026-03-07  
> 关联文档：缺陷管理模块PRD.md、缺陷管理模块TDD.md

---

## 任务概览

| Task | 标题 | 模块 | 状态 |
|------|------|------|------|
| task001 | 产品方案设计 & 技术分析 | 文档 | ✅已完成 |
| task002 | 新增 BugChangeTypeEnum 枚举 | ticket-common | ✅已完成 |
| task003 | 新增 DTO 类（变更历史响应/字段明细/简报摘要） | ticket-entity | ✅已完成 |
| task004 | 扩展 TicketDetailOutput，增加 bugSummaryInfo 字段 | ticket-entity | ✅已完成 |
| task005 | 新增 BugChangeHistoryPageInput | ticket-entity | ✅已完成 |
| task006 | TicketLogMapper 扩展 + Mapper XML | ticket-infrastructure | ✅已完成 |
| task007 | 新增 TicketChangeHistoryRecorder（变更检测 + 写入） | ticket-application | ✅已完成 |
| task008 | 增强 TicketBugApplicationService（显式记录变更） | ticket-application | ✅已完成 |
| task009 | 新增 TicketChangeHistoryApplicationService（历史查询） | ticket-application | ✅已完成 |
| task010 | 扩展 TicketApplicationService.getTicketDetail()（组装 bugSummaryInfo） | ticket-application | ✅已完成 |
| task011 | **修复 BugReportApplicationService DB 红线**（批量 insert） | ticket-application | ✅已完成 |
| task012 | 新增 TicketChangeHistoryController | ticket-controller | ✅已完成 |
| task013 | 前端：新增 TypeScript 类型定义 | miduo-frontend | ✅已完成 |
| task014 | 前端：新增 API 封装 | miduo-frontend | ✅已完成 |
| task015 | 前端：开发 BugStatusBadge、BugDetailInfoPanel 组件 | miduo-frontend | ✅已完成 |
| task016 | 前端：开发 BugChangeHistory、BugChangeHistoryItem 组件 | miduo-frontend | ✅已完成 |
| task017 | 前端：改造 TicketDetailView（变更历史Tab + 右侧面板独立） | miduo-frontend | ✅已完成 |
| task018 | 联调测试 | 全栈 | ⏳待测试 |

---

## task001：产品方案设计 & 技术分析

**状态**：✅已完成  
**输出物**：
- `miduo-md/business/缺陷管理模块PRD.md`
- `miduo-md/business/缺陷管理模块TDD.md`
- `miduo-md/workflow/功能接口对应关系.md`
- `miduo-md/workflow/@缺陷管理/task001-task018.md`（本文档）

---

## task002：新增 BugChangeTypeEnum 枚举

**文件路径**：  
`ticket-platform/ticket-common/src/main/java/com/miduo/cloud/ticket/common/enums/BugChangeTypeEnum.java`

**枚举值**：
- `CREATE`（创建缺陷）
- `MANUAL_CHANGE`（手动变更）
- `STATUS_CHANGE`（状态流转）
- `SYSTEM_AUTO`（系统自动）
- `COMMENT`（添加评论）
- `ATTACHMENT`（附件操作）

---

## task003：新增 DTO 类

**文件路径**：`ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/`

新增三个类：
1. `BugFieldChangeItem.java`（单字段变更明细）
2. `BugChangeHistoryOutput.java`（变更历史条目响应）
3. `BugSummaryInfoOutput.java`（简报摘要信息）

具体字段定义见 TDD §4.2。

---

## task004：扩展 TicketDetailOutput

**文件路径**：  
`ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketDetailOutput.java`

**变更**：在现有字段末尾增加：
```java
private BugSummaryInfoOutput bugSummaryInfo;
```

---

## task005：新增 BugChangeHistoryPageInput

**文件路径**：  
`ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/BugChangeHistoryPageInput.java`

---

## task006：TicketLogMapper 扩展 + Mapper XML

**Java 文件**：  
`ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/ticket/mapper/TicketLogMapper.java`

新增方法：`selectChangeHistoryByTicketId()`

**XML 文件**（如不存在则新建）：  
`ticket-platform/ticket-infrastructure/src/main/resources/mapper/TicketLogMapper.xml`

---

## task007：新增 TicketChangeHistoryRecorder

**文件路径**：  
`ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketChangeHistoryRecorder.java`

**核心职责**：
1. `record(ticketId, userId, changeType, changes)` — 序列化并写入 ticket_log
2. `detectCustomerInfoChanges(old, input)` — 客服信息变更检测
3. `detectTestInfoChanges(old, input)` — 测试信息变更检测
4. `detectDevInfoChanges(old, input)` — 开发信息变更检测

**约束**：remark JSON 不超过 450 字符；text 字段截断为前 200 字符。

---

## task008：增强 TicketBugApplicationService

**文件路径**：  
`ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketBugApplicationService.java`

**变更点**：
1. 注入 `TicketChangeHistoryRecorder`
2. `updateCustomerInfo()` — 在保存前检测变更，保存后记录日志
3. `updateTestInfo()` — 同上
4. `updateDevInfo()` — 同上
5. 抽取 `applyXxxChanges()` 私有方法，分离"赋值逻辑"与"变更检测逻辑"

---

## task009：新增 TicketChangeHistoryApplicationService

**文件路径**：  
`ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketChangeHistoryApplicationService.java`

**核心职责**：
1. `listChangeHistory(ticketId, changeType, fieldName)` — 查询 + 解析 + 批量查用户 + 组装响应
2. `fieldName` 过滤在内存完成（避免 MySQL JSON 函数全表扫描）

---

## task010：扩展 TicketApplicationService.getTicketDetail()

**文件路径**：  
`ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketApplicationService.java`

**变更点**：在 `getTicketDetail()` 末尾调用 `buildBugSummaryInfo(ticketId)` 组装简报摘要信息。

关联查询：`bug_report_ticket` → `bug_report` → `bug_report_responsible`，全部批量查询。

---

## task011：修复 BugReportApplicationService DB 红线（紧急）

**文件路径**：  
`ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/bugreport/BugReportApplicationService.java`

**问题**：`syncReportTickets()` 方法内循环调用 `bugReportTicketMapper.insert()`（违反数据库红线）

**修复方案**：改为先构建 List，再批量 `saveBatch()`，见 TDD §4.4.4。

---

## task012：新增 TicketChangeHistoryController

**文件路径**：  
`ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/ticket/TicketChangeHistoryController.java`

**接口**：`GET /api/ticket/{ticketId}/change-history`（API000501）

---

## task013：前端类型定义

**文件路径**：`miduo-frontend/src/types/ticket.ts`

新增：`BugFieldChangeItem`、`BugChangeHistoryOutput`、`BugSummaryInfoOutput` 接口类型，并在 `TicketDetailOutput` 中新增 `bugSummaryInfo?: BugSummaryInfoOutput` 字段。

---

## task014：前端 API 封装

**文件路径**：`miduo-frontend/src/api/ticket.ts`

新增 `getTicketChangeHistory()` 函数（API000501）。

---

## task015：前端组件 BugStatusBadge、BugDetailInfoPanel

**文件路径**：
- `miduo-frontend/src/views/ticket/components/bug/BugStatusBadge.vue`
- `miduo-frontend/src/views/ticket/components/bug/BugDetailInfoPanel.vue`

**BugStatusBadge**：props = `{ status: string, statusLabel: string }`，根据状态渲染对应颜色的徽章。

**BugDetailInfoPanel**：props = `{ detail: TicketDetailOutput, readonly: boolean }`，展示右侧基础信息面板所有字段，可编辑字段支持行内编辑。

---

## task016：前端组件 BugChangeHistory、BugChangeHistoryItem

**文件路径**：
- `miduo-frontend/src/views/ticket/components/bug/BugChangeHistory.vue`
- `miduo-frontend/src/views/ticket/components/bug/BugChangeHistoryItem.vue`
- `miduo-frontend/src/views/ticket/components/bug/BugChangeHistoryFilter.vue`

**BugChangeHistory**：接收 `ticketId`，调用 `getTicketChangeHistory()`，本地过滤，触发 `count-update` 事件更新 Tab 角标。

**BugChangeHistoryItem**：接收单条 `BugChangeHistoryOutput`，渲染序号/时间/人/类型/字段变更列表。

**BugChangeHistoryFilter**：包含"变更内容"和"变更方式"两个 Select，emit change 事件。

---

## task017：改造 TicketDetailView

**文件路径**：`miduo-frontend/src/views/ticket/TicketDetailView.vue`

**变更点**：
1. 引入 `BugStatusBadge`，在顶部展示状态徽章
2. 引入 `BugDetailInfoPanel` 替换右侧信息展示逻辑
3. 在 el-tabs 中新增「变更历史」Tab，引入 `BugChangeHistory` 组件
4. 顶部操作按钮组增加上一条/下一条导航（读取 history.state 中的 prevId/nextId）
5. 评论 Tab 增加评论数角标

---

## task018：联调测试

**测试要点**（使用真实数据，禁止 Mock）：

1. **变更记录完整性**
   - 创建缺陷工单 → 检查 ticket_log 是否有 CREATE 记录
   - 修改客服信息 → 检查 ticket_log.remark JSON 是否包含正确的字段变更
   - 状态流转 → 检查是否有 STATUS_CHANGE 类型记录

2. **变更历史查询**
   - 不带筛选 → 返回全部记录，按时间倒序
   - changeType=STATUS_CHANGE → 仅返回状态流转记录
   - fieldName=severity_level → 仅返回涉及缺陷等级字段的记录

3. **简报摘要信息**
   - 关联简报的工单 → bugSummaryInfo 正确返回缺陷划分、有效报告、责任人
   - 未关联简报的工单 → bugSummaryInfo 为 null

4. **逾期判断**
   - expected_time < now 且非终态 → isOverdue=true
   - 已关闭工单 → isOverdue=false（不判断逾期）

5. **前端 UI 验证**
   - 变更历史 Tab 角标数量正确
   - 筛选功能生效（本地过滤）
   - 状态徽章颜色正确
   - 上一条/下一条导航正常跳转
