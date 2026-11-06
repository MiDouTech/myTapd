# MTS-3：双轨富文本评论交付说明

任务：https://app.hifox.com/miduo/task/MTS-3

内部备注和公开回复使用同款富文本编辑器，分别保留草稿。历史评论全部保持公开；新增员工评论默认内部，Agent API Key/MCP 强制内部，客户插件只能补充公开信息。没有原地切换评论可见域的接口。

## 权限与图片

- 内部读取和员工写入在数据库实时检查：账号存在、未删除、启用且绑定企业微信。沿用当前系统“有效内部登录人员可访问工单详情”的模型，未引入新的逐工单 ACL。
- 内部图片通过评论 POST 的 HTML data URI 上传，保存于 `ticket_comment.content`，经同一权限检查随详情返回。**没有独立内部图片下载接口、对象存储 key 或 CDN 地址**；公开页、插件、无企微绑定用户的详情 SQL 均只查询 PUBLIC。禁用账号或解除绑定后，旧 JWT 不能继续读取内部图片。
- 内部图片只接受实际 PNG/JPEG/GIF，单张512KB、总计2MB、最多5张、单图1600万像素。外链、SVG、无效图片拒绝；不写入公开附件表。已授权人员复制走图片数据后的传播不属于服务端可撤回范围。
- 内部和新增公开富文本均执行服务端白名单清洗；旧内部纯文本按纯文本显示。保留客户插件原有公开视频。评论请求正文不再落入通用操作日志。
- 公开回复图片仍走原七牛上传链路。测试桶未配置时该外部链路不能验收；不能以本地内嵌图片测试替代七牛上传验收。

## 数据库迁移与历史保留

旧版本存在两份 V54，直接重命名其中一份会使某些部署的 Flyway 历史无法校验。本次不修改原脚本内容、不更新或删除任何已执行的历史行：

| 数据库历史 | 启动时解析的 V54 |
| --- | --- |
| 无 V54（包括空库） | 原始 bug_report_add_resolve_time |
| V54 为 bug_report_add_resolve_time | 同一原始脚本 |
| V54 为 workflow_customization_delete_guard | 同一原始脚本 |
| V54 失败、多条或未知脚本 | 停止，人工核查 |

两份原文件分别移到 `db/legacy-v54/bug-report` 和 `db/legacy-v54/workflow`，仅选择一份加载。新 V72 幂等补齐两种分支的缺失字段/索引；已有完整工作流统计不重算。历史校验和由 Flyway 正常校验，校验失败不能通过自动 repair 掩盖。

真实 MySQL 8 首次迁移还发现原 V57 使用 MariaDB 的 `ADD COLUMN IF NOT EXISTS`，在 MySQL 8 报1064。原文件原样存入 `db/legacy-v57`，仅在数据库已有成功 V57 历史时加载校验；新库使用原 V56 加字段，新 V73 做幂等补齐。已失败的 V57 会阻止启动，必须先核对实际 DDL、备份历史，再由运维按 Flyway 失败迁移流程处理；本次不自动 repair。历史上成功执行 V57 的真实环境尚未提供，本地验证覆盖的是 MySQL 新库及两种 V54 成功历史。

V70 新增 visibility 并将迁移前所有评论标为 PUBLIC，新增行默认 INTERNAL；V71 扩大正文至 MEDIUMTEXT、新增 content_format，历史公开评论标 HTML，已有内部评论保持 PLAIN_TEXT。

## 已执行验证

2026-11-06，本机独立 MySQL Community 8.0.44、Redis 5.0.14.1、JDK8、完整 Spring Boot 后端及 Chrome；未连接生产库。

- `mvn package`：24项测试通过（23项常规测试 + 1项真实 MySQL 多路径集成测试）。MySQL 测试覆盖空库、原 bug-report V54、原 workflow V54 从 V69 升到 V73；逐行比较既有完整历史不变，重跑无变化，故意损坏校验和后阻止启动且不修复历史。
- `scripts/verify-comment-isolation.cjs`：真实 HTTP、JWT、API Key、SQL存取。覆盖有效/未绑定/停用/匿名主体、撤销绑定、内部图片上传读取、公开接口过滤、图片类型校验、内外 HTML 清洗、公开回复、旧纯文本、操作日志无正文。插件使用真实 HMAC 签名签发 LaunchToken，检查公开详情、补充回复/视频及其他用户归属拒绝。
- Chrome 通过实际前端连接上述后端，粘贴私密图片、保存并确认图片解码成功，发布公开回复，匿名公开页没有内部文字或图片；没有模拟 API。
- 前端构建与修改文件 ESLint 已通过；仍有既有 Sass 弃用及 bundle 体积警告。
- 未验证：七牛测试桶上传/读取、真实企微/邮件/Webhook投递、生产版本 Redis 7。内部通知出口隔离有自动化回归测试，但没有声称完成外部渠道投递测试。

### 复现

仅使用专用空测试库与本机服务，禁止把下述脚本指向生产。完整运行前须在本机配置 datasource、Redis、JWT，关闭 Nacos 或配置专用测试命名空间。配置保存在仓库外，不提交秘密。

1. 从 `ticket-platform` 运行 `mvn package`；要启用真实 MySQL 迁移测试，设置 `MYSQL_TEST_URL`、`MYSQL_TEST_USER`、`MYSQL_TEST_PASSWORD`，该账号需能创建 `mts3_migration_*` 专用数据库。测试保留数据库供审计，不删除已有数据库。
2. 以专用配置启动 `ticket-bootstrap/target/ticket-bootstrap-1.0.0-SNAPSHOT.jar`，必须启用 Flyway。使用完整启动配置，不能只对默认 `db/migration` 路径独立运行 Flyway CLI（否则无法选择历史变体）。
3. 从仓库根运行 `node scripts/verify-comment-isolation.cjs`。需要 `MTS3_TEST_DATABASE`（必须 `mts3_` 前缀）、`MTS3_MYSQL_BIN`、`MTS3_MYSQL_PORT`、`MTS3_TEST_JWT_SECRET`、可选 `MTS3_TEST_BASE`（仅允许127.0.0.1）、`MYSQL_TEST_USER` 和 `MYSQL_PWD`。该脚本写入991xxx测试用户和工单，只用于专用测试库，输出不含令牌。
4. 前端配置 `VITE_USE_PROXY=true`、`VITE_API_PROXY_TARGET` 指向本机后端，执行 `npm run dev`。

## 部署步骤（本次不执行）

1. 备份数据库及完整 `flyway_schema_history`；查询 V54/V57 的 script/checksum/success，确认无失败迁移；在生产快照的隔离副本完整演练。若历史脚本内容曾被修改，按原始发布包核验，禁止启动前统一 repair。
2. 补齐七牛测试桶验收；确认网关请求体上限至少容纳3MB评论 JSON，数据库 `max_allowed_packet` 满足该范围。
3. 暂停评论写入及 Agent/工作流/告警写评论，停止旧后端实例。不要滚动混跑：旧实例不认识 visibility，会造成内部正文泄露。
4. `mvn clean package` 和 `npm ci && npm run build` 构建。先启动一个新后端实例，显式启用 Flyway（当前 test profile 默认关闭），验证 V70–V73 成功及 V54/V57 原历史未变，再启动其余同版本实例并同步前端。
5. 以员工、未绑定外部用户、匿名、停用账号、API Key 和插件逐项冒烟；确认公开响应及附件表不含内部图片。验证后恢复写入，观察错误与数据库容量。

## 回滚步骤

- **禁止直接退回不识别 visibility 的旧后端**，禁止删除 visibility/content_format 或把 INTERNAL 改为 PUBLIC。已有内部数据时，旧代码会重新公开全部评论。
- 优先关闭评论写入、回退前端或部署保留可见域过滤的修复版本，保留 V70–V73 结构和历史；不要缩回正文 TEXT，以免截断图片。
- 必须完整恢复迁移前版本时：停所有入口，备份迁移后的数据，将升级后的内部评论隔离保存到仅授权人员可访问的备份，再恢复升级前数据库快照和相配的旧应用。该路径会损失快照后的写入，需按业务恢复流程补偿；不能仅回滚 Git 提交。
- V72/V73 为增加/补齐结构，不做反向 DROP。数据库升级异常保留现场和历史，由运维核对后处理，不自动清理失败记录。
