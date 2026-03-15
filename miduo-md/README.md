# miduo-md — 增量业务模块文档

> 本目录存放工单系统**增量业务模块**的产品文档、技术文档和研发任务文档。  
> 基础系统文档请参见 [doc/README.md](../doc/README.md)

---

## 目录结构

```
miduo-md/
├── README.md                       # 本文件
├── business/                       # 增量业务需求文档（PRD/TDD）
│   ├── 缺陷管理模块PRD.md
│   ├── 缺陷管理模块TDD.md
│   ├── Bug简报创建功能PRD.md
│   └── 仪表盘模块个性化布局PRD.md
├── database/                       # 增量数据库迁移脚本
│   └── V15_workflow_engine_refactor_20260312.sql
└── workflow/                       # 增量研发流程文档
    ├── 功能接口对应关系.md            # 增量模块API编号注册表
    ├── DEPLOYMENT.md               # 部署说明
    ├── 架构分析与问题梳理报告.md       # 架构现状分析（2026-03-12）
    ├── @缺陷管理/                   # 缺陷管理模块任务（task001–018）
    └── @仪表盘布局/                  # 仪表盘个性化布局任务（task001–007）
```

---

## 业务需求文档

| 文档 | 版本 | 状态 | 说明 |
|------|------|------|------|
| [缺陷管理模块PRD](business/缺陷管理模块PRD.md) | v1.0 | ✅已定稿 | 缺陷/Bug跟踪工作流完整需求 |
| [缺陷管理模块TDD](business/缺陷管理模块TDD.md) | v1.0 | ✅已定稿 | 缺陷模块技术设计文档 |
| [Bug简报创建功能PRD](business/Bug简报创建功能PRD.md) | v1.0 | 📝待评审 | Bug简报创建功能需求 |
| [仪表盘模块个性化布局PRD](business/仪表盘模块个性化布局PRD.md) | v2.0 | ✅已定稿 | 用户独立仪表盘布局定制 |

---

## API接口编号注册表

| 编号段 | 模块 | 文档 |
|--------|------|------|
| API000500–API000599 | 缺陷增强（变更历史/图片上传） | [功能接口对应关系.md](workflow/功能接口对应关系.md) |
| API000411–API000413 | 仪表盘布局 | [功能接口对应关系.md](workflow/功能接口对应关系.md) |

---

## 研发任务

### 缺陷管理模块（task001–task018）

> [workflow/@缺陷管理/task001-task018.md](workflow/@缺陷管理/task001-task018.md)

| 范围 | 内容 |
|------|------|
| task001 | 产品方案设计&技术分析 |
| task002–005 | 后端枚举/DTO/实体扩展 |
| task006–012 | 后端Mapper/Service/Controller |
| task013–017 | 前端类型定义/API/组件/页面改造 |
| task018 | 联调测试 |

### 仪表盘个性化布局（task001–task007）

> 目录：[workflow/@仪表盘布局/](workflow/@仪表盘布局/)

| Task | 文档 | 说明 |
|------|------|------|
| task001 | [task001-数据库迁移脚本.md](workflow/@仪表盘布局/task001-数据库迁移脚本.md) | dashboard_layout表创建 |
| task002 | [task002-后端基础层开发.md](workflow/@仪表盘布局/task002-后端基础层开发.md) | 后端常量/枚举/Entity/DTO/Mapper |
| task003 | [task003-后端Service和Controller开发.md](workflow/@仪表盘布局/task003-后端Service和Controller开发.md) | 后端Service+Controller |
| task004 | [task004-前端API封装和Store.md](workflow/@仪表盘布局/task004-前端API封装和Store.md) | 前端API封装+Pinia Store |
| task005 | [task005-前端仪表盘重构动态渲染.md](workflow/@仪表盘布局/task005-前端仪表盘重构动态渲染.md) | 前端动态渲染重构 |
| task006 | [task006-前端拖拽交互和编辑模式UI.md](workflow/@仪表盘布局/task006-前端拖拽交互和编辑模式UI.md) | 前端拖拽交互与编辑模式 |
| task007 | [task007-多账号隔离联调测试.md](workflow/@仪表盘布局/task007-多账号隔离联调测试.md) | 多账号数据隔离联调测试 |

---

## 架构与部署

| 文档 | 说明 |
|------|------|
| [workflow/架构分析与问题梳理报告.md](workflow/架构分析与问题梳理报告.md) | 2026-03-12 系统架构现状审查与设计问题清单 |
| [workflow/DEPLOYMENT.md](workflow/DEPLOYMENT.md) | GitHub Actions CI/CD流水线与生产Docker部署说明 |

---

*更多文档请参见 [doc/README.md](../doc/README.md)*
