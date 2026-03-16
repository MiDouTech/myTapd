# Task017：管理端接口封装与联调深化

> **业务模块**：工单系统（前端）  
> **依赖**：Task016  
> **预估工时**：2~3天  
> **对应产品文档**：4.4、4.7、4.8、4.10、4.11；前端架构规范与接口编号管理规范

---

## 一、任务目标

在 Task016 页面可用基础上，完成管理端 API 层、TypeScript 类型层、页面数据层的标准化治理，解决“可用但不稳、可查但难维护”的问题，形成可扩展的前端联调基线。

## 二、实施边界

### 2.1 本 Task 覆盖

- 新增或完善 API 模块（按业务拆分）
- 新增或完善 DTO 类型定义，并与后端 `ticket-entity` 对齐
- 统一分页参数、排序参数、错误处理、空态处理
- 补齐接口注释（接口编号、功能点）
- 管理端页面联调回归（Workflow/SLA/User/Settings）

### 2.2 本 Task 不覆盖

- 新后端接口开发（仅复用已有接口）
- 系统设置高级功能逻辑（Task020）
- 自动化测试体系（Task022）

## 三、详细实施内容

## 3.1 API 模块拆分建议

> 目标：做到“页面无直写 URL，全部通过 api 层调用”。

| 模块 | 建议文件 | 主要接口 |
|---|---|---|
| 工作流管理 | `src/api/workflow.ts` | workflow list/detail、handler-group list/create |
| SLA管理 | `src/api/sla.ts` | policy list/create/update |
| 组织与用户 | `src/api/department.ts`、`src/api/user.ts` | department tree、user list/current |
| 通知中心 | `src/api/notification.ts` | notification page/read/all/unread/preference |
| 集成配置 | `src/api/webhook.ts`、`src/api/wecom.ts` | webhook config CRUD、wecom group binding |

## 3.2 类型层治理

> 目标：页面只依赖 `src/types/*`，不出现 `any` 透传。

- 建议新增/完善：
  - `src/types/workflow.ts`
  - `src/types/sla.ts`
  - `src/types/notification.ts`
  - `src/types/webhook.ts`
  - `src/types/wecom.ts`
  - `src/types/department.ts`
- 统一分页泛型复用 `PageOutput<T>`，避免重复定义。
- 字段命名与后端返回保持一致，必要时在 api 层做转换，不在 UI 层散改。

## 3.3 页面数据层治理

- 统一请求流程：`加载中 -> 成功渲染/空态 -> 失败提示`
- 统一筛选重置行为：点击重置后回到 `pageNum=1`
- 统一表单提交行为：
  - 提交按钮 loading
  - 成功提示 + 刷新列表
  - 失败提示 + 保持弹窗

## 3.4 接口编号与注释校对（前端侧）

- 每个 API 方法注释至少包含：
  - 接口功能中文描述
  - 接口编号（按治理文档最终版）
  - 对应产品功能点
- 对已存在重复编号先保留并标注“待治理”，在 Task021 执行统一纠偏。

## 四、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 管理端 API 模块化封装 | `miduo-frontend/src/api/*.ts` |
| 2 | 管理端类型定义完善 | `miduo-frontend/src/types/*.ts` |
| 3 | 页面数据流统一改造 | `src/views/manage/*.vue` |
| 4 | 联调异常问题清单（含字段差异） | `doc/workflow/test/` 文档 |
| 5 | 接口注释补齐与编号校对记录 | 文档附录或任务说明 |

## 五、实施步骤建议

### Step 1：接口与类型补齐
- 扫描页面中所有请求点，补齐缺失 API 方法与类型
- 将页面内临时对象改为类型化对象

### Step 2：页面联调治理
- Workflow/SLA/User/Settings 四页逐一回归
- 统一分页、筛选、排序行为

### Step 3：文档与注释同步
- 更新 API 注释
- 输出“接口映射表（页面 -> API -> DTO）”

## 六、验收标准

- [ ] 管理端页面不存在硬编码 URL，请求全部通过 `src/api`  
- [ ] 页面不出现 `any` 透传核心业务对象，类型可追踪  
- [ ] 分页/筛选/排序行为一致，用户体验统一  
- [ ] 接口失败可感知、可恢复，不出现静默失败  
- [ ] 管理端联调问题有记录、有归类、有责任归属  

## 七、风险与应对

- **风险1：后端字段演进快导致前端频繁改动**  
  处理：引入“类型映射层”，页面只依赖稳定视图模型。

- **风险2：接口编号短期无法一次性纠正**  
  处理：Task017 先做注释补齐，Task021 做全量治理与校验脚本落地。

- **风险3：多页面并行改造引入回归**  
  处理：每页改造后立即进行手工冒烟（列表/详情/提交/异常）并记录。

## 八、与后续任务衔接

- Task018 在此基础上接入通知中心与实时消息  
- Task019 复用统一 API/类型规范快速落地 Bug 简报页面  
- Task021 对接口编号与文档进行统一治理，消除历史不一致
