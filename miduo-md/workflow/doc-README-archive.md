# 米多内部工单系统 — 文档总览索引

> **版本**：v1.0  
> **更新日期**：2026-03-15  
> **维护说明**：每次新增或修改文档后请同步更新本索引

---

## 目录结构说明

```
/workspace/
├── doc/                          # 工单系统主要文档（产品+技术+研发流程）
│   ├── README.md                 # 本文件：文档总览索引
│   ├── 工单系统产品设计方案.md      # 产品总设计方案（核心主文档）
│   ├── 技术分析文档.md             # 技术架构分析文档
│   ├── business/                 # 业务需求（增量PRD）
│   ├── database/                 # 数据库迁移脚本
│   ├── rules/                    # 项目开发规则
│   └── workflow/                 # 研发流程文档
│       ├── 功能接口对应关系.md      # 全局API编号注册表
│       ├── 工单系统/              # 工单系统任务拆解（task001~task024）
│       ├── sso/                  # 企业微信SSO集成任务
│       └── test/                 # 联调测试记录
│
├── miduo-md/                     # 增量业务模块文档（新功能专项）
│   ├── business/                 # 新功能PRD与TDD
│   ├── database/                 # 增量数据库迁移脚本
│   └── workflow/                 # 新功能研发流程
│       ├── 功能接口对应关系.md      # 增量API编号注册表
│       ├── DEPLOYMENT.md         # 部署说明
│       ├── 架构分析与问题梳理报告.md # 架构现状分析
│       ├── @缺陷管理/             # 缺陷管理模块任务
│       └── @仪表盘布局/           # 仪表盘个性化布局任务
│
├── ticket-platform/              # 后端项目
│   └── README.md                 # 后端技术栈与启动说明
│
├── miduo-frontend/               # 前端项目
│   ├── README.md                 # 前端技术栈与启动说明
│   └── src/components/common/README.md  # 通用组件使用文档
│
├── scripts/
│   └── README-cloud-agent.md     # 云端Agent环境初始化说明
│
└── AGENTS.md                     # Cloud Agent运行指南（服务启动/端口/注意事项）
```

---

## 一、产品文档

### 1.1 核心产品设计文档

| 文档 | 路径 | 版本 | 说明 |
|------|------|------|------|
| 工单系统产品设计方案 | [doc/工单系统产品设计方案.md](工单系统产品设计方案.md) | v1.3 | **核心主文档**，覆盖全部功能模块，所有开发须以此为准 |
| 技术分析文档 | [doc/技术分析文档.md](技术分析文档.md) | v1.0 | 技术可行性分析、架构决策、风险识别 |

### 1.2 增量业务需求（PRD）

| 文档 | 路径 | 版本 | 说明 |
|------|------|------|------|
| 缺陷管理模块PRD | [miduo-md/business/缺陷管理模块PRD.md](../miduo-md/business/缺陷管理模块PRD.md) | v1.0 | 缺陷/Bug跟踪工作流需求（已定稿） |
| 缺陷管理模块TDD | [miduo-md/business/缺陷管理模块TDD.md](../miduo-md/business/缺陷管理模块TDD.md) | v1.0 | 缺陷模块技术设计文档 |
| Bug简报创建功能PRD | [miduo-md/business/Bug简报创建功能PRD.md](../miduo-md/business/Bug简报创建功能PRD.md) | v1.0 | Bug简报创建功能需求（待评审） |
| 仪表盘个性化布局PRD | [miduo-md/business/仪表盘模块个性化布局PRD.md](../miduo-md/business/仪表盘模块个性化布局PRD.md) | v2.0 | 用户独立仪表盘布局定制需求 |
| 企微文本消息自动创建工单PRD | [doc/business/企微文本消息自动创建工单产品方案.md](business/企微文本消息自动创建工单产品方案.md) | v1.0 | 企微文本消息自动建单方案（对应Task023） |
| 企微图片消息工单关联PRD | [doc/business/企微图片消息工单关联产品方案.md](business/企微图片消息工单关联产品方案.md) | v1.0 | 企微截图/图片消息关联工单方案（对应Task024） |

---

## 二、技术规范与架构文档

| 文档 | 路径 | 说明 |
|------|------|------|
| Java项目目录规则 | [doc/rules/JAVA项目的目录规则 (AI).md](rules/JAVA项目的目录规则%20(AI).md) | miduo-basebackend多模块项目目录规范（AI参考用） |
| 架构分析与问题梳理报告 | [miduo-md/workflow/架构分析与问题梳理报告.md](../miduo-md/workflow/架构分析与问题梳理报告.md) | 2026-03-12现有系统架构审查与设计问题清单 |
| 部署说明 | [miduo-md/workflow/DEPLOYMENT.md](../miduo-md/workflow/DEPLOYMENT.md) | GitHub Actions CI/CD流水线与生产Docker Compose部署 |
| Cloud Agent运行指南 | [AGENTS.md](../AGENTS.md) | 服务端口表、JDK/MySQL/Docker注意事项、构建测试命令 |

---

## 三、API接口管理

> **接口编号规则**：API + 6位自增编码，如 API000001

| 文档 | 路径 | 说明 |
|------|------|------|
| 功能接口对应关系（主表） | [doc/workflow/功能接口对应关系.md](workflow/功能接口对应关系.md) | 工单系统 API000001起，按模块分段，全局唯一编号注册表 |
| 功能接口对应关系（增量） | [miduo-md/workflow/功能接口对应关系.md](../miduo-md/workflow/功能接口对应关系.md) | 缺陷管理、仪表盘等增量模块接口编号表（含API000500+段） |
| 工单核心管理模块API设计 | [doc/workflow/工单系统/工单核心管理模块API接口设计.md](workflow/工单系统/工单核心管理模块API接口设计.md) | Task004 工单核心接口详细设计 |
| 认证与用户模块API设计 | [doc/workflow/工单系统/认证与用户模块API接口设计.md](workflow/工单系统/认证与用户模块API接口设计.md) | Task003 认证/用户/组织接口详细设计 |
| 企微深度集成API设计 | [doc/workflow/工单系统/task006-企业微信深度集成API接口设计.md](workflow/工单系统/task006-企业微信深度集成API接口设计.md) | Task006 企微消息推送/群机器人接口设计 |
| SLA与通知中心API设计 | [doc/workflow/工单系统/task007-SLA管理与通知中心API接口设计.md](workflow/工单系统/task007-SLA管理与通知中心API接口设计.md) | Task007 SLA规则/升级/通知接口设计 |
| 缺陷工单增强API设计 | [doc/workflow/工单系统/task008-缺陷工单增强与时间追踪API接口设计.md](workflow/工单系统/task008-缺陷工单增强与时间追踪API接口设计.md) | Task008 缺陷工单增强接口设计 |
| Bug简报管理API设计 | [doc/workflow/工单系统/task009-Bug简报管理API接口设计.md](workflow/工单系统/task009-Bug简报管理API接口设计.md) | Task009 Bug简报接口设计 |
| 数据看板与开放能力API设计 | [doc/workflow/工单系统/task010-数据看板与开放能力API接口设计.md](workflow/工单系统/task010-数据看板与开放能力API接口设计.md) | Task010 看板/报表/OpenAPI设计 |

---

## 四、数据库文档

| 文件 | 路径 | 说明 |
|------|------|------|
| 项目初始化全量SQL | [doc/database/项目初始化数据库/ticket_platform.sql](database/项目初始化数据库/ticket_platform.sql) | 全量初始化脚本 |
| V1 基础表 | [doc/database/20260302_V1__init_base.sql](database/20260302_V1__init_base.sql) | Flyway V1：基础结构 |
| V2 工单核心表 | [doc/database/20260302_V2__init_ticket_core.sql](database/20260302_V2__init_ticket_core.sql) | Flyway V2：工单核心 |
| V3 工作流SLA表 | [doc/database/20260302_V3__init_workflow_sla.sql](database/20260302_V3__init_workflow_sla.sql) | Flyway V3：工作流+SLA |
| V4 时间追踪表 | [doc/database/20260302_V4__init_time_track.sql](database/20260302_V4__init_time_track.sql) | Flyway V4：工时追踪 |
| V5 缺陷工单增强 | [doc/database/20260302_V5__init_bug_ticket.sql](database/20260302_V5__init_bug_ticket.sql) | Flyway V5：缺陷工单扩展 |
| V6 Bug简报 | [doc/database/20260302_V6__init_bug_report.sql](database/20260302_V6__init_bug_report.sql) | Flyway V6：Bug简报 |
| V7 企微通知 | [doc/database/20260302_V7__init_wecom_notification.sql](database/20260302_V7__init_wecom_notification.sql) | Flyway V7：企微通知 |
| V12 组织账号管理增强 | [doc/database/20260304_V12__enhance_org_account_management.sql](database/20260304_V12__enhance_org_account_management.sql) | Flyway V12：组织账号管理 |
| V15 工作流引擎重构 | [miduo-md/database/V15_workflow_engine_refactor_20260312.sql](../miduo-md/database/V15_workflow_engine_refactor_20260312.sql) | Flyway V15：工作流引擎重构 |

---

## 五、研发任务文档（Task）

### 5.1 工单系统主体任务（Task001–Task024）

> 任务总览：[doc/workflow/工单系统/README-工单系统Task任务拆解总览.md](workflow/工单系统/README-工单系统Task任务拆解总览.md)
> 
> **执行顺序**：Task001 → Task002 → … → Task010（后端） → Task011 → … → Task015（前端） → Task016 → … → Task022（质量治理） → Task023 → Task024（企微扩展）

| Task | 文档路径 | 状态 | 说明 |
|------|----------|------|------|
| Task001 | [task001-项目工程搭建与基础架构.md](workflow/工单系统/task001-项目工程搭建与基础架构.md) | ✅ | 项目搭建与基础架构 |
| Task002 | [task002-数据库设计与初始化.md](workflow/工单系统/task002-数据库设计与初始化.md) | ✅ | 数据库设计 |
| Task003 | [task003-企业微信认证与用户体系.md](workflow/工单系统/task003-企业微信认证与用户体系.md) | ✅ | 企微认证与用户体系 |
| Task004 | [task004-工单核心管理.md](workflow/工单系统/task004-工单核心管理.md) | ✅ | 工单创建/列表/详情 |
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

### 5.2 企业微信SSO集成任务

> 目录：[doc/workflow/sso/](workflow/sso/)

| Task | 文档 | 说明 |
|------|------|------|
| Task001 | [task001-企业微信账号体系复用任务拆解.md](workflow/sso/task001-企业微信账号体系复用任务拆解.md) | 企微账号体系复用总体任务拆解 |
| Task001 | [task001-项目启动记录.md](workflow/sso/task001-项目启动记录.md) | 项目启动会议记录 |
| Task001 | [task001-验收口径对齐记录.md](workflow/sso/task001-验收口径对齐记录.md) | 验收标准对齐记录 |
| Task002 | [task002-数据模型与字段映射说明.md](workflow/sso/task002-数据模型与字段映射说明.md) | 数据模型与字段映射 |
| Task003 | [task003-企微连接配置与API客户端交付说明.md](workflow/sso/task003-企微连接配置与API客户端交付说明.md) | 企微连接配置与API客户端 |
| Task004 | [task004-同步引擎阶段性说明.md](workflow/sso/task004-同步引擎阶段性说明.md) | 组织同步引擎实现 |
| Task005 | [task005-组织查询接口交付说明.md](workflow/sso/task005-组织查询接口交付说明.md) | 组织查询接口 |
| Task006 | [task006-组织账号管理产品规划方案.md](workflow/sso/task006-组织账号管理产品规划方案.md) | 组织账号管理产品方案 |
| Task007 | [task007-组织账号管理技术实现方案.md](workflow/sso/task007-组织账号管理技术实现方案.md) | 组织账号管理技术方案 |
| Task008 | [task008-企微用户管理模块重构产品方案.md](workflow/sso/task008-企微用户管理模块重构产品方案.md) | 企微用户管理重构产品方案 |
| Task009 | [task009-企微用户管理模块重构技术方案.md](workflow/sso/task009-企微用户管理模块重构技术方案.md) | 企微用户管理重构技术方案 |
| — | [工单系统-企业微信账号体系复用实施清单.md](workflow/sso/工单系统-企业微信账号体系复用实施清单.md) | SSO复用实施清单 |
| — | [工单系统-企业微信复用测试与上线手册.md](workflow/sso/工单系统-企业微信复用测试与上线手册.md) | SSO测试与上线手册 |

### 5.3 缺陷管理模块任务

> 目录：[miduo-md/workflow/@缺陷管理/](../miduo-md/workflow/@缺陷管理/)

| Task | 文档 | 说明 |
|------|------|------|
| Task001–018 | [task001-task018.md](../miduo-md/workflow/@缺陷管理/task001-task018.md) | 缺陷管理模块全量任务清单（18个任务，已完成17个） |

### 5.4 仪表盘个性化布局任务

> 目录：[miduo-md/workflow/@仪表盘布局/](../miduo-md/workflow/@仪表盘布局/)

| Task | 文档 | 说明 |
|------|------|------|
| Task001 | [task001-数据库迁移脚本.md](../miduo-md/workflow/@仪表盘布局/task001-数据库迁移脚本.md) | 数据库表创建迁移脚本 |
| Task002 | [task002-后端基础层开发.md](../miduo-md/workflow/@仪表盘布局/task002-后端基础层开发.md) | 后端常量/枚举/Entity/DTO/Mapper |
| Task003 | [task003-后端Service和Controller开发.md](../miduo-md/workflow/@仪表盘布局/task003-后端Service和Controller开发.md) | 后端Service+Controller（API000411–413） |
| Task004 | [task004-前端API封装和Store.md](../miduo-md/workflow/@仪表盘布局/task004-前端API封装和Store.md) | 前端API封装+Pinia Store |
| Task005 | [task005-前端仪表盘重构动态渲染.md](../miduo-md/workflow/@仪表盘布局/task005-前端仪表盘重构动态渲染.md) | 前端仪表盘动态渲染重构 |
| Task006 | [task006-前端拖拽交互和编辑模式UI.md](../miduo-md/workflow/@仪表盘布局/task006-前端拖拽交互和编辑模式UI.md) | 前端拖拽交互与编辑模式 |
| Task007 | [task007-多账号隔离联调测试.md](../miduo-md/workflow/@仪表盘布局/task007-多账号隔离联调测试.md) | 多账号数据隔离联调测试 |

---

## 六、联调测试记录

> 目录：[doc/workflow/test/](workflow/test/)

| 文档 | 说明 |
|------|------|
| [测试环境发布与企微集成从零实施手册.md](workflow/test/测试环境发布与企微集成从零实施手册.md) | 测试环境首次部署+企微集成完整步骤 |
| [测试脚本记录.md](workflow/test/测试脚本记录.md) | 前端测试JWT注入脚本（绕过企微认证） |
| [task016-管理端页面落地与基础联调-联调记录.md](workflow/test/task016-管理端页面落地与基础联调-联调记录.md) | Task016联调结果记录 |
| [task017-管理端接口封装与联调深化-治理记录.md](workflow/test/task017-管理端接口封装与联调深化-治理记录.md) | Task017接口层治理记录 |
| [task018-通知中心与消息闭环-联调记录.md](workflow/test/task018-通知中心与消息闭环-联调记录.md) | Task018联调结果记录 |
| [task019-Bug简报前端模块落地-联调记录.md](workflow/test/task019-Bug简报前端模块落地-联调记录.md) | Task019联调结果记录 |
| [task020-系统设置增强与集成配置-联调记录.md](workflow/test/task020-系统设置增强与集成配置-联调记录.md) | Task020联调结果记录 |

---

## 七、项目README文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 后端README | [ticket-platform/README.md](../ticket-platform/README.md) | 后端技术栈、模块结构、启动指南 |
| 前端README | [miduo-frontend/README.md](../miduo-frontend/README.md) | 前端技术栈、开发启动、代理配置 |
| 通用组件使用文档 | [miduo-frontend/src/components/common/README.md](../miduo-frontend/src/components/common/README.md) | BaseTable/BasePagination/StatusTag使用说明 |
| Cloud Agent脚本说明 | [scripts/README-cloud-agent.md](../scripts/README-cloud-agent.md) | 云端Agent环境初始化脚本说明 |

---

## 八、快速检索

### 按模块检索

| 模块 | 相关文档 |
|------|---------|
| **工单核心** | 产品设计方案§4.2+§4.3、Task004、工单核心管理API接口设计 |
| **工作流引擎** | 产品设计方案§4.4、Task005、V15数据库脚本 |
| **SLA管理** | 产品设计方案§4.7、Task007、SLA API接口设计 |
| **企微认证** | 产品设计方案§4.6.1+§4.6.2、Task003、SSO任务系列 |
| **企微深度集成** | 产品设计方案§4.6.3+§4.6.4、Task006+Task023+Task024、企微API接口设计 |
| **缺陷工单** | 产品设计方案§4.4.5、缺陷管理模块PRD、缺陷管理TDD、缺陷模块Task001–018 |
| **Bug简报** | 产品设计方案§4.12、Bug简报创建功能PRD、Task009+Task019 |
| **数据看板** | 产品设计方案§4.9+§4.11、Task010、看板API接口设计 |
| **仪表盘布局** | 仪表盘个性化布局PRD、仪表盘布局Task001–007 |
| **系统设置** | 产品设计方案§4.10、Task020 |
| **通知中心** | 产品设计方案§4.8、Task007+Task018、通知API接口设计 |
| **开放API** | 产品设计方案§5.2+§5.3、Task010、看板API接口设计 |

### 按角色检索

| 角色 | 推荐阅读 |
|------|---------|
| **产品经理** | 工单系统产品设计方案、各模块PRD、功能接口对应关系 |
| **后端开发** | 技术分析文档、Java目录规则、各Task后端部分、各模块API接口设计、数据库SQL |
| **前端开发** | Task011–020、前端README、通用组件README、各Task前端部分 |
| **测试工程师** | 联调测试记录、测试环境部署手册、测试脚本记录 |
| **运维** | DEPLOYMENT.md、AGENTS.md、测试环境部署手册 |
| **新成员** | AGENTS.md（先读） → 产品设计方案 → 技术分析文档 → 对应模块Task |

---

*本索引由 AI Agent 于 2026-03-15 整理生成，后续请人工维护更新。*
