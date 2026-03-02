# Task005：工作流引擎与分派

> **业务模块**：工单系统  
> **依赖**：Task004  
> **预估工时**：4天  
> **对应产品文档**：4.4 工作流引擎、4.5 分派与路由

---

## 一、任务目标

实现自研工作流引擎（状态机模式），支持通用工单、审批工单、缺陷工单三种内置工作流；实现分类默认分派、轮询分派、负载均衡分派；完善流转校验与自动化动作。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 | 状态 |
|------|--------|-----------|------|
| 1 | WorkflowEngine 接口 | `ticket-domain/.../workflow/service/WorkflowEngine.java` + `StateMachineWorkflowEngine.java` | ✅ |
| 2 | 通用工作流 | V3 Flyway数据 id=1，完整状态图（待受理→处理中→挂起→待验收→已完成） | ✅ |
| 3 | 审批工作流 | V3 Flyway数据 id=2，提交→部门审批→执行中→完成 | ✅ |
| 4 | 缺陷工作流 | V3 Flyway数据 id=3，待分派→待测试受理→测试中→待开发受理→开发中→待验收→待客服确认→已完成 | ✅ |
| 5 | 流转规则 | JSON定义在workflow表transitions字段，支持角色权限校验 | ✅ |
| 6 | 分派策略 | `ticket-application/.../workflow/DispatchAppService.java`，分类默认/轮询/负载均衡 | ✅ |
| 7 | 处理组管理 | `ticket-controller/.../workflow/HandlerGroupController.java`，处理组列表+创建 | ✅ |
| 8 | 领域事件 | `TicketStatusChangedEvent` + `TicketAssignedEvent`，通过Spring事件发布 | ✅ |
| 9 | 转派/退回 | `ticket-controller/.../workflow/TicketWorkflowController.java`，同角色转派、退回上一节点 | ✅ |

## 三、接口清单

| 接口编号 | 接口 | 方法 | 路径 | 说明 |
|----------|------|------|------|------|
| API000012 | 工作流列表 | GET | /api/workflow/list | 工作流定义列表 |
| API000013 | 工作流详情 | GET | /api/workflow/detail/{id} | 工作流详情（含状态定义和流转规则） |
| API000014 | 可用操作 | GET | /api/ticket/{id}/available-actions | 当前工单可执行操作 |
| API000015 | 状态流转 | PUT | /api/ticket/transit/{id} | 执行状态流转 |
| API000016 | 转派 | PUT | /api/ticket/transfer/{id} | 同角色转派 |
| API000017 | 退回 | PUT | /api/ticket/return/{id} | 退回上一节点 |
| API000018 | 处理组列表 | GET | /api/handler-group/list | 处理组列表 |
| API000019 | 处理组创建 | POST | /api/handler-group/create | 新增处理组 |

## 四、验收标准

- [x] 三种工作流状态流转符合产品 4.4 定义  
- [x] 流转时权限校验、必填校验生效  
- [x] 分派策略正确执行（分类默认、轮询、负载均衡）  
- [x] 转派与退回逻辑正确，不改变错误状态  

## 五、产出说明

工作流与分派能力就绪后，缺陷工单、SLA、通知等可基于流转事件驱动。

## 六、代码产出清单

### 新增枚举类（ticket-common）
- `com.miduo.cloud.ticket.common.enums.DispatchStrategy` - 分派策略枚举
- `com.miduo.cloud.ticket.common.enums.WorkflowMode` - 工作流模式枚举

### 领域层（ticket-domain）
- `com.miduo.cloud.ticket.domain.workflow.model.WorkflowState` - 工作流状态值对象
- `com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition` - 工作流流转规则值对象
- `com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine` - 工作流引擎接口
- `com.miduo.cloud.ticket.domain.workflow.service.StateMachineWorkflowEngine` - 状态机引擎实现
- `com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent` - 工单分派事件

### PO持久化对象（ticket-infrastructure）
- `WorkflowPO` - 工作流定义
- `HandlerGroupPO` - 处理组
- `HandlerGroupMemberPO` - 处理组成员
- `DispatchRulePO` - 分派规则
- `TicketPO` - 工单主表（供Task004复用）
- `TicketLogPO` - 工单操作日志（供Task004复用）
- `TicketCategoryPO` - 工单分类（供Task004复用）
- `SysUserPO` - 系统用户（供Task003复用）

### Mapper接口（ticket-infrastructure）
- `WorkflowMapper`、`HandlerGroupMapper`、`HandlerGroupMemberMapper`、`DispatchRuleMapper`
- `TicketMapper`、`TicketLogMapper`、`TicketCategoryMapper`、`SysUserMapper`

### DTO请求/响应（ticket-entity）
- `WorkflowListOutput`、`WorkflowDetailOutput` - 工作流查询输出
- `AvailableActionOutput` - 可用操作输出
- `TransitInput`、`TransferInput`、`ReturnInput` - 流转/转派/退回请求
- `HandlerGroupListOutput`、`HandlerGroupCreateInput` - 处理组查询/创建

### 应用服务（ticket-application）
- `WorkflowAppService` - 工作流查询服务
- `TicketWorkflowAppService` - 工单流转/转派/退回服务
- `HandlerGroupAppService` - 处理组管理服务
- `DispatchAppService` - 分派策略执行服务
- `WorkflowConfig` - 工作流引擎Bean配置

### 接口层（ticket-controller）
- `WorkflowController` - 工作流管理接口（API000012-013）
- `TicketWorkflowController` - 工单工作流操作接口（API000014-017）
- `HandlerGroupController` - 处理组管理接口（API000018-019）
