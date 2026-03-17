# 技术缺陷管理规范优化（V2.2 对齐）研发排期确认版 PRD

> 文档版本：v1.0（确认版待评审）  
> 日期：2026-03-16  
> 适用项目：`ticket-platform` + `miduo-frontend`  
> 关联规范：《技术缺陷管理办法（V2.2）》  
> 关联任务：task001~task016（本次规划）

---

## 0. 关键修正说明（已确认）

### 0.1 已确认修正
1. **溯源报告不在系统中提交正文**。  
2. 系统只做“**外部溯源流程留痕与关单校验**”：记录外部报告编号、链接、完成时间、确认状态。  
3. P1/P0 缺陷关闭前，必须校验外部溯源凭证已完成（规则可配置）。

### 0.2 本期目标
- 将 V2.2 规范中“分级、状态、时效、责任、闭环”落地为系统能力。
- 输出可直接进入研发排期与开发执行的任务清单。
- 为“确认后开发”提供统一基线文档。

---

## 1. 背景与问题定义

当前系统已具备工单流转、SLA策略管理、Bug简报、统计看板等能力，但与规范存在以下核心差距：

1. 分级口径并存（P0-P4 与 FATAL/CRITICAL/NORMAL/MINOR）。
2. 生命周期状态与规范不完全一致（缺“排查中”“临时解决”标准状态）。
3. SLA策略已存在，但与工单主流程联动不完整。
4. “唯一责任人”原则未全域落地（简报存在多责任人）。
5. 缺少外部溯源凭证留痕、技术债务、改进任务闭环的系统化约束。
6. 缺陷治理统计可视化存在，但月报闭环与改进跟踪不足。

---

## 2. 建设范围

## 2.1 In Scope（本期范围）
- 严重级别统一（P0-P4）
- 缺陷状态机对齐（含排查中/临时解决）
- SLA全链路联动（start/pause/resume/complete）
- 唯一责任人模型（owner + participants）
- 时限承诺变更审计
- 技术债务单模块
- 外部溯源凭证留痕与关单校验（不含正文）
- 改进任务闭环
- 质量月报与趋势报表

## 2.2 Out of Scope（本期不做）
- 系统内溯源报告正文编辑、审核流
- TAPD/Jira 双向同步
- AI 自动根因分析

---

## 3. 成功指标（上线后 2 个自然月观察）

| 指标 | 目标值 |
|---|---|
| 分级口径一致率 | 100% |
| SLA 自动计时覆盖率 | >=95% |
| P1/P0 关单外部溯源凭证完整率 | 100% |
| 临时解决缺陷债务单绑定率 | 100% |
| 改进任务按期完成率 | >=85% |
| 重复根因缺陷率（3个月） | 下降 >=20% |

---

## 4. 里程碑与排期建议

| 迭代 | 周期建议 | 任务范围 | 核心交付 |
|---|---:|---|---|
| Iteration A | 2周 | task001~task005 | 口径统一、状态对齐、规则阻断 |
| Iteration B | 2周 | task006~task009 | SLA联动、责任模型、时限审计 |
| Iteration C | 2周 | task010~task013 | 债务单、外部溯源留痕、改进闭环、月报 |
| Iteration D | 1~1.5周 | task014~task016 | 提醒、灰度、UAT、发布收口 |

---

## 5. task001~task016 研发任务拆解（开发子任务级）

## task001 现状基线冻结
**目标**：形成统一口径，不再反复变更规则。  
**依赖**：无

### 后端子任务
- 盘点现有枚举/状态/SLA逻辑：
  - `ticket-common/.../SeverityLevel.java`
  - `ticket-application/.../workflow/TicketWorkflowAppService.java`
  - `ticket-application/.../sla/SlaTimerService.java`

### 前端子任务
- 盘点页面口径冲突：
  - `src/views/ticket/TicketDetailView.vue`
  - `src/views/bugreport/BugReportEditView.vue`
  - `src/views/bugreport/BugReportDetailView.vue`

### 交付物
- 《缺陷治理字段与状态字典》
- 《规则冻结清单 v1.0》

### 验收
- 评审通过（产品/测试/研发）并作为后续开发基线。

---

## task002 数据库迁移：分级与状态统一
**目标**：完成底层口径统一与历史映射。  
**依赖**：task001

### DB 子任务
- 新增迁移脚本：`V17__defect_standard_alignment.sql`
  - `ticket_bug_test_info.severity_level` 映射到 P0~P4
  - 工作流状态补齐：`investigating`、`temp_resolved`
  - 历史数据映射与兼容处理

### 后端子任务
- 兼容读写策略：旧值读入映射、新值统一输出

### 验收
- 历史数据抽样核对通过（至少近3个月数据）。

---

## task003 后端枚举与DTO统一
**目标**：接口层完全统一 P0-P4 与规范状态。  
**依赖**：task002

### 后端子任务
- 统一枚举与校验：
  - `SeverityLevel`、相关 DTO/VO/校验注解
- `TicketBugApplicationService` 入参校验与映射逻辑更新

### API 影响
- 路径不变，入参/出参口径收敛

### 验收
- 非法等级请求返回参数错误；
- 所有缺陷详情/列表返回一致等级口径。

---

## task004 前端等级与状态统一展示
**目标**：UI 文案、颜色、排序一致。  
**依赖**：task003

### 前端子任务
- 更新缺陷等级选项、标签与说明文案
- 更新状态映射显示（新增排查中/临时解决）
- 类型定义同步：
  - `src/types/ticket.ts`
  - `src/types/bugreport.ts`

### 验收
- 列表页/详情页/编辑页无双口径字段展示。

---

## task005 状态机对齐 V2.2
**目标**：流程规则与规范一致，非法流转可阻断。  
**依赖**：task003

### 后端子任务
- 工作流迁移与规则：
  - 增加 `investigating -> processing`
  - 增加 `processing -> temp_resolved/suspended/closed`
- 增加必填校验：
  - 挂起理由
  - 临时方案
  - 客户同意标记

### 前端子任务
- 动作弹窗增加必填项
- 动作按钮按状态与角色动态渲染

### 验收
- 非法流转被拒绝；
- 合法流转可执行并有审计记录。

---

## task006 SLA 生命周期联动接入
**目标**：SLA 自动计时，不依赖人工。  
**依赖**：task005

### 后端子任务
- 在创建/流转/关单链路接入：
  - `startTimers`
  - `pauseTimers`
  - `resumeTimers`
  - `completeAllTimers`

### 涉及类
- `TicketApplicationService`
- `TicketWorkflowAppService`
- `SlaTimerService`

### 验收
- 关键状态切换后计时状态正确变化。

---

## task007 SLA 预警升级（criticalPct 生效）
**目标**：实现 warning/critical/breached 三段预警。  
**依赖**：task006

### 后端子任务
- `criticalPct` 纳入预警逻辑
- 预警事件消息内容分级

### 前端子任务
- 工单详情展示预警等级与剩余时长
- 报表展示预警分布

### 验收
- 阈值边界场景测试通过（75%、90%、超时）。

---

## task008 唯一责任人模型落地
**目标**：满足“凡事只有一个责任人”。  
**依赖**：task003

### DB 子任务
- 新增 `owner_id`（ticket、bug_report）
- 参与人关系表（或复用改造）

### 后端子任务
- create/update 接口增加 owner 校验
- 责任变更记录审计

### 前端子任务
- 表单改为 owner 单选 + participants 多选

### 验收
- 任意缺陷均可唯一定位 owner。

---

## task009 处理时限承诺与变更审计
**目标**：时限可追踪、可解释。  
**依赖**：task008

### DB 子任务
- 新表：`ticket_due_commitment_history`

### 后端子任务
- 新增改期接口与历史查询接口
- 改期必须填写原因

### 前端子任务
- 预计结束时间编辑弹窗
- 历史时间线展示

### 验收
- 每次改期可追溯“谁/何时/为何改”。

---

## task010 技术债务单模块
**目标**：临时解决可治理，避免长期悬挂。  
**依赖**：task005

### DB 子任务
- 新表：`tech_debt_ticket`

### 后端子任务
- 债务单 CRUD
- `temp_resolved` 进入时强制绑定债务单

### 前端子任务
- 债务单列表/详情/编辑

### 验收
- 无债务单时禁止提交“临时解决”状态。

---

## task011 外部溯源流程留痕（修正版）
**目标**：登记外部溯源凭证并作为关单前置校验。  
**依赖**：task005

### DB 子任务
- `ticket` 增加字段（或单独表）：
  - `external_report_no`
  - `external_report_url`
  - `external_report_at`
  - `external_report_status`

### 后端子任务
- 新增凭证登记接口
- P1/P0 关闭校验（status 必须 CONFIRMED）

### 前端子任务
- 工单详情新增“外部溯源凭证”区块
- 仅登记凭证，不提供正文编辑

### 验收
- P1/P0 无凭证禁止关闭；有凭证可关闭。

---

## task012 改进任务闭环
**目标**：让改进措施可执行、可验收、可复盘。  
**依赖**：task011

### DB 子任务
- 新表：`defect_improvement_task`

### 后端子任务
- 改进任务 CRUD 与状态流转
- 重大缺陷最少1条改进任务校验

### 前端子任务
- 改进任务看板

### 验收
- 改进任务可完整追踪执行与验收结果。

---

## task013 质量月报自动汇总
**目标**：形成数据驱动治理闭环。  
**依赖**：task007、task012

### 后端子任务
- 月报聚合接口（数量、级别、模块、时效、超时、重复根因）
- 趋势接口与导出

### 前端子任务
- 月报页面
- 导出按钮（CSV/Excel）

### 验收
- 月报数据可与明细对账，导出可用。

---

## task014 定时提醒与预警任务
**目标**：逾期与到期事项自动通知。  
**依赖**：task009、task010、task012

### 后端子任务
- 新增/扩展 job：
  - SLA临近超时提醒
  - 债务单到期提醒
  - 改进任务逾期提醒

### 验收
- 提醒触发准确，重复提醒可控（去重）。

---

## task015 历史数据回填与灰度发布
**目标**：低风险切换到新规范。  
**依赖**：task002~task014

### 后端子任务
- 灰度开关（按团队/角色）
- 历史数据回填脚本

### 验收
- 灰度可控；回填后关键报表口径一致。

---

## task016 联调、UAT、发布收口
**目标**：确认可上线并可回滚。  
**依赖**：全部前序任务

### 子任务
- 全链路联调
- UAT执行与缺陷清零
- 发布检查清单与回滚预案

### 验收
- UAT主场景通过；
- 发布前检查项全部通过。

---

## 6. 接口清单（草案，待确认后正式编号）

> 说明：以下为“拟新增接口”，用于研发评审。  
> 正式开发时需按接口编号规范在 `miduo-md/workflow/功能接口对应关系.md` 登记正式 API 编号。

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 时限承诺 | PUT | `/api/ticket/due/update/{id}` | 更新预计结束时间与原因 |
| 时限承诺 | GET | `/api/ticket/due/history/{id}` | 查询改期历史 |
| 技术债务 | POST | `/api/tech-debt/create` | 创建债务单 |
| 技术债务 | PUT | `/api/tech-debt/update/{id}` | 更新债务单 |
| 技术债务 | GET | `/api/tech-debt/page` | 债务单分页 |
| 外部溯源凭证 | PUT | `/api/ticket/external-report/update/{id}` | 登记外部报告凭证 |
| 外部溯源凭证 | GET | `/api/ticket/external-report/detail/{id}` | 查询外部报告凭证 |
| 改进任务 | POST | `/api/improvement-task/create` | 创建改进项 |
| 改进任务 | PUT | `/api/improvement-task/update/{id}` | 更新改进项 |
| 改进任务 | GET | `/api/improvement-task/page` | 改进项分页 |
| 质量月报 | GET | `/api/quality-report/monthly` | 月报聚合 |
| 趋势报表 | GET | `/api/quality-report/trend` | 趋势数据 |

---

## 7. 数据库 DDL 草案（待评审）

## 7.1 `tech_debt_ticket`（技术债务单）
```sql
CREATE TABLE `tech_debt_ticket` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint(20) NOT NULL COMMENT '关联缺陷工单ID',
  `debt_type` varchar(50) NOT NULL COMMENT '债务类型',
  `risk_level` varchar(20) NOT NULL COMMENT '风险等级',
  `debt_desc` text DEFAULT NULL COMMENT '债务描述',
  `repay_plan` text DEFAULT NULL COMMENT '偿还计划',
  `owner_id` bigint(20) NOT NULL COMMENT '责任人ID',
  `due_date` datetime DEFAULT NULL COMMENT '偿还截止时间',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技术债务单';
```

## 7.2 `defect_improvement_task`（改进任务）
```sql
CREATE TABLE `defect_improvement_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint(20) NOT NULL COMMENT '关联缺陷工单ID',
  `measure_desc` text NOT NULL COMMENT '改进措施',
  `owner_id` bigint(20) NOT NULL COMMENT '责任人ID',
  `due_date` datetime DEFAULT NULL COMMENT '截止时间',
  `acceptance_criteria` text DEFAULT NULL COMMENT '验收标准',
  `acceptance_result` text DEFAULT NULL COMMENT '验收结果',
  `status` varchar(20) NOT NULL DEFAULT 'TODO' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷改进任务';
```

## 7.3 外部溯源凭证字段（建议加在 `ticket`）
```sql
ALTER TABLE `ticket`
  ADD COLUMN `external_report_no` varchar(100) DEFAULT NULL COMMENT '外部溯源报告编号',
  ADD COLUMN `external_report_url` varchar(500) DEFAULT NULL COMMENT '外部溯源报告链接',
  ADD COLUMN `external_report_at` datetime DEFAULT NULL COMMENT '外部溯源完成时间',
  ADD COLUMN `external_report_status` varchar(20) DEFAULT NULL COMMENT '外部溯源状态(PENDING/CONFIRMED)';
```

## 7.4 时限变更历史表
```sql
CREATE TABLE `ticket_due_commitment_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint(20) NOT NULL COMMENT '工单ID',
  `old_due_time` datetime DEFAULT NULL COMMENT '原预计结束时间',
  `new_due_time` datetime DEFAULT NULL COMMENT '新预计结束时间',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '变更原因',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单时限承诺变更历史';
```

---

## 8. 测试用例清单（执行模板）

## 8.1 核心用例编号

| 用例ID | 场景 | 预期 |
|---|---|---|
| TC-WF-001 | 非法状态流转 | 被系统阻断并提示原因 |
| TC-WF-002 | 合法状态流转 | 流转成功并写入历史 |
| TC-SLA-001 | 创建后SLA启动 | response/resolve计时器启动 |
| TC-SLA-002 | 挂起与恢复 | 计时暂停与恢复正确 |
| TC-SLA-003 | 关闭后计时结束 | 计时器状态完成 |
| TC-OWNER-001 | 责任人唯一性 | 仅允许唯一owner |
| TC-DUE-001 | 改期必填原因 | 无原因不可提交 |
| TC-DEBT-001 | 临时解决无债务单 | 不允许提交 |
| TC-EXT-001 | P1/P0无外部凭证关单 | 不允许关闭 |
| TC-EXT-002 | P1/P0有外部凭证关单 | 允许关闭 |
| TC-IMP-001 | 重大缺陷改进项校验 | 至少1条改进项 |
| TC-REPORT-001 | 月报与明细对账 | 数据一致 |

## 8.2 用例模板
```markdown
### [用例ID] [用例名称]
- 前置条件：
- 测试数据（真实数据）：
- 操作步骤：
  1.
  2.
  3.
- 预期结果：
  1.
  2.
  3.
- 实际结果：
- 结论：通过 / 不通过
```

---

## 9. 评审确认清单（确认后进入开发）

### 9.1 规则确认
- [ ] 分级口径最终版（P0-P4）确认
- [ ] 状态机最终版确认（含排查中/临时解决）
- [ ] P1/P0 外部溯源凭证关单规则确认

### 9.2 技术确认
- [ ] DDL 草案确认
- [ ] API 草案确认
- [ ] 历史数据映射策略确认
- [ ] 灰度与回滚策略确认

### 9.3 组织协同确认
- [ ] 产品、测试、研发三方评审通过
- [ ] 月报口径与质量会议模板确认
- [ ] 上线窗口与值班机制确认

---

## 10. 下一步动作

确认通过后按如下顺序执行：
1. 锁定 task001~task016 负责人与迭代归属  
2. 先落地 task001~task005（规则底座）  
3. 每个 task 完成后更新对应 workflow 文档与联调记录  
4. 进入 UAT 前完成一次全链路演练（P1 场景 + 临时解决场景）

---

## 11. 备注

- 本文档为“确认后开发”版本，不代表接口与数据结构已上线。  
- 正式开发时，新增 API 必须按接口编号规范登记并同步更新：
  - `miduo-md/workflow/功能接口对应关系.md`
  - `miduo-md/workflow/工单系统/功能接口对应关系.md`

