# Task002：数据库设计与初始化

> **业务模块**：工单系统  
> **依赖**：Task001  
> **预估工时**：2天  
> **对应产品文档**：附录 A 数据库核心表结构、技术分析文档 4.x

---

## 一、任务目标

完成工单系统核心数据表 DDL 设计，建立 Flyway 迁移脚本，实现数据库初始化与版本化管理。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | Flyway 配置 | application-dev.yml 中 flyway 配置 |
| 2 | V1__init_base.sql | 用户、部门、组织架构表 |
| 3 | V2__init_ticket_core.sql | ticket、ticket_category、ticket_template、ticket_custom_field、ticket_comment、ticket_log、ticket_attachment、ticket_follower |
| 4 | V3__init_workflow_sla.sql | workflow、sla_policy、handler_group、handler_group_member、dispatch_rule、sla_timer |
| 5 | V4__init_time_track.sql | ticket_time_track、ticket_node_duration |
| 6 | V5__init_bug_ticket.sql | ticket_bug_info、ticket_bug_test_info、ticket_bug_dev_info |
| 7 | V6__init_bug_report.sql | bug_report、bug_report_responsible、bug_report_ticket、bug_report_log、bug_report_attachment、dict_logic_cause、dict_defect_category、dict_project |
| 8 | V7__init_wecom_notification.sql | wecom_group_binding、wecom_bot_message_log、notification、notification_preference、system_config |
| 9 | 索引设计 | 按技术分析文档 4.1.3 创建覆盖索引 |

## 三、设计要点

- **基础字段**：所有业务表含 id、create_time、update_time、create_by、update_by、deleted  
- **id 规则**：从 1 开始自增  
- **JSON 字段**：ticket_template.fields_config、workflow.states/transitions 需约定 Schema  
- **索引**：idx_ticket_assignee_status、idx_ticket_creator_status、idx_ticket_created_at 等  

## 四、验收标准

- [ ] Flyway 迁移全部成功执行  
- [ ] 表结构与产品方案附录 A 及技术分析文档 4.1 一致  
- [ ] 基础字段、索引、约束正确  
- [ ] 可重复执行（幂等）  

## 五、产出说明

数据库表就绪后，Task003 用户体系与 Task004 工单管理可开始实体映射与仓储实现。
