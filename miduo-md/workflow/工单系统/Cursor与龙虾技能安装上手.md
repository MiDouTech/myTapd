# Cursor 与龙虾 · 工单技能安装上手

> **版本**：v1.0  
> **日期**：2026-04-08  
> **配套**：个人 API 密钥（`X-Api-Key`）、技能包目录 `ticket-platform/docs/lobster-skill/`  
> **关联说明**：[Agent与IDE集成说明.md](./Agent与IDE集成说明.md)

本文面向**第一次**把「米多工单」接到 Cursor 或**龙虾**的同事：先能**拿到密钥**，再能**让 Agent 读到技能说明**，最后用**一句话验证**是否连上工单接口。

---

## 1. 你将得到什么

| 能力 | 说明 |
|------|------|
| 拉待办/列表 | 与 Web「我的工单」等视图一致（`view` 参数与系统枚举一致） |
| 看详情 | 标题、描述、评论、附件元数据等（以接口返回为准） |
| 写评论 / 流转 | 与 Web 同一套工作流，需具备对应权限；建议**先让 Agent 出草稿，你确认再提交** |

**不能替代的事**：Agent 不能替你绕过权限；密钥泄露等于账号泄露，需立即禁用。

---

## 2. 技能包从哪里拿（下载）

技能包**随代码仓库提供**，不在独立 CDN：

- **路径**：`ticket-platform/docs/lobster-skill/`
- **建议用法**：Git 拉取后整目录复制到本机；或从 Git 托管网页进入该路径 **Download / 打包 zip**。
- **目录内文件**：
  - `README.md`：接口与 Header 约定
  - `SKILL.md`：给模型看的触发词与推荐流程（安装到 Cursor Skills 后由 Agent 遵循）
  - `manifest.json`：技能元数据
  - `config.example.json`：本地配置模板（勿提交真实 Key）

> 若公司后续提供「Release zip 或站内下载按钮」，以运维公告为准；当前以**仓库目录**为唯一权威来源。

---

## 3. 第一步：在 Web 创建个人 API 密钥

1. 浏览器登录**工单 Web**（与日常办公相同账号）。
2. 右上角用户菜单 → **个人 API 密钥**（路由：`/account/api-keys`）。
3. 点击 **新建密钥**，填写易识别名称，例如：`Cursor-公司电脑`、`Cloud-Agent-项目A`、`龙虾-测试号`。
4. 在弹出框中 **立即复制** 完整密钥（**只显示一次**）。
5. **不要**把密钥粘贴到工单评论、群聊、Wiki 或 Git 仓库。

更多安全与接口说明见：[Agent与IDE集成说明.md](./Agent与IDE集成说明.md)。

---

## 4. 配置密钥与 Base URL（二选一或同时）

所有调用工单开放接口时，请求需带：

```http
X-Api-Key: <你的完整密钥>
```

`Base URL` 与前端一致，一般为 **`https://<你的域名>/api`**（开发环境可能是 `/api` 代理，以实际部署为准）。

### 方式 A：环境变量（推荐）

在运行 Cursor / Agent / 龙虾 的环境里配置（名称可按团队规范统一）：

```bash
export MIDUO_TICKET_BASE_URL="https://your-host/api"
export MIDUO_TICKET_API_KEY="mdt_xxxxxxxx_xxxxxxxx"
```

**Cursor Cloud / 远程 Agent**：在平台提供的「环境变量 / Secrets」里配置上述两项，**不要**写进仓库里的 `.env` 并提交。

### 方式 B：`config.json`（与技能包同目录）

1. 复制 `config.example.json` 为 **`config.json`**（与 `SKILL.md` 同级或按工具要求的路径）。
2. 填入 `baseUrl`、`apiKey`。
3. 将 **`config.json` 加入 `.gitignore`**，确保永不提交。

---

## 5. Cursor 中如何「安装」技能（通用步骤）

Cursor 不同版本菜单名称可能变化，按下面**原则**操作即可；若与界面不一致，以 Cursor 官方文档为准。

### 5.1 本地 Cursor

1. 将 `ticket-platform/docs/lobster-skill/` **整目录**复制到本机，例如：  
   `~/.cursor/skills/miduo-ticket-lobster-skill/`（具体路径以 Cursor 当前「Skills / Rules」说明为准）。
2. 保证该目录下存在 **`SKILL.md`**（供模型识别触发词与流程）。
3. 在 **项目或用户级** 配置中注入 **环境变量**（见第 4 节），或在对话中**不要**把完整 Key 贴进可被日志采集的公开频道。
4. 新开对话，试用触发词，例如：  
   「按米多工单技能：用环境变量里的 Key 拉我第一页待办工单」。

### 5.2 Cursor Cloud（云开发、少开本地 IDE）

1. 在 **Cloud Agent / 云会话** 的环境变量（Secrets）中配置 `MIDUO_TICKET_BASE_URL`、`MIDUO_TICKET_API_KEY`。
2. 若云环境支持 **上传或挂载 Skills**：将 `lobster-skill` 目录同步到云工作区可读位置，并确保 Agent 能读取 `SKILL.md`。
3. 首次使用建议先 **curl 或单接口探测**（在云端终端执行，Key 用变量引用，勿写死在脚本里进 Git）：

```bash
curl -sS -H "X-Api-Key: $MIDUO_TICKET_API_KEY" \
  "$MIDUO_TICKET_BASE_URL/ticket/page?pageNum=1&pageSize=5&view=<你的视图编码>"
```

4. 再在对话里让 Agent 按 `SKILL.md` 流程拉单。

### 5.3 若团队使用 MCP 封装工单

部分团队会把 HTTP 封装成 **MCP Server**，此时 `SKILL.md` 里可补充「优先调用某某 MCP 工具」；密钥仍建议放在 **MCP 服务端环境变量**，而不是对话里。

---

## 6. 龙虾（Lobster）侧怎么用

「龙虾」指公司内部接入大模型能力、可执行工具链的助手（入口可能是 **企微、网页、独立客户端** 等）。因产品形态不统一，这里给**可落地的共性步骤**：

1. **密钥归属**：每个使用人使用**自己的**个人 API 密钥，与浏览器登录权限一致；禁止多人共用一条 Key。
2. **配置位置**：在龙虾后台或会话初始化参数中配置 `baseUrl` + `apiKey`（或等价 Secrets），由**服务端**带 `X-Api-Key` 调工单，避免在群聊里明文传 Key。
3. **技能内容**：将 `SKILL.md` 的核心段落（触发词、流程、边界）录入龙虾的 **技能 / 提示词模板**，并附上 `ticket-platform/docs/lobster-skill/README.md` 中的路径与 Header 说明。
4. **验证**：在龙虾里问：「查询我名下待处理工单列表第一页」；若返回与 Web 一致，说明链路通了。

具体菜单名称与「安装 zip」流程以**龙虾产品手册**为准；本文只保证与**工单后端契约**一致。

---

## 7. 装好后怎么验收（5 分钟）

| 步骤 | 操作 | 期望 |
|------|------|------|
| 1 | Web 能打开「个人 API 密钥」并看到刚建的记录（脱敏前缀） | 管理面正常 |
| 2 | 用 curl 或 REST 客户端带 `X-Api-Key` 调 `GET .../ticket/page` | 返回 200，数据与本人 Web 视图一致 |
| 3 | Cursor/龙虾 中说触发词，让 Agent **只读**拉列表 | 内容与步骤 2 一致 |
| 4 | （可选）测试写评论前，先让 Agent **展示草稿**，你确认后再提交 | 评论出现在工单详情 |

**失败常见原因**：`Base URL` 少了 `/api`；`view` 拼写与系统枚举不一致；密钥禁用或复制缺字符；公司网络需 VPN。

---

## 8. 推荐对话示例（复制即用）

- 「按米多工单技能：读取 SKILL.md 里的流程，用环境变量调用接口，列出我待办工单前 10 条，只显示单号、标题、状态。」
- 「拉工单详情 ID=12345，总结描述里的复现步骤，不要编造未在接口中出现的内容。」
- 「根据我们刚才讨论的修复内容，生成一条工单评论草稿（根因 + 修改点 + 分支名），我回复确认后再调用评论接口。」

---

## 9. 培训与扩展

- **新人 5 分钟版**：完成本文「第 3 + 4 + 7 节」即可。
- **录屏建议**：录一段「Web 创建密钥 → Cloud 环境贴变量 → 一句对话拉待办」，比纯文档效果好。
- **变更通知**：工单接口或 `view` 枚举变更时，同步更新 `ticket-platform/docs/lobster-skill/README.md` 并通知使用方。

---

## 10. 相关文档

| 文档 | 用途 |
|------|------|
| [Agent与IDE集成说明.md](./Agent与IDE集成说明.md) | 密钥、Header、管理 API、安全 |
| `ticket-platform/docs/lobster-skill/README.md` | 接口路径速查、环境变量名 |
| `ticket-platform/docs/lobster-skill/SKILL.md` | 模型侧流程与边界 |

---

**文档维护**：产品或研发在 Cursor/龙虾入口或环境变量命名变更时，请更新本文第 5、6 节并递增版本号。
