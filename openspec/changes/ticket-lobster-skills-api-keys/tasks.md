# 实施任务列表：龙虾技能包与个人 API 密钥

## task001 — 需求与文档索引

**目标**：在 `miduo-md/workflow/`（或项目约定的集成文档目录）新增「Agent/龙虾集成」说明：背景、密钥自助流程、安全注意、技能包下载位置。

**要点**：与 `openspec/changes/ticket-lobster-skills-api-keys/proposal.md` 目标与非目标一致；如有 API 编号规范，预留管理接口编号段落。

---

## task002 — 数据库：用户 API 密钥表

**目标**：新增迁移脚本，创建 `user_api_key`（名称以最终实现为准），包含：`id`, `user_id`, `name`, `key_prefix`, `secret_hash`, `scopes`（JSON 可选）, `status`, `last_used_at`, 及项目要求的基础字段。

**要点**：`secret_hash` 非空；`key_prefix` 建议唯一；禁止存明文密钥。

---

## task003 — 领域与持久化层

**目标**：PO、Mapper、基础 CRUD（按 userId 列表、创建、按 id 更新状态、软删或硬删策略按规范）。

**要点**：创建逻辑仅在 Application 层组装哈希与前缀展示。

---

## task004 — 密钥管理 Application 服务

**目标**：`createKey(userId, name, scopes?)` 返回一次性 plaintext + 实体；`listKeys` 脱敏；`disableKey` / `deleteKey` 校验归属；可选 `touchLastUsed`。

**要点**：使用安全随机数生成器；哈希算法与全局 `pepper`（配置）约定一致。

---

## task005 — 密钥管理 REST API（须登录 JWT）

**目标**：提供创建、列表、禁用、删除（及可选更新名称）接口；路径符合项目 REST 约定；Controller 仅编排，校验在 Service。

**要点**：OpenAPI 注解完整；响应体永不返回 `secretHash`；创建响应仅一次含 `apiKey` 明文字段。

---

## task006 — API Key 鉴权过滤器与安全配置

**目标**：解析约定 Header（如 `X-Api-Key` 或 `Authorization: Bearer <api-key>`，需统一文档），校验哈希，构建 `Authentication`，与现有 Spring Security 链路融合。

**要点**：失败不抛 500；与 JWT 无冲突；日志脱敏；可选按 IP 限流（Redis）。

---

## task007 — 业务接口在 Key 身份下可用性

**目标**：选定首批 Agent 可调用的接口（如：分页工单、详情、可用流转动作、执行流转、评论列表/新增），验证在 API Key 身份下与 JWT 用户行为一致且权限正确。

**要点**：禁止绕过工作流引擎；若有「仅管理员」接口，默认不纳入 scope。

---

## task008 — 前端：个人 API 密钥管理页

**目标**：在「个人设置」或「集成设置」下增加密钥列表、创建（展示一次复制框）、禁用、删除；调用 task005 API。

**要点**：提示勿提交 Git；可复制按钮；空状态引导。

---

## task009 — 技能包仓库产物

**目标**：在仓库内固定目录（如 `ticket-platform/docs/lobster-skill/` 或顶层 `artifacts/miduo-ticket-lobster-skill/`）放置 `README.md`、`SKILL.md`、`manifest.json`、`config.example.json`，内容与当前 API 及环境变量一致。

**要点**：打包 zip 的脚本或 CI 步骤可选；版本号与 `manifest.json` 同步。

---

## task010 — 测试与验收

**目标**：关键路径测试：创建密钥 → curl 带 Key 调查询 → 流转 → 禁用后 401；前端创建流程冒烟；若有集成测试框架则补充鉴权单元测试。

**要点**：不使用假密钥提交仓库；测试数据本地或测试库。

---

## 执行顺序

task001 → task002 → task003 → task004 → task005 → task006 → task007 → task008 → task009 → task010

**说明**：task006 与 task007 可并行开发但合并前必须联调；task009 可在 task007 接口稳定后定稿。
