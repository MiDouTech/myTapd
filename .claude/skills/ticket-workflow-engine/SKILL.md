# SKILL: 工单流转引擎设计

## 触发词
"工单流转"、"状态机"、"工作流引擎"、"ticket workflow"、"流转设计"

---

## 一、市面最优工单系统设计思想总结

### 1.1 核心设计范式

| 系统 | 核心引擎 | 关键设计 |
|---|---|---|
| ServiceNow | 有限状态机 + CMDB | State Flows（状态流+UI动作+业务规则三合一）|
| Jira | 工作流配置器 | 角色权限 × 条件过滤 × 后置动作 |
| Zendesk | Action Flows | 事件触发 + 最多50步串行编排 |
| TAPD | 高级流转配置 | 流转前/流转时/流转后三阶段钩子 |
| Camunda | BPMN + DMN | 可视化流程图 + 决策表 |

### 1.2 六大设计原则（综合最佳实践）

1. **状态机强制性**：所有状态流转必须经过引擎校验，不存在任何绕过路径
2. **配置驱动**：工作流存储在数据库中（JSON），运行时动态加载，不硬编码
3. **角色精确匹配**：基于系统角色 + 工单身份双重判断（不仅仅依赖工单 creatorId/assigneeId）
4. **动态操作**：前端从后端动态获取可用操作列表，不再硬编码按钮
5. **完整审计**：每次流转写入独立流水表（含角色、时间、变更内容）
6. **N+1 消除**：分派策略使用批量 SQL 查询，避免循环内查数据库

### 1.3 工作流状态节点类型

```
INITIAL     - 初始状态（工单创建后进入）
INTERMEDIATE - 中间状态（处理流程节点）
TERMINAL    - 终态（完成/关闭/驳回，不可再流转）
```

### 1.4 流转规则关键属性

```json
{
  "id": "t01",            // 规则唯一ID（支持精确触发）
  "from": "pending_accept",
  "to": "processing",
  "name": "受理",
  "allowedRoles": ["HANDLER", "ADMIN"],
  "requireRemark": false, // 是否必填备注
  "allowTransfer": true,  // 是否支持流转时变更处理人
  "isReturn": false       // 是否为退回流转
}
```

### 1.5 SLA 三段式设计

```
START_RESPONSE  → 开始计算首次响应时间（进入初始状态）
START_RESOLVE   → 开始计算解决时间（处理人受理后）
PAUSE           → 暂停计时（挂起/待验收）
STOP            → 停止（终态）
```

---

## 二、工单流转引擎技术架构

### 2.1 分层架构

```
Controller 层
  ↓  接收请求，调用 AppService
TicketWorkflowAppService（应用层）
  ↓  角色解析、业务校验、事件发布
StateMachineWorkflowEngine（领域层）
  ↓  纯 FSM 逻辑，无副作用
WorkflowPO（基础设施层）
  ↓  JSON 配置存储
ticket_flow_record（流水表）
  ↓  完整审计追踪
```

### 2.2 角色解析策略（双重判断）

```java
String resolveUserRole(Long operatorId, TicketPO ticket) {
    // 1. 查系统角色表（最高优先级）
    List<String> systemRoles = sysUserMapper.selectRoleCodesByUserId(operatorId);
    if (systemRoles.contains("ADMIN")) return "ADMIN";
    if (systemRoles.contains("TICKET_ADMIN")) return "TICKET_ADMIN";

    // 2. 工单内身份判断
    if (operatorId.equals(ticket.getAssigneeId())) return "HANDLER";
    if (operatorId.equals(ticket.getCreatorId())) return "SUBMITTER";

    return "SUBMITTER";
}
```

### 2.3 流转触发两种模式

```
模式1（推荐）：transitionId 精确触发
  前端从 /ticket/{id}/available-actions 获取 transitionId
  → POST /ticket/transit/{id} { transitionId: "t01" }

模式2（兼容）：targetStatus 触发
  → POST /ticket/transit/{id} { targetStatus: "processing" }
```

### 2.4 分派策略矩阵

| 策略 | 实现 | 复杂度 |
|---|---|---|
| MANUAL | 不触发自动分派 | O(1) |
| CATEGORY_DEFAULT | 取分类默认组第一人 | O(1) |
| ROUND_ROBIN | Redis 计数器轮询 | O(1) |
| LOAD_BALANCE | 批量查 GROUP BY（无 N+1）| O(1) |
| MATRIX | skill_match_config JSON 字段匹配 | O(rules) |

---

## 三、数据库设计规范

### 3.1 工作流 JSON 字段规范（states/transitions）

```json
// states 数组
{"code": "pending_accept", "name": "待受理", "type": "INITIAL", "slaAction": "START_RESPONSE", "order": 1}

// transitions 数组
{"id": "t01", "from": "pending_accept", "to": "processing", "name": "受理",
 "allowedRoles": ["HANDLER","ADMIN"], "requireRemark": false, "allowTransfer": false, "isReturn": false}
```

### 3.2 状态码规范

- **统一小写下划线**：`pending_accept`、`processing`、`completed`（不使用 UPPER_CASE）
- 与 `TicketStatus` 枚举的 `code` 字段保持一致
- `fromCode()` 方法支持历史大写别名兼容（`PENDING` → `pending_accept`）

### 3.3 ticket_flow_record 流水表（新增）

```sql
-- 每次流转都写入，供审计/分析使用
flow_type: TRANSIT / TRANSFER / RETURN / ASSIGN / CLOSE
transition_id: 触发的流转规则ID
from_status / to_status: 流转前后状态
from_assignee_id / to_assignee_id: 处理人变更
operator_role: 操作时的角色（SUBMITTER/HANDLER/ADMIN/TICKET_ADMIN）
```

---

## 四、前端实现规范

### 4.1 动态操作按钮（核心原则）

```typescript
// ❌ 禁止硬编码
<el-option label="处理中" value="processing" />
<el-option label="已完成" value="resolved" />   // 错误：应为 completed

// ✅ 正确：从接口动态获取
const availableActions = await getAvailableActions(ticketId)
// 渲染 availableActions.actions 列表
```

### 4.2 状态颜色映射规则

```typescript
// 终态（完成/关闭/驳回）→ success（绿色）
// 待处理类（待分派/待受理/待验收等）→ warning（橙色）
// 进行中类（处理中/测试中/开发中）→ primary（蓝色）
// 挂起 → danger（红色）
// 其他 → info（灰色）
```

### 4.3 状态筛选下拉框完整列表

通用工单：`pending_assign`、`pending_accept`、`processing`、`suspended`、`pending_verify`、`completed`、`closed`

缺陷工单：以上 + `pending_test_accept`、`testing`、`pending_dev_accept`、`developing`、`pending_cs_confirm`

---

## 五、常见问题避坑

| 问题 | 原因 | 解决 |
|---|---|---|
| 状态码大小写不一致 | 工作流 JSON 用大写，枚举用小写 | 统一改为小写，FSM 引擎 normalize() 处理 |
| closeTicket 绕过工作流 | 直接 setStatus("CLOSED") | 改为通过 transit() 方法路由 |
| 负载均衡 N+1 查询 | 循环内 selectCount | 改为 GROUP BY 批量查询 |
| 管理员无法转派 | 只判断 assigneeId | 查系统角色表，ADMIN/TICKET_ADMIN 可转派 |
| 退回逻辑硬编码 | RETURN_STATUS_MAP | 改为从工作流 isReturn=true 的流转驱动 |
| 前端用 "resolved" | 后端用 "completed" | 统一改为 "completed" |

---

## 六、API 接口清单

| 接口编号 | 方法 | 路径 | 说明 |
|---|---|---|---|
| API000014 | GET | `/api/ticket/{id}/available-actions` | 获取可用操作列表 |
| API000015 | PUT | `/api/ticket/transit/{id}` | 执行状态流转 |
| API000016 | PUT | `/api/ticket/transfer/{id}` | 转派工单 |
| API000017 | PUT | `/api/ticket/return/{id}` | 退回上一节点 |
| API000018 | GET | `/api/ticket/{id}/flow-history` | 查询流转历史 |
