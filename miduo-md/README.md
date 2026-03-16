# 米多内部工单系统 — 文档总览索引

> **版本**：v2.0  
> **更新日期**：2026-03-15  
> **说明**：所有项目文档统一归档在本目录（`miduo-md/`），以后新增文档也归类到此处。

---

## 目录结构

```
miduo-md/
├── README.md                           # 本文件：文档总览索引
│
├── business/                           # 产品需求文档（PRD / TDD）
│   ├── 工单系统产品设计方案.md            # 核心主文档 v1.3
│   ├── 技术分析文档.md                   # 技术架构分析 v1.0
│   ├── 缺陷管理模块PRD.md
│   ├── 缺陷管理模块TDD.md
│   ├── Bug简报创建功能PRD.md
│   ├── 仪表盘模块个性化布局PRD.md
│   ├── 企微文本消息自动创建工单产品方案.md
│   └── 企微图片消息工单关联产品方案.md
│
├── database/                           # 数据库迁移脚本（Flyway）
│   ├── 项目初始化数据库/
│   │   └── ticket_platform.sql         # 全量初始化脚本
│   ├── 20260302_V1__init_base.sql
│   ├── 20260302_V2__init_ticket_core.sql
│   ├── 20260302_V3__init_workflow_sla.sql
│   ├── 20260302_V4__init_time_track.sql
│   ├── 20260302_V5__init_bug_ticket.sql
│   ├── 20260302_V6__init_bug_report.sql
│   ├── 20260302_V7__init_wecom_notification.sql
│   ├── 20260304_V12__enhance_org_account_management.sql
│   └── V15_workflow_engine_refactor_20260312.sql
│
├── rules/                              # 项目开发规则
│   └── JAVA项目的目录规则 (AI).md
│
└── workflow/                           # 研发流程文档
    ├── 功能接口对应关系.md               # 全局API编号注册表（最新版，持续维护）
    ├── 功能接口对应关系-v1.0-历史归档.md  # v1.0早期版本（仅作历史参考）
    ├── DEPLOYMENT.md                   # CI/CD与生产部署说明
    ├── 架构分析与问题梳理报告.md          # 架构现状分析（2026-03-12）
    ├── 工单系统/                        # 工单系统任务（task001–task024）
    ├── sso/                            # 企微SSO集成任务（task001–task009）
    ├── test/                           # 联调测试记录
    ├── @缺陷管理/                       # 缺陷管理模块任务（task001–task018）
    └── @仪表盘布局/                     # 仪表盘个性化布局任务（task001–task007）
```

---

## 一、产品需求文档（business/）

| 文档 | 版本 | 状态 | 说明 |
|------|------|------|------|
| [工单系统产品设计方案.md](business/工单系统产品设计方案.md) | v1.3 | ✅已定稿 | **核心主文档**，覆盖全部功能模块，所有开发须以此为准 |
| [技术分析文档.md](business/技术分析文档.md) | v1.0 | ✅已定稿 | 技术可行性分析、架构决策、风险识别 |
| [缺陷管理模块PRD.md](business/缺陷管理模块PRD.md) | v1.0 | ✅已定稿 | 缺陷/Bug跟踪工作流完整需求 |
| [缺陷管理模块TDD.md](business/缺陷管理模块TDD.md) | v1.0 | ✅已定稿 | 缺陷模块技术设计文档 |
| [Bug简报创建功能PRD.md](business/Bug简报创建功能PRD.md) | v1.0 | 📝待评审 | Bug简报创建功能需求 |
| [仪表盘模块个性化布局PRD.md](business/仪表盘模块个性化布局PRD.md) | v2.0 | ✅已定稿 | 用户独立仪表盘布局定制（v2.0全量改写） |
| [企微文本消息自动创建工单产品方案.md](business/企微文本消息自动创建工单产品方案.md) | v1.0 | ✅已定稿 | 企微文本消息自动建单（对应Task023） |
| [企微图片消息工单关联产品方案.md](business/企微图片消息工单关联产品方案.md) | v1.0 | ✅已定稿 | 企微截图/图片消息关联工单（对应Task024） |

---

## 二、技术规范（rules/）

| 文档 | 说明 |
|------|------|
| [rules/JAVA项目的目录规则 (AI).md](<rules/JAVA项目的目录规则 (AI).md>) | `miduo-basebackend` 多模块项目目录完整规范（AI生成代码参考用） |

---

## 三、数据库迁移脚本（database/）

> **说明**：脚本通过 Flyway 在后端服务启动时自动执行，命名规则为 `V{版本号}__{描述}.sql`

| 脚本 | 版本 | 说明 |
|------|------|------|
| [项目初始化数据库/ticket_platform.sql](database/项目初始化数据库/ticket_platform.sql) | — | 全量初始化脚本（首次建库用） |
| [20260302_V1__init_base.sql](database/20260302_V1__init_base.sql) | V1 | 基础结构（用户、权限） |
| [20260302_V2__init_ticket_core.sql](database/20260302_V2__init_ticket_core.sql) | V2 | 工单核心表 |
| [20260302_V3__init_workflow_sla.sql](database/20260302_V3__init_workflow_sla.sql) | V3 | 工作流 + SLA策略表 |
| [20260302_V4__init_time_track.sql](database/20260302_V4__init_time_track.sql) | V4 | 工时追踪表 |
| [20260302_V5__init_bug_ticket.sql](database/20260302_V5__init_bug_ticket.sql) | V5 | 缺陷工单扩展表 |
| [20260302_V6__init_bug_report.sql](database/20260302_V6__init_bug_report.sql) | V6 | Bug简报表 |
| [20260302_V7__init_wecom_notification.sql](database/20260302_V7__init_wecom_notification.sql) | V7 | 企微通知表 |
| [20260304_V12__enhance_org_account_management.sql](database/20260304_V12__enhance_org_account_management.sql) | V12 | 组织账号管理增强 |
| [V15_workflow_engine_refactor_20260312.sql](database/V15_workflow_engine_refactor_20260312.sql) | V15 | 工作流引擎重构（2026-03-12） |

---

## 四、API接口编号管理（workflow/功能接口对应关系.md）

> **规则**：接口编号格式为 API + 6位自增编码，如 API000001。每次新增接口后必须更新此文档。  
> **主文件**：[workflow/功能接口对应关系.md](workflow/功能接口对应关系.md)（最新版，持续维护）

### 编号段分配

| 编号段 | 模块 | 说明 |
|--------|------|------|
| API000001 – API000099 | 工单核心 | TicketController、KanbanController |
| API000100 – API000199 | 通知中心 | NotificationController |
| API000200 – API000299 | 工作流/处理组 | WorkflowController、HandlerGroupController |
| API000300 – API000399 | SLA策略 | SlaPolicyController |
| API000400 – API000499 | 认证/用户/组织/仪表盘 | AuthController、UserController、DashboardController |
| API000500 – API000599 | 缺陷增强/图片上传 | TicketChangeHistoryController、TicketImageController |
| API000600 – API000699 | 预留 | — |

---

## 五、研发任务文档（workflow/）

### 5.1 工单系统主体任务（task001–task024）

> 总览：[workflow/工单系统/README-工单系统Task任务拆解总览.md](workflow/工单系统/README-工单系统Task任务拆解总览.md)  
> **执行顺序**：Task001→…→Task010（后端）→Task011→…→Task015（前端）→Task016→…→Task022（质量治理）→Task023→Task024（企微扩展）

| Task | 文档 | 状态 | 说明 |
|------|------|------|------|
| Task001 | [task001-项目工程搭建与基础架构.md](workflow/工单系统/task001-项目工程搭建与基础架构.md) | ✅ | 项目工程搭建 |
| Task002 | [task002-数据库设计与初始化.md](workflow/工单系统/task002-数据库设计与初始化.md) | ✅ | 数据库设计与初始化 |
| Task003 | [task003-企业微信认证与用户体系.md](workflow/工单系统/task003-企业微信认证与用户体系.md) | ✅ | 企微认证与用户体系 |
| Task004 | [task004-工单核心管理.md](workflow/工单系统/task004-工单核心管理.md) | ✅ | 工单分类/创建/列表/详情 |
| Task005 | [task005-工作流引擎与分派.md](workflow/工单系统/task005-工作流引擎与分派.md) | ✅ | 工作流引擎与分派 |
| Task006 | [task006-企业微信深度集成.md](workflow/工单系统/task006-企业微信深度集成.md) | ✅ | 企微应用消息/群机器人 |
| Task007 | [task007-SLA管理与通知中心.md](workflow/工单系统/task007-SLA管理与通知中心.md) | ✅ | SLA管理与通知中心 |
| Task008 | [task008-缺陷工单增强与时间追踪.md](workflow/工单系统/task008-缺陷工单增强与时间追踪.md) | ✅ | 缺陷工单增强+时间追踪 |
| Task009 | [task009-Bug简报管理.md](workflow/工单系统/task009-Bug简报管理.md) | ✅ | Bug简报管理 |
| Task010 | [task010-数据看板与开放能力.md](workflow/工单系统/task010-数据看板与开放能力.md) | ✅ | 数据看板+报表+开放API |
| Task011 | [task011-前端工程搭建与基础架构.md](workflow/工单系统/task011-前端工程搭建与基础架构.md) | ✅ | 前端工程搭建 |
| Task012 | [task012-前端布局与路由.md](workflow/工单系统/task012-前端布局与路由.md) | ✅ | 前端布局与路由 |
| Task013 | [task013-前端认证与全局状态.md](workflow/工单系统/task013-前端认证与全局状态.md) | ✅ | 前端认证与全局状态 |
| Task014 | [task014-前端通用组件与设计系统.md](workflow/工单系统/task014-前端通用组件与设计系统.md) | ✅ | 前端通用组件库 |
| Task015 | [task015-工单前端模块骨架与联调.md](workflow/工单系统/task015-工单前端模块骨架与联调.md) | ✅ | 工单前端骨架与联调 |
| Task016 | [task016-管理端页面落地与基础联调.md](workflow/工单系统/task016-管理端页面落地与基础联调.md) | ✅ | 管理端页面落地 |
| Task017 | [task017-管理端接口封装与联调深化.md](workflow/工单系统/task017-管理端接口封装与联调深化.md) | ✅ | 管理端接口治理 |
| Task018 | [task018-通知中心与消息闭环.md](workflow/工单系统/task018-通知中心与消息闭环.md) | ✅ | 通知中心消息闭环 |
| Task019 | [task019-Bug简报前端模块落地.md](workflow/工单系统/task019-Bug简报前端模块落地.md) | ✅ | Bug简报前端落地 |
| Task020 | [task020-系统设置增强与集成配置.md](workflow/工单系统/task020-系统设置增强与集成配置.md) | ✅ | 系统设置与集成配置 |
| Task021 | [task021-接口编号与文档治理.md](workflow/工单系统/task021-接口编号与文档治理.md) | ✅ | 接口编号与文档治理 |
| Task022 | [task022-测试与发布基线建设.md](workflow/工单系统/task022-测试与发布基线建设.md) | ✅ | 测试与发布基线 |
| Task023 | [task023-企微文本消息自动创建工单.md](workflow/工单系统/task023-企微文本消息自动创建工单.md) | 🚧 | 企微文本消息自动建单 |
| Task024 | [task024-企微图片消息工单关联PRD.md](workflow/工单系统/task024-企微图片消息工单关联PRD.md) | ⏳ | 企微图片消息工单关联 |

**Task006/007/008/009/010 API接口设计文档：**

| 文档 | 说明 |
|------|------|
| [task006-企业微信深度集成API接口设计.md](workflow/工单系统/task006-企业微信深度集成API接口设计.md) | 企微消息推送/群机器人接口 |
| [task007-SLA管理与通知中心API接口设计.md](workflow/工单系统/task007-SLA管理与通知中心API接口设计.md) | SLA规则/升级/通知接口 |
| [task008-缺陷工单增强与时间追踪API接口设计.md](workflow/工单系统/task008-缺陷工单增强与时间追踪API接口设计.md) | 缺陷工单增强接口 |
| [task009-Bug简报管理API接口设计.md](workflow/工单系统/task009-Bug简报管理API接口设计.md) | Bug简报管理接口 |
| [task010-数据看板与开放能力API接口设计.md](workflow/工单系统/task010-数据看板与开放能力API接口设计.md) | 看板/报表/OpenAPI |
| [工单核心管理模块API接口设计.md](workflow/工单系统/工单核心管理模块API接口设计.md) | Task004 工单核心接口详细设计 |
| [认证与用户模块API接口设计.md](workflow/工单系统/认证与用户模块API接口设计.md) | Task003 认证/用户/组织接口 |

---

### 5.2 企业微信SSO集成任务（sso/）

| 文档 | 说明 |
|------|------|
| [sso/task001-企业微信账号体系复用任务拆解.md](workflow/sso/task001-企业微信账号体系复用任务拆解.md) | SSO总体任务拆解 |
| [sso/task001-项目启动记录.md](workflow/sso/task001-项目启动记录.md) | 项目启动会议记录 |
| [sso/task001-验收口径对齐记录.md](workflow/sso/task001-验收口径对齐记录.md) | 验收标准对齐 |
| [sso/task002-数据模型与字段映射说明.md](workflow/sso/task002-数据模型与字段映射说明.md) | 数据模型与字段映射 |
| [sso/task003-企微连接配置与API客户端交付说明.md](workflow/sso/task003-企微连接配置与API客户端交付说明.md) | 企微连接配置+API客户端 |
| [sso/task004-同步引擎阶段性说明.md](workflow/sso/task004-同步引擎阶段性说明.md) | 组织同步引擎实现 |
| [sso/task005-组织查询接口交付说明.md](workflow/sso/task005-组织查询接口交付说明.md) | 组织查询接口交付 |
| [sso/task006-组织账号管理产品规划方案.md](workflow/sso/task006-组织账号管理产品规划方案.md) | 组织账号管理产品方案 |
| [sso/task007-组织账号管理技术实现方案.md](workflow/sso/task007-组织账号管理技术实现方案.md) | 组织账号管理技术方案 |
| [sso/task008-企微用户管理模块重构产品方案.md](workflow/sso/task008-企微用户管理模块重构产品方案.md) | 企微用户管理重构产品方案 |
| [sso/task009-企微用户管理模块重构技术方案.md](workflow/sso/task009-企微用户管理模块重构技术方案.md) | 企微用户管理重构技术方案 |
| [sso/工单系统-企业微信账号体系复用实施清单.md](workflow/sso/工单系统-企业微信账号体系复用实施清单.md) | SSO复用实施清单 |
| [sso/工单系统-企业微信复用测试与上线手册.md](workflow/sso/工单系统-企业微信复用测试与上线手册.md) | SSO测试与上线手册 |

---

### 5.3 缺陷管理模块任务（@缺陷管理/）

> [workflow/@缺陷管理/task001-task018.md](workflow/@缺陷管理/task001-task018.md)

| 范围 | 内容 | 状态 |
|------|------|------|
| task001 | 产品方案设计 & 技术分析 | ✅ |
| task002–005 | 后端枚举/DTO/实体扩展 | ✅ |
| task006–012 | 后端Mapper/Service/Controller | ✅ |
| task013–017 | 前端类型/API/组件/页面改造 | ✅ |
| task018 | 联调测试 | ⏳待测试 |

---

### 5.4 仪表盘个性化布局任务（@仪表盘布局/）

| Task | 文档 | 说明 |
|------|------|------|
| task001 | [task001-数据库迁移脚本.md](workflow/@仪表盘布局/task001-数据库迁移脚本.md) | dashboard_layout表创建 |
| task002 | [task002-后端基础层开发.md](workflow/@仪表盘布局/task002-后端基础层开发.md) | 后端常量/枚举/Entity/DTO/Mapper |
| task003 | [task003-后端Service和Controller开发.md](workflow/@仪表盘布局/task003-后端Service和Controller开发.md) | 后端Service+Controller（API000411–413） |
| task004 | [task004-前端API封装和Store.md](workflow/@仪表盘布局/task004-前端API封装和Store.md) | 前端API封装+Pinia Store |
| task005 | [task005-前端仪表盘重构动态渲染.md](workflow/@仪表盘布局/task005-前端仪表盘重构动态渲染.md) | 前端动态渲染重构 |
| task006 | [task006-前端拖拽交互和编辑模式UI.md](workflow/@仪表盘布局/task006-前端拖拽交互和编辑模式UI.md) | 前端拖拽交互与编辑模式 |
| task007 | [task007-多账号隔离联调测试.md](workflow/@仪表盘布局/task007-多账号隔离联调测试.md) | 多账号数据隔离联调测试 |

---

## 六、联调测试记录（workflow/test/）

| 文档 | 说明 |
|------|------|
| [test/测试环境发布与企微集成从零实施手册.md](workflow/test/测试环境发布与企微集成从零实施手册.md) | 测试环境首次部署+企微集成完整步骤 |
| [test/测试脚本记录.md](workflow/test/测试脚本记录.md) | 前端测试JWT注入脚本（绕过企微认证） |
| [test/task016-管理端页面落地与基础联调-联调记录.md](workflow/test/task016-管理端页面落地与基础联调-联调记录.md) | Task016 管理端页面联调结果 |
| [test/task017-管理端接口封装与联调深化-治理记录.md](workflow/test/task017-管理端接口封装与联调深化-治理记录.md) | Task017 接口层治理记录 |
| [test/task018-通知中心与消息闭环-联调记录.md](workflow/test/task018-通知中心与消息闭环-联调记录.md) | Task018 通知中心联调记录 |
| [test/task019-Bug简报前端模块落地-联调记录.md](workflow/test/task019-Bug简报前端模块落地-联调记录.md) | Task019 Bug简报前端联调记录 |
| [test/task020-系统设置增强与集成配置-联调记录.md](workflow/test/task020-系统设置增强与集成配置-联调记录.md) | Task020 系统设置联调记录 |

---

## 七、架构与部署（workflow/）

| 文档 | 说明 |
|------|------|
| [workflow/架构分析与问题梳理报告.md](workflow/架构分析与问题梳理报告.md) | 2026-03-12 系统架构现状审查与设计问题清单 |
| [workflow/DEPLOYMENT.md](workflow/DEPLOYMENT.md) | GitHub Actions CI/CD流水线 + 生产Docker Compose部署说明 |

---

## 八、快速检索

### 按模块检索

| 模块 | 相关文档 |
|------|---------|
| **工单核心** | 产品设计方案§4.2+§4.3 · Task004 · 工单核心管理API接口设计 |
| **工作流引擎** | 产品设计方案§4.4 · Task005 · V15数据库脚本 |
| **SLA管理** | 产品设计方案§4.7 · Task007 · SLA API接口设计 |
| **企微认证** | 产品设计方案§4.6.1+§4.6.2 · Task003 · SSO任务系列 |
| **企微深度集成** | 产品设计方案§4.6.3+§4.6.4 · Task006+Task023+Task024 · 企微API接口设计 |
| **缺陷工单** | 缺陷管理模块PRD · 缺陷管理TDD · @缺陷管理/task001–018 |
| **Bug简报** | Bug简报创建功能PRD · Task009+Task019 · Bug简报API接口设计 |
| **数据看板** | 产品设计方案§4.9+§4.11 · Task010 · 看板API接口设计 |
| **仪表盘布局** | 仪表盘个性化布局PRD · @仪表盘布局/task001–007 |
| **系统设置** | 产品设计方案§4.10 · Task020 |
| **通知中心** | 产品设计方案§4.8 · Task007+Task018 |
| **开放API** | 产品设计方案§5.2+§5.3 · Task010 · 看板API接口设计 |

### 按角色检索

| 角色 | 推荐阅读 |
|------|---------|
| **产品经理** | 工单系统产品设计方案 → 各模块PRD → 功能接口对应关系 |
| **后端开发** | 技术分析文档 → Java目录规则 → 对应Task → 对应API接口设计 → 数据库SQL |
| **前端开发** | Task011–020 → 对应模块PRD → 联调测试记录 |
| **测试工程师** | 联调测试记录（test/） → 测试环境部署手册 → 测试脚本记录 |
| **运维** | DEPLOYMENT.md → AGENTS.md（/workspace根目录） |
| **新成员** | AGENTS.md（项目根目录，先读）→ 产品设计方案 → 技术分析文档 → 对应模块Task |

---

*本索引由 AI Agent 于 2026-03-15 整理生成，后续请人工维护更新。*
