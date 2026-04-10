# Agent 与 IDE 集成说明（个人 API 密钥）

> **版本**：v1.0  
> **日期**：2026-04-08  
> **关联变更**：已归档 `openspec/changes/archive/2026-04-08-ticket-lobster-skills-api-keys`  
> **安装培训**：[Cursor与龙虾技能安装上手.md](./Cursor与龙虾技能安装上手.md)

## 背景

研发在 Cursor 等 IDE 中通过「技能 / Agent」拉取本人待办工单、查看详情、补充评论或执行流转时，需要 **不经过浏览器 JWT** 的编程访问方式。系统提供 **个人 API 密钥**，权限边界与对应登录用户一致。

## 密钥获取与保管

1. 使用浏览器登录工单 Web。
2. 右上角用户菜单 → **个人 API 密钥**（路径：`/account/api-keys`）。
3. **新建密钥**，为设备起名（如「公司 Mac」）；创建成功后 **完整密钥仅显示一次**，请立即复制保存。
4. 密钥列表展示 **累计调用次数**（每次 `X-Api-Key` 鉴权成功后异步自增，与「最后使用」同为近似实时，刷新列表即可查看）。
5. **禁止**将密钥写入 Git 仓库、截图外传或提交到技能包 zip。

## 调用方式

- **HTTP 头**：`X-Api-Key: <完整密钥>`
- **与 JWT 关系**：同一请求只需 **JWT（Authorization: Bearer）或 X-Api-Key 其一**；两者同时存在时，**优先 JWT**（有效 JWT 则忽略 `X-Api-Key`）。
- **Base URL**：与前端一致，例如 `https://<host>/api`（以部署环境为准）。

## 密钥管理 API（须登录 JWT）

| 接口编号 | 方法 | 路径 | 说明 |
|----------|------|------|------|
| API000509 | POST | /api/user/api-key/create | 创建密钥 |
| API000510 | GET | /api/user/api-key/list | 列表（脱敏，含 `invocationCount` 累计调用次数） |
| API000511 | PUT | /api/user/api-key/disable/{id} | 禁用 |
| API000512 | DELETE | /api/user/api-key/delete/{id} | 删除 |

## 技能包位置

- **站内下载**：工单 Web → **个人 API 密钥**（`/account/api-keys`）→ **下载技能包**（`lobster-skill.zip`，由前端构建时从下方源目录打包）。
- **仓库源目录**：`ticket-platform/docs/lobster-skill/`（含 `README.md`、`SKILL.md`、`manifest.json`、`config.example.json`）。

**安装与教学**：请阅读 [Cursor与龙虾技能安装上手.md](./Cursor与龙虾技能安装上手.md)（下载路径、Cursor 本地/云、龙虾、验收步骤）。

## 安全注意

- 密钥等同于账号凭证，泄露后请立即在页面 **禁用或删除**。
- 服务端仅存 **BCrypt 哈希**，不存明文。
- 日志中避免输出完整 `X-Api-Key`。

## 验收要点（研发自测）

1. 创建密钥后，使用 `curl -H "X-Api-Key: ..." <base>/api/ticket/page?view=my_todo` 能返回与本人待办一致的数据（需替换 view 为系统实际枚举）。
2. 禁用密钥后，相同请求应返回 401。
3. 列表页仅显示前缀脱敏，不显示完整密钥。
