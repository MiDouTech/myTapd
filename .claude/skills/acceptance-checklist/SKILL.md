---
name: acceptance-checklist
description: 生成给真人手动执行的用户验收测试（UAT）清单：每个 checkpoint 都列出"敲什么命令 / 点哪个按钮 / 应该看到什么 / 看不到时按什么键排错"，覆盖后端接口、前端页面与混合场景，支持冷启动注入（Docker / MySQL / Redis / Nacos）和打勾驱动的进度跟踪。区别于 `/smoke`（curl 冒烟）和 `/handoff`（交接文档），本技能面向"亲手操作系统的真人"。触发词："/uat"、"验收清单"、"真人验收"、"acceptance test"、"让我验收"。
---

# Acceptance Checklist — 真人验收清单

给**即将亲手操作米多工单系统的真人**用的结构化打勾清单。不是代码审查，不是 curl 冒烟——是"按这张表操作，每一步都告诉我应该看到什么、看不到时按哪个键排错"。

## 目录

- [核心理念](#核心理念)
- [何时用 / 何时别用](#何时用--何时别用)
- [执行流程](#执行流程)
- [UAT 文档模板](#uat-文档模板)
- [双通道支持：后端 / 前端](#双通道支持后端--前端)
- [交互模式：逐步打勾](#交互模式逐步打勾)
- [失败处置手册](#失败处置手册)
- [上下游技能](#上下游技能)
- [项目特定合规](#项目特定合规)

## 核心理念

**四个不妥协**：

1. **真人视角，不是代码视角**：每步动作必须是"敲这条命令 / 点这个按钮"，不是"调用这个函数"
2. **可打勾可回溯**：每个 checkpoint 有 ☐ 符号，跑完一项打 ✅ / ❌ / ⏭
3. **失败不留悬案**：任何 ❌ 都必须有对应的 `failurePlaybook`，不能光说"失败了"
4. **禁止空白等待**：长操作必须有进度反馈（编译进度、Vite HMR、Spring Boot banner），不是让用户盯着黑屏猜

## 何时用 / 何时别用

### ✅ 该用

- 一项功能后端接口已写完、前端页面已联调、单元测试通过，准备交给真人跑一遍
- 涉及用户操作路径：管理后台菜单 / 工单列表 / 工单详情弹窗 / 审批流转
- 新增模块（CRM、商品、SSO、工单流转）/ 新增页面 / 新增 SQL 迁移脚本
- PR 合并前的最终确认（比 `/handoff` 更细粒度，要真动手跑）
- 涉及 docker-compose 起服务、Nacos 配置、Flyway 迁移这类"环境敏感"变更

### ❌ 别用

- 纯库函数 / Util 重构（没有真人入口）→ 用 `/verify` 做代码审查
- 仅 Controller + Service 的 API 契约验证（没有 UI 联动）→ 用 `/smoke` 做 curl 链式冒烟
- 纯文档 / SQL 注释变更 → 不需要 UAT

### 与其他技能的关系

| 维度 | `/verify` (human-verify) | `/smoke` (smoke-test) | `/handoff` (task-handoff-checklist) | **`/uat` (本技能)** |
|---|---|---|---|---|
| 视角 | 代码 reviewer | 开发者 | 移交文档作者 | **真人用户** |
| 产出 | 问题清单 | curl 链 | 交接报告 | **打勾清单 + 排错手册** |
| 何时用 | 写完代码 | 写完接口 | 提交 PR 前 | **真人动手前** |
| 自动 / 手动 | AI 自动 | AI 自动 | AI 自动 | **半自动**：AI 生成清单，真人执行，AI 收反馈打勾 |

## 执行流程

```
UAT 生成进度：
- [ ] Phase A: 场景收集（问 3 个关键问题）
- [ ] Phase B: 冷启动注入判定（涉及容器 / Nacos / Flyway 时强制加）
- [ ] Phase C: 分阶段清单生成（0 准备 → 1 执行 → 2 验证 → 3 回归 → 4 回滚）
- [ ] Phase D: 失败处置路径逐项填写
- [ ] Phase E: 输出 Markdown 打勾表 + 整体验收汇总
- [ ] Phase F: 串联下游技能建议
```

### Phase A: 场景收集（必须先问）

调用本技能时必须先收集以下 3 条信息（用户没说齐，就追问，不猜）：

| 必填项 | 示例 |
|---|---|
| **功能描述**：本次验收的是什么 | "工单创建：员工提工单 → 经理审批 → 工单分派给客服" |
| **入口**：真人从哪开始操作 | 后端：`http://localhost:8080/api/ticket/create` / 前端：`http://localhost:5173/ticket/list` |
| **场景类型**：后端 / 前端 / 混合 | 混合（先前端创建工单，再用 jwt 调后端审批接口） |

**可选项**（有更好，没有 AI 自己推断）：
- 预期耗时估算
- 依赖的外部服务（MySQL / Redis / Nacos / Qiniu OSS / 企业微信 OAuth）
- 已知风险点（从 `/risk` 或 `/verify` 继承）
- 对应的接口编号（参见 `api-interface-numbering.mdc`，如 API000123）

### Phase B: 冷启动注入判定

**强制触发条件**：场景描述命中以下任一关键词时，必须在 Phase 0 之前插入"冷启动自检"：

| 触发词 | 冷启动动作 |
|---|---|
| Docker / 容器 / docker-compose | `sudo docker ps`、`sudo docker logs ticket-mysql --tail 30` |
| MySQL / 数据库 / Flyway | `sudo docker exec ticket-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES"` |
| Redis / 缓存 | `sudo docker exec ticket-redis redis-cli PING` |
| Nacos / 配置中心 | 检查 Nacos 控制台是否能拉到 `ticket-platform-dev.yml` |
| Spring Boot / 后端 | `curl -s http://localhost:8080/actuator/health \| jq` 或访问 `/v3/api-docs` |
| Vite / 前端 | `curl -sI http://localhost:5173/ \| head -1` 应为 `HTTP/1.1 200` |
| JWT / 鉴权 | 用 Nacos 中 `jwt.secret` 生成测试 token，确认 `/api/user/me` 不 401 |

**原因**：真人经常在"上次失败的残留状态"上测试，测出来是假阳性。冷启动是最容易省略、最致命的一步。

### Phase C: 分阶段清单生成

每个 UAT 文档必须包含以下 5 个 Phase（某阶段为空时明确写"本场景无此阶段"）：

| Phase | 含义 | 典型内容 |
|---|---|---|
| **Phase 0: 前置检查** | 跑测试前必须满足的环境 | JDK 8、`JAVA_HOME=/opt/jdk8`、容器全绿、前端 `npm install` 已执行 |
| **Phase 1: 核心执行** | 真正要测的新功能主路径 | 敲命令 / 点按钮 / 等结果 |
| **Phase 2: 验证效果** | 检查核心执行是否产生了期望副作用 | DB 落盘（`SELECT * FROM ticket WHERE id=...`）、UI 刷新、日志出现关键字 |
| **Phase 3: 回归检查** | 确认老功能没被新改动搞坏 | 既有页面还能访问 / 老接口还能调 / 老角色权限不变 |
| **Phase 4: 回滚演练** | 撤销操作也要能工作 | 撤销审批 / 删除工单 / 回滚 Flyway 迁移（生产慎用） |

每个 Phase 内部的行项必须遵循 [UAT 文档模板](#uat-文档模板) 的表格结构。

### Phase D: 失败处置路径（failurePlaybook）

每个 checkpoint **必须**附加一个 `failurePlaybook` 字段——"预期结果没出现时，按这个手册排查"。格式：

```
**如果 X.Y 失败**：
- 常见原因 1 → 诊断命令 / 修复步骤
- 常见原因 2 → 诊断命令 / 修复步骤
- 仍然不行 → 贴这几条日志给开发者
```

留一条"仍然不行"兜底：真人遇到非预期情况知道去哪求助，不会卡死。

### Phase E: 输出格式

见 [UAT 文档模板](#uat-文档模板)。

### Phase F: 串联下游技能

UAT 完成后，根据结果提示下一步技能：

- **全绿通过** → 建议 `/handoff` 生成交接文档
- **部分失败** → 建议 `/verify` 复查代码 或 `/risk` 重评风险
- **回归检查发现老功能坏了** → 建议 `/trace`（flow-trace）追数据流
- **反复出现冷启动相关问题** → 建议把场景写进 `miduo-md/workflow/test` 长期沉淀

## UAT 文档模板

**文件名**：`acceptance-{feature-slug}-{YYYYMMDD}.md`（**默认不写盘**，只打印到对话；用户明确说"保存"时才写到 `miduo-md/workflow/test/` 或 `miduo-md/todo/test-todo/`）

**顶部元信息**：

```markdown
# {功能名称} 验收清单

> **验收目标**：{一句话说明}
> **分支**：{branch name}
> **关联接口编号**：{API000xxx，可多条}
> **入口**：{后端 URL | 前端 URL | 混合}
> **场景类型**：后端 / 前端 / 混合
> **估算总时长**：{N} 分钟
> **打勾方式**：每个 checkpoint 有 ☐，跑完一项写 ✅ / ❌ / ⏭ (跳过)
```

**每个 Phase 的表格结构**：

```markdown
## 🟢 Phase N: {阶段名}（{预计耗时}）

> {可选的一句话说明：这个阶段在验什么}

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| N.1 | {具体命令或点击动作} | {看到什么 stdout / UI 元素 / 状态码} | ☐ |
| N.2 | ... | ... | ☐ |

**如果 N.1 失败**：
- 原因 A → 诊断命令
- 原因 B → 排查步骤
- 都不行 → 贴 `tail -100 ticket-platform/logs/app.log` 或浏览器 Network 面板截图给我

**验收点 N**：{这个 Phase 的核心价值判断，一句话说清楚跑这组 checkpoint 是在验什么关键事实}
```

**文末总结**：

```markdown
## 🧾 整体验收汇总

| 验收点 | Phase | 你的结论（✅ / ❌ / ⏭）|
|---|---|---|
| {point 1} | 1 | |
| {point 2} | 2 | |

**任一 ❌**：按对应 Phase 的"失败处置"排查；还不行就贴日志
**全部 ✅**：功能正式验收通过 → 建议下一步跑 `/handoff`
```

完整模板见 [reference/checklist-template.md](reference/checklist-template.md)。

## 双通道支持：后端 / 前端

UAT 往往是混合场景，清单必须对两种通道都有标准化的表述。

### 后端通道（Spring Boot）

```markdown
| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 1.1 | `curl -X POST http://localhost:8080/api/ticket/create -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d @ticket.json` | HTTP 200，响应体 `code=200`、`data.id` 为正整数 | ☐ |
| 1.2 | `sudo docker exec ticket-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" miduo -e "SELECT id,status FROM ticket ORDER BY id DESC LIMIT 1"` | 看到刚创建的工单，`status=PENDING`、`deleted=0` | ☐ |
```

**预期结果三要素**（任选其一或组合）：
- HTTP 状态码 + `code` 字段（统一返回结构 `CommonResult`）
- DB 副作用（`SELECT ... ORDER BY id DESC LIMIT 1`，注意逻辑删除字段 `deleted=0`）
- 日志关键字（`tail -f` 后端日志看 `INFO` / `ERROR`）
- Redis 副作用（`redis-cli GET ticket:cache:xxx`）

### 前端通道（Vue 3 + Element Plus + Vite）

```markdown
| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 2.3 | 浏览器打开 `http://localhost:5173/ticket/list` | 看到顶部标题"工单管理" + 默认 20 条数据，分页"共 N 条" | ☐ |
| 2.4 | 点击"新建工单"按钮 | 弹出 Element Plus Dialog，含必填项红星标记 | ☐ |
| 2.5 | 按 F12 打开 DevTools → Network | 有一条 `GET /api/ticket/page?pageNum=1&pageSize=20` 返回 200 | ☐ |
| 2.6 | 在表格中点击"编辑"列 | 跳转 `/ticket/edit/{id}`，URL 中 id 与列表行 id 一致 | ☐ |
```

**前端通道强制项**：
- 不能只调 API，必须真的开浏览器看渲染
- F12 检查 Network（API 200 ≠ UI 渲染正确）
- 分页组件需符合 `frontend-ui-standards.mdc`：默认 20 条 / `[10,20,50,100]` / 显示"共 N 条" / 带 `background`
- 表格列对齐居中、操作列固定右侧
- 主色调 `#1675d1`（米多蓝）目视一致

### 混合场景示范

```
Phase 1（前端）：用浏览器创建工单
Phase 2（后端）：curl 取 token，调审批接口
Phase 3（前端）：刷新列表看到状态由 PENDING → APPROVED
Phase 4（后端）：DB 直查 status_history 表，确认有审批日志
```

## 交互模式：逐步打勾

**半自动执行**（默认，推荐）：

1. AI 打印完整清单给用户（顶部总览 + 所有 Phase）
2. 用户自己按顺序操作，**每完成一个 Phase 在对话里回复**：
   - `pass 1.1-1.5` → 标记 1.1-1.5 为 ✅
   - `fail 2.3` + 错误信息 → AI 查 failurePlaybook 给建议
   - `skip 4.*` → Phase 4 全部跳过
3. AI 维护进度面板，每收到一次反馈就更新汇总表

**自动监督模式**（用户明确请求）：

如果用户说"你帮我一步一步执行"：
- AI 一次只打印一步
- 用户执行完回 `y`（通过）/ `n 错误信息`（失败）
- 失败时 AI 给排查建议，用户尝试修复后回 `retry`
- 禁止 AI 一次性 dump 全部清单（会让用户跟丢）

**关键字自动分级**：

| 用户回复包含 | 自动分级 |
|---|---|
| "crash" / "炸了" / "500" / "502" / "timeout" / "空指针" | 🔴 blocker |
| "慢" / "等了很久" / "loading 转圈" | 🟡 minor |
| "样式不对" / "布局乱了" / "颜色不是米多蓝" | 🟡 minor（UI polish）|
| "不符合预期" / "字段不对" / "权限放错了" | 🔴 blocker |
| "看到了" / "对的" / "通过" / "OK" | ✅ pass |

## 失败处置手册

每个 failurePlaybook 必须同时覆盖三类失败：

1. **预期失败**：文档里列的常见错误 → 给确定的修复命令
2. **环境失败**：依赖没装 / 端口占用 / `JAVA_HOME` 没指到 jdk8 → 给探活命令
3. **未知失败**：日志 + 贴给开发者 → 给明确的日志抓取命令

**绝不能写**：
- "失败就重试"（空话）
- "看日志"（不告诉看哪里）
- "问一下管理员"（不告诉问什么）

**必须写**（示例）：
- "如果 401：用 Nacos 控制台查 `jwt.secret`，再用 `https://jwt.io` 生成 token，确认 `exp` 字段未过期"
- "如果 Flyway 报 checksum mismatch：贴 `mvn flyway:info` 输出，并确认是否手改过历史 V1-V9 脚本"
- "如果前端 401 但后端日志没拒绝：F12 → Network → 看 Request Headers 是否带 `Authorization`，可能是 axios 拦截器没注入"

## 上下游技能

```
       /verify  → 代码审查通过
              ↓
       /smoke   → API 契约冒烟通过
              ↓
       (前后端起服务并联调)
              ↓
   → /uat ← 本技能（真人打勾验收）
              ↓
       /handoff → 生成交接文档
              ↓
              PR
```

### 不是替代而是组合

- `/smoke` 告诉你 API 接口是**技术正确**的（curl 链式冒烟，编号链路打通）
- `/uat` 告诉你从**真人体验**看是**业务正确**的（角色权限、UI 细节、回归不坏）
- 两者都过 = 可以合 PR
- `/smoke` 过但 `/uat` 没过 = API 对但用户用不上（前端没接 / 权限放错 / UI 错位）
- `/uat` 过但 `/smoke` 没过 = 偶发的测试未覆盖路径

## 项目特定合规

- **中文输出**：所有 checkpoint 描述和错误提示必须中文
- **不写盘默认**：生成的 UAT 文档默认只打印到对话。仅当用户明确说"保存"时才写到：
  - `miduo-md/todo/test-todo/`（测试用例文档，参见 `project-structure.mdc` 第 19 条）
  - `miduo-md/workflow/test/`（执行情况和质量报告，参见 `project-structure.mdc` 第 20 条）
- **接口编号必须**：每个 checkpoint 涉及后端接口时，在描述里挂上接口编号（API000xxx），与 `miduo-md/workflow/功能接口对应关系.md` 一致
- **真实数据**：参见 `project-structure.mdc` 第 23 条 — UAT 必须用真实数据，禁止 mock
- **角色覆盖**：工单系统涉及多角色（员工 / 经理 / 客服 / 管理员），每个 Phase 至少声明使用哪个角色的 token；权限相关变更必须额外加一个"用低权限角色再跑一次"的 checkpoint
- **JDK 8 提醒**：Phase 0 必须包含 `echo $JAVA_HOME` 应为 `/opt/jdk8`（参见 `AGENTS.md`）
- **数据库回滚谨慎**：Phase 4 涉及 Flyway 回滚时，必须明确"仅 dev 环境"，禁止在 prod 上跑

## 灵感来源 & 致谢

- 借鉴自 [inernoro/prd_agent](https://github.com/inernoro/prd_agent) 的 `acceptance-checklist` 技能（GSD 社区 `verify-work` 模式的中文落地版）
- 本地化要点：移除 CDS 集群、Capsule 执行器等强耦合内容，替换为本仓 Spring Boot + Vue 3 + Docker Compose + Nacos 的真实入口
