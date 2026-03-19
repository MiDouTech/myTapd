# 技术缺陷管理规范优化（V2.2 对齐）开发启动包

> 文档版本：v1.0  
> 日期：2026-03-16  
> 前置文档：`task019-技术缺陷管理规范优化-研发排期确认版PRD.md`  
> 用途：评审通过后，研发/测试按本文档直接开工执行

---

## 0. 启动前共识（必须确认）

1. **外部溯源流程约束已确认**：系统仅记录外部溯源凭证，不提交正文。  
2. **本次编号策略**：先按“接口草案”开发，评审通过后再登记正式 API 编号。  
3. **灰度发布策略**：按团队灰度，不做全量一次切换。  
4. **真实数据验证**：联调/UAT 禁止 mock。

---

## 1. 环境启动与分支规范

## 1.1 后端本地启动（JDK8）
```bash
cd /workspace/ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn clean install -DskipTests
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn spring-boot:run -pl ticket-bootstrap -Dspring-boot.run.profiles=dev -Djdk.tls.client.protocols=TLSv1.2
```

## 1.2 前端本地启动
```bash
cd /workspace/miduo-frontend
npm install
npm run dev
```

## 1.3 代码分支建议
- 主开发分支：`cursor/prd-79e8`（当前）
- 每个 task 合并前至少包含：
  - 代码
  - 对应文档更新
  - 自测记录

---

## 2. 模块责任分工（建议）

| 角色 | 主要职责 |
|---|---|
| 后端负责人 | DB迁移、状态机、SLA、规则校验、聚合报表 |
| 前端负责人 | 页面改造、表单校验、报表展示、交互一致性 |
| 测试负责人 | 用例编写与执行、回归、UAT 报告、发布回归 |
| 产品/测试组长 | 规则确认、争议裁定口径、月报口径确认 |

---

## 3. task001~task016 开发执行清单（文件级）

> 说明：以下清单按“修改现有文件 / 新增文件”列出，便于直接派工。

## task001 基线冻结
### 修改文档
- `miduo-md/workflow/@缺陷管理/task019-技术缺陷管理规范优化-研发排期确认版PRD.md`

### 输出
- 《字段字典》
- 《状态字典》
- 《SLA口径字典》

---

## task002 DB迁移：分级与状态统一
### 新增文件
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V17__defect_standard_alignment.sql`

### 修改文件
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V16__workflow_engine_refactor.sql`（如通过新增迁移覆盖可不改原文件）

### SQL关注点
- `ticket_bug_test_info.severity_level` 历史映射到 P0~P4
- 新增状态：`investigating`、`temp_resolved`

---

## task003 后端枚举与DTO统一
### 修改文件（后端）
- `ticket-platform/ticket-common/src/main/java/com/miduo/cloud/ticket/common/enums/SeverityLevel.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketBugTestInfoInput.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketBugTestInfoOutput.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketBugApplicationService.java`

### 新增建议（如需兼容层）
- `ticket-platform/ticket-common/src/main/java/com/miduo/cloud/ticket/common/enums/SeverityLegacyMappingEnum.java`

---

## task004 前端分级与状态展示统一
### 修改文件（前端）
- `miduo-frontend/src/views/ticket/TicketDetailView.vue`
- `miduo-frontend/src/views/bugreport/BugReportEditView.vue`
- `miduo-frontend/src/views/bugreport/BugReportDetailView.vue`
- `miduo-frontend/src/types/ticket.ts`
- `miduo-frontend/src/types/bugreport.ts`

---

## task005 状态机对齐 V2.2
### 修改文件（后端）
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/workflow/TicketWorkflowAppService.java`
- `ticket-platform/ticket-domain/src/main/java/com/miduo/cloud/ticket/domain/workflow/service/StateMachineWorkflowEngine.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/workflow/TransitInput.java`

### 修改文件（前端）
- `miduo-frontend/src/views/ticket/TicketDetailView.vue`
- `miduo-frontend/src/types/workflow.ts`

### 新增建议（后端）
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/workflow/DefectWorkflowGuardService.java`

---

## task006 SLA链路接入
### 修改文件（后端）
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketApplicationService.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/workflow/TicketWorkflowAppService.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/sla/SlaTimerService.java`

---

## task007 SLA预警升级
### 修改文件（后端）
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/sla/SlaTimerService.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/notification/SlaEventListener.java`

### 修改文件（前端）
- `miduo-frontend/src/views/ticket/components/bug/BugDetailInfoPanel.vue`
- `miduo-frontend/src/views/report/ReportView.vue`
- `miduo-frontend/src/types/sla.ts`（如增加展示字段）

---

## task008 唯一责任人模型
### 新增文件（DB迁移）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V18__owner_model.sql`

### 修改文件（后端）
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/ticket/po/TicketPO.java`
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/bugreport/po/BugReportPO.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketApplicationService.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/bugreport/BugReportApplicationService.java`

### 修改文件（前端）
- `miduo-frontend/src/views/bugreport/BugReportEditView.vue`
- `miduo-frontend/src/types/bugreport.ts`

---

## task009 时限承诺与变更审计
### 新增文件（DB迁移）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V19__ticket_due_commitment_history.sql`

### 新增文件（后端建议）
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketDueUpdateInput.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketDueHistoryOutput.java`
- `ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/ticket/TicketDueController.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketDueApplicationService.java`
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/ticket/po/TicketDueCommitmentHistoryPO.java`
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/ticket/mapper/TicketDueCommitmentHistoryMapper.java`

### 新增文件（前端建议）
- `miduo-frontend/src/api/ticketDue.ts`
- `miduo-frontend/src/types/ticketDue.ts`

---

## task010 技术债务单模块
### 新增文件（DB迁移）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V20__tech_debt_ticket.sql`

### 新增文件（后端建议）
- `ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/debt/TechDebtController.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/debt/TechDebtApplicationService.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/debt/`（创建/更新/分页/详情 DTO）
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/debt/po/TechDebtTicketPO.java`
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/debt/mapper/TechDebtTicketMapper.java`

### 新增文件（前端建议）
- `miduo-frontend/src/api/debt.ts`
- `miduo-frontend/src/types/debt.ts`
- `miduo-frontend/src/views/debt/TechDebtListView.vue`
- `miduo-frontend/src/views/debt/TechDebtDetailView.vue`

---

## task011 外部溯源凭证留痕（修正版）
### 新增文件（DB迁移）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V21__external_postmortem_trace.sql`

### 新增文件（后端建议）
- `ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/ticket/TicketExternalReportController.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/ticket/TicketExternalReportApplicationService.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketExternalReportUpdateInput.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/ticket/TicketExternalReportOutput.java`

### 修改文件（后端）
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/workflow/TicketWorkflowAppService.java`（P1/P0关单校验）

### 修改文件（前端）
- `miduo-frontend/src/views/ticket/TicketDetailView.vue`
- `miduo-frontend/src/types/ticket.ts`
- `miduo-frontend/src/api/ticket.ts`（新增外部溯源凭证接口）

---

## task012 改进任务闭环
### 新增文件（DB迁移）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V22__defect_improvement_task.sql`

### 新增文件（后端建议）
- `ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/improvement/ImprovementTaskController.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/improvement/ImprovementTaskApplicationService.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/improvement/`（DTO）
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/improvement/po/DefectImprovementTaskPO.java`
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/improvement/mapper/DefectImprovementTaskMapper.java`

### 新增文件（前端建议）
- `miduo-frontend/src/api/improvement.ts`
- `miduo-frontend/src/types/improvement.ts`
- `miduo-frontend/src/views/improvement/ImprovementTaskBoardView.vue`

---

## task013 质量月报自动汇总
### 新增文件（后端建议）
- `ticket-platform/ticket-controller/src/main/java/com/miduo/cloud/ticket/controller/report/QualityReportController.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/report/QualityReportApplicationService.java`
- `ticket-platform/ticket-entity/src/main/java/com/miduo/cloud/ticket/entity/dto/report/`（月报与趋势 DTO）
- `ticket-platform/ticket-infrastructure/src/main/java/com/miduo/cloud/ticket/infrastructure/persistence/mybatis/dashboard/mapper/QualityReportMapper.java`（可按现有 dashboard mapper 扩展）

### 新增文件（前端建议）
- `miduo-frontend/src/api/qualityReport.ts`
- `miduo-frontend/src/types/qualityReport.ts`
- `miduo-frontend/src/views/report/QualityMonthlyReportView.vue`

---

## task014 定时提醒与预警
### 新增文件（后端建议）
- `ticket-platform/ticket-job/src/main/java/com/miduo/cloud/ticket/job/handler/DefectReminderJobHandler.java`
- `ticket-platform/ticket-application/src/main/java/com/miduo/cloud/ticket/application/notification/DefectReminderService.java`

### 修改文件
- `ticket-platform/ticket-bootstrap/src/main/resources/application-dev.yml`（如需新增 cron 配置）

---

## task015 历史回填与灰度
### 新增文件（后端建议）
- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V23__defect_data_backfill.sql`
- `ticket-platform/ticket-common/src/main/java/com/miduo/cloud/ticket/common/constants/FeatureToggleConstants.java`

### 修改文件
- `ticket-platform/ticket-application/...`（接入 feature toggle 的关键路径）

---

## task016 联调、UAT、发布收口
### 输出文件建议
- `miduo-md/workflow/test/task0xx-缺陷规范优化联调记录.md`
- `miduo-md/workflow/test/task0xx-缺陷规范优化UAT报告.md`
- `miduo-md/workflow/test/task0xx-缺陷规范优化上线检查清单.md`

---

## 4. 接口契约样例（开发可直接对照）

## 4.1 时限承诺更新
`PUT /api/ticket/due/update/{id}`

请求：
```json
{
  "newDueTime": "2026-03-20 18:00:00",
  "changeReason": "客户新增验证范围，需延长1天"
}
```

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

## 4.2 外部溯源凭证登记
`PUT /api/ticket/external-report/update/{id}`

请求：
```json
{
  "externalReportNo": "POSTMORTEM-2026-0316-001",
  "externalReportUrl": "https://wiki.xxx.com/postmortem/POSTMORTEM-2026-0316-001",
  "externalReportAt": "2026-03-16 21:30:00",
  "externalReportStatus": "CONFIRMED"
}
```

## 4.3 技术债务单创建
`POST /api/tech-debt/create`

请求：
```json
{
  "ticketId": 12345,
  "debtType": "TEMP_WORKAROUND",
  "riskLevel": "HIGH",
  "debtDesc": "临时绕过支付签名校验",
  "repayPlan": "下个迭代完成统一签名中间件改造",
  "ownerId": 10086,
  "dueDate": "2026-03-30 18:00:00"
}
```

---

## 5. 后端开发规范补充（本次必遵守）

1. Controller 仅做参数接收和服务调用，不写业务私有方法。  
2. 所有 SQL 走 mapper/xml，禁止注解写 SQL。  
3. 关键流程统一判空，避免 NPE。  
4. 常量、状态、类型统一收敛到枚举/常量类。  
5. 数据库访问避免循环内单条查询，统一批量查。

---

## 6. 前端开发规范补充（本次必遵守）

1. 所有新增列表页使用标准分页：10/20/50/100，默认20。  
2. 表格头部背景色 `#f5f7fa`，与现有系统一致。  
3. 状态/级别颜色全局复用，不在页面散落硬编码。  
4. API 封装统一在 `src/api/*.ts`，类型统一在 `src/types/*.ts`。  
5. UI 文案与规范术语保持一致（排查中/处理中/临时解决/挂起/关闭）。

---

## 7. 联调顺序建议（减少返工）

1. 先联调 task002~task005（规则底座）  
2. 再联调 task006~task009（SLA+责任+时限）  
3. 然后联调 task010~task013（债务+凭证+改进+月报）  
4. 最后 task014~task016（提醒、灰度、UAT、上线）

---

## 8. 上线门禁（Go/No-Go）

### Go 条件
- [ ] P1/P0 外部溯源凭证关单校验生效
- [ ] 临时解决必须绑定债务单
- [ ] SLA 计时链路全量可用
- [ ] 月报数据与明细对账一致
- [ ] UAT 核心用例通过率 100%

### No-Go 条件
- [ ] 出现规则绕过（可关单但无凭证/无债务单）
- [ ] 数据映射导致严重级别错乱
- [ ] 关键接口无回归测试

---

## 9. 确认后立即执行清单（Kickoff Checklist）

1. 锁定每个 task 的负责人和起止时间  
2. 创建迁移脚本分支并先完成 task002  
3. 建立 API 草案评审会（后端+前端+测试）  
4. 建立周会机制：进度、风险、阻塞、决策  
5. 每个 task 完成后必须回填：
   - 代码清单
   - 自测结果
   - 联调记录

---

## 10. 备注

- 本启动包是“可直接执行版”，不替代详细设计。  
- 开发期间如规则有变更，必须同步回写 `task019` 与本 `task020` 文档，避免口径漂移。

