# Task009：Bug 简报管理

> **业务模块**：工单系统  
> **依赖**：Task008  
> **预估工时**：5天  
> **对应产品文档**：4.12 Bug 简报管理

---

## 一、任务目标

实现 Bug 简报 CRUD、审核工作流、工单关联、自动预填、通知与超期催促、简报统计看板。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 简报主表与关联表 | bug_report、bug_report_responsible、bug_report_ticket、bug_report_log、bug_report_attachment |
| 2 | 字典表 | dict_logic_cause、dict_defect_category、dict_project |
| 3 | 简报 CRUD | 创建、编辑、提交审核、审核通过/不通过、作废 |
| 4 | 审核流程 | 待填写→待审核→已归档 / 已退回→待审核 |
| 5 | 工单关联 | 自动关联（缺陷工单关闭触发）、手动关联多个工单 |
| 6 | 自动预填 | 问题描述、开始/解决时间、反馈人、缺陷等级、解决方案、影响范围、责任人 |
| 7 | 工单关闭触发 | TicketCompletedEvent 监听，创建简报草稿、通知责任人 |
| 8 | 简报通知 | 提交审核→通知审核人；审核结果→通知责任人；超期催促 |
| 9 | 简报统计看板 | 逻辑归因分布、缺陷分类分布、引入项目 TOP、责任人统计、填写及时率 |
| 10 | 工单详情关联展示 | 缺陷工单详情页 Bug 简报 Tab |
| 11 | 前端页面 | 简报列表、简报详情、简报编辑、统计看板 |

## 三、接口清单（需填接口编号）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 简报分页 | GET | /api/bug-report/page | 分页列表 |
| 简报详情 | GET | /api/bug-report/detail/{id} | 简报详情 |
| 简报创建 | POST | /api/bug-report/create | 创建简报 |
| 简报更新 | PUT | /api/bug-report/update/{id} | 更新简报 |
| 提交审核 | PUT | /api/bug-report/submit/{id} | 提交审核 |
| 审核通过 | PUT | /api/bug-report/approve/{id} | 审核通过 |
| 审核不通过 | PUT | /api/bug-report/reject/{id} | 审核不通过 |
| 作废 | PUT | /api/bug-report/void/{id} | 作废简报 |
| 简报统计 | GET | /api/bug-report/statistics | 统计看板数据 |
| 逻辑归因字典 | GET | /api/dict/logic-cause | 逻辑归因树 |
| 缺陷分类字典 | GET | /api/dict/defect-category | 缺陷分类列表 |

## 四、验收标准

- [ ] 缺陷工单关闭后自动创建简报草稿并通知责任人  
- [ ] 自动预填字段正确从工单信息填充  
- [ ] 审核流程正确流转（通过/退回/作废）  
- [ ] 简报统计看板数据正确  

## 五、产出说明

Bug 简报完成后，形成缺陷从发现到归因总结的闭环，支撑质量改进决策。
