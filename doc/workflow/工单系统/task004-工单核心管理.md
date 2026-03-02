# Task004：工单核心管理（分类、创建、列表、详情）

> **业务模块**：工单系统  
> **依赖**：Task002、Task003  
> **预估工时**：5天  
> **对应产品文档**：4.2 工单管理、4.3 分类与模板管理

---

## 一、任务目标

实现工单分类管理、工单创建/列表/详情基础功能，支持多视图、筛选、排序，完成通用工单基础工作流（待受理→处理中→待验收→已完成）。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 分类 CRUD | 三级分类树、分类与模板/工作流/SLA 绑定 |
| 2 | 模板管理 | 模板定义、fields_config(JSON)、基础字段配置 |
| 3 | 工单创建 | 选择分类→加载模板→填写→提交，触发工作流 |
| 4 | 工单列表 | 我创建的/我待办的/我参与的/我关注的/所有工单 |
| 5 | 工单详情 | 基本信息、描述、自定义字段、附件、评论 |
| 6 | 筛选与排序 | 编号、标题、分类、状态、优先级、创建人、处理人、时间范围、SLA 状态 |
| 7 | 通用工作流 | 待受理→受理→处理中→挂起/恢复→待验收→已完成/已关闭 |
| 8 | 手动分派 | 指定处理人/处理组 |
| 9 | 基础操作 | 处理、转派、关闭、关注 |
| 10 | 前端页面 | 工单列表、工单详情、创建工单、分类管理 |

## 三、接口清单（已填接口编号）

| 接口编号 | 接口 | 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|------|------|
| API000001 | 分类树 | GET | /api/category/tree | 三级分类树 | ✅已完成 |
| API000002 | 分类详情 | GET | /api/category/detail/{id} | 分类详情 | ✅已完成 |
| API000003 | 分类创建 | POST | /api/category/create | 新增分类 | ✅已完成 |
| API000004 | 分类更新 | PUT | /api/category/update/{id} | 修改分类 | ✅已完成 |
| API000005 | 模板列表 | GET | /api/template/list | 按分类查模板 | ✅已完成 |
| API000006 | 工单创建 | POST | /api/ticket/create | 创建工单 | ✅已完成 |
| API000007 | 工单分页 | GET | /api/ticket/page | 分页列表 | ✅已完成 |
| API000008 | 工单详情 | GET | /api/ticket/detail/{id} | 工单详情 | ✅已完成 |
| API000009 | 工单分派 | PUT | /api/ticket/assign/{id} | 手动分派 | ✅已完成 |
| API000010 | 工单处理 | PUT | /api/ticket/process/{id} | 处理并流转 | ✅已完成 |
| API000011 | 工单关闭 | PUT | /api/ticket/close/{id} | 关闭工单 | ✅已完成 |
| API000012 | 关注工单 | POST | /api/ticket/follow/{id} | 关注工单 | ✅已完成 |
| API000013 | 取消关注 | DELETE | /api/ticket/follow/{id} | 取消关注 | ✅已完成 |

## 四、验收标准

- [x] 分类三级树正确展示与维护  
- [x] 工单创建后自动进入待受理，可手动分派  
- [x] 列表多视图、筛选、排序、分页正确  
- [x] 详情页展示完整信息，可执行处理、转派、关闭  

## 五、产出说明

工单核心能力就绪后，Task005 工作流引擎与 Task006 企微集成可在此基础上扩展。

## 六、后端实现清单（2026-03-02 完成）

### 基础设施层（ticket-infrastructure）

| 文件 | 说明 |
|------|------|
| BaseEntity.java | 数据库实体基类（id, createTime, updateTime, createBy, updateBy, deleted） |
| TicketCategoryPO.java | 工单分类PO |
| TicketTemplatePO.java | 工单模板PO |
| TicketPO.java | 工单主表PO（含乐观锁@Version） |
| TicketCommentPO.java | 评论/处理记录PO |
| TicketLogPO.java | 操作日志PO |
| TicketAttachmentPO.java | 附件PO |
| TicketFollowerPO.java | 关注人PO |
| TicketCustomFieldPO.java | 自定义字段PO |
| SysUserPO.java | 系统用户PO |
| WorkflowPO.java | 工作流定义PO |
| SlaPolicyPO.java | SLA策略PO |
| HandlerGroupPO.java | 处理组PO |
| TicketCategoryMapper.java | 分类Mapper |
| TicketTemplateMapper.java | 模板Mapper |
| TicketMapper.java | 工单Mapper（含自定义分页查询） |
| TicketCommentMapper.java | 评论Mapper |
| TicketLogMapper.java | 日志Mapper |
| TicketAttachmentMapper.java | 附件Mapper |
| TicketFollowerMapper.java | 关注人Mapper |
| TicketCustomFieldMapper.java | 自定义字段Mapper |
| SysUserMapper.java | 用户Mapper |
| WorkflowMapper.java | 工作流Mapper |
| SlaPolicyMapper.java | SLA策略Mapper |
| HandlerGroupMapper.java | 处理组Mapper |
| TicketMapper.xml | 工单分页查询SQL映射（多视图+筛选+排序） |

### DTO层（ticket-entity）

| 文件 | 说明 |
|------|------|
| CategoryTreeOutput.java | 分类树响应 |
| CategoryDetailOutput.java | 分类详情响应 |
| CategoryCreateInput.java | 新增分类请求 |
| CategoryUpdateInput.java | 修改分类请求 |
| TemplateListOutput.java | 模板列表响应 |
| TicketCreateInput.java | 创建工单请求 |
| TicketPageInput.java | 分页查询请求 |
| TicketListOutput.java | 列表项响应 |
| TicketDetailOutput.java | 工单详情响应（含附件、评论、日志） |
| TicketAssignInput.java | 分派请求 |
| TicketProcessInput.java | 处理流转请求 |
| TicketCloseInput.java | 关闭请求 |

### 枚举类（ticket-common）

| 文件 | 说明 |
|------|------|
| TicketAction.java | 工单操作类型枚举 |
| CommentType.java | 评论类型枚举 |
| TicketView.java | 工单列表视图枚举 |

### 应用服务层（ticket-application）

| 文件 | 说明 |
|------|------|
| CategoryApplicationService.java | 分类管理服务（CRUD、树构建、路径管理） |
| TemplateApplicationService.java | 模板管理服务（按分类查询） |
| TicketApplicationService.java | 工单核心服务（创建、分页、详情、分派、处理、关闭、关注） |
| WorkflowApplicationService.java | 工作流服务（状态流转校验、初始状态、终态判断） |

### 控制器层（ticket-controller）

| 文件 | 说明 |
|------|------|
| CategoryController.java | 分类管理API（API000001~API000004） |
| TemplateController.java | 模板管理API（API000005） |
| TicketController.java | 工单管理API（API000006~API000013） |

### 前端页面

前端页面（工单列表、工单详情、创建工单、分类管理）待前端项目搭建后实现，后端接口已预留完整支持。
