# Task009 Bug简报管理 API 接口设计

> 版本：v1.0  
> 日期：2026-03-02  
> 对应产品文档：4.12 Bug简报管理

---

## 1. 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 | 说明 |
|---|---|---|---|---|
| API000020 | Bug简报分页 | GET | /api/bug-report/page | 分页查询简报列表 |
| API000021 | Bug简报详情 | GET | /api/bug-report/detail/{id} | 查询简报详情（含关联工单、责任人、日志） |
| API000022 | Bug简报创建 | POST | /api/bug-report/create | 创建简报草稿 |
| API000023 | Bug简报更新 | PUT | /api/bug-report/update/{id} | 编辑草稿/退回简报 |
| API000024 | 提交审核 | PUT | /api/bug-report/submit/{id} | 从待填写/已退回提交流程 |
| API000025 | 审核通过 | PUT | /api/bug-report/approve/{id} | 审核通过后归档 |
| API000026 | 审核驳回 | PUT | /api/bug-report/reject/{id} | 审核不通过退回责任人 |
| API000027 | 作废简报 | PUT | /api/bug-report/void/{id} | 作废未归档简报 |
| API000028 | 简报统计看板 | GET | /api/bug-report/statistics | 归因分布、分类分布、项目TOP、责任人统计、及时率 |
| API000029 | 逻辑归因字典 | GET | /api/dict/logic-cause | 二级归因树 |
| API000030 | 缺陷分类字典 | GET | /api/dict/defect-category | 缺陷分类下拉数据 |

---

## 2. 状态流转

`DRAFT(待填写)` → `PENDING_REVIEW(待审核)` → `ARCHIVED(已归档)`  
`PENDING_REVIEW(待审核)` → `REJECTED(已退回)` → `PENDING_REVIEW(待审核)`  
`DRAFT/REJECTED/PENDING_REVIEW` → `VOIDED(已作废)`

---

## 3. 关键业务规则

1. 工单关闭触发 `TicketCompletedEvent`，若识别为缺陷工单则自动创建简报草稿。  
2. 创建/更新支持多工单关联，并可按缺陷工单信息自动预填简报字段。  
3. 提交审核时通知审核人；审核结果通知责任人；超期按 `bug_report_remind_days` 每日催促。  
4. 统计默认排除 `VOIDED` 状态，及时率计算规则：`submitted_at <= create_time + remind_days`。  
5. **解决信息字段（与缺陷工单状态联动）**  
   - 数据库字段 `bug_report.resolve_time`（DATETIME）：工单「处理完成」场景下简报必填的**解决时间**（含时分秒）。  
   - 工单处于 **临时解决**（`temp_resolved`）时：简报填写 `temp_resolve_date`、`temp_solution`、`resolve_date`、`solution`（四者必填）；更新时可传 `clearResolveTime=true` 清空 `resolve_time`。  
   - 关联工单**全部**为 **已完成/已关闭**（`completed` / `closed`）时：简报仅填写 `resolve_time`；更新时可传 `clearThoroughAndTempResolution=true` 清空上述四个字段。  
