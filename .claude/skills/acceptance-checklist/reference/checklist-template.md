# {功能名称} 验收清单

> **验收目标**：{一句话说明}
> **分支**：{branch name}
> **关联接口编号**：{API000xxx，可多条}
> **入口**：{后端 URL | 前端 URL | 混合}
> **场景类型**：后端 / 前端 / 混合
> **估算总时长**：{N} 分钟
> **打勾方式**：每个 checkpoint 有 ☐，跑完一项写 ✅ / ❌ / ⏭ (跳过)

---

## 🟢 Phase 0: 前置检查（{N} 分钟）

> 跑测试之前必须满足的环境状态

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 0.1 | `echo $JAVA_HOME` | 输出 `/opt/jdk8` | ☐ |
| 0.2 | `sudo docker ps --format "table {{.Names}}\t{{.Status}}"` | `ticket-mysql` 与 `ticket-redis` 均为 `Up`（健康） | ☐ |
| 0.3 | `curl -s http://localhost:8080/actuator/health \| jq -r .status` | 输出 `UP` | ☐ |
| 0.4 | `curl -sI http://localhost:5173/ \| head -1` | `HTTP/1.1 200 OK` | ☐ |
| 0.5 | `git rev-parse --abbrev-ref HEAD` | 当前分支与本次验收分支一致 | ☐ |

**如果 0.x 失败**：
- `JAVA_HOME` 不对 → 执行 `export JAVA_HOME=/opt/jdk8 && export PATH=$JAVA_HOME/bin:$PATH`
- 容器未启动 → `sudo dockerd &>/tmp/dockerd.log &` 等 5s，再 `sudo docker compose -f /workspace/ticket-platform/deployment/docker/docker-compose.yml up -d`
- 后端 health 不 UP → `tail -100 ticket-platform/logs/app.log`，关注 Flyway / Nacos / DB 连接报错
- 前端不 200 → `cd /workspace/miduo-frontend && npm run dev`
- 仍然不行 → 把 `docker ps` + `tail -50 logs/app.log` 一起贴出来

**验收点 0**：环境冷启动干净，没有上次失败的残留状态。

---

## 🟢 Phase 1: 核心执行（{N} 分钟）

> 真正要测的新功能主路径

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 1.1 | {敲命令 / 点按钮} | {stdout / UI 元素 / 状态码} | ☐ |
| 1.2 | ... | ... | ☐ |

**如果 1.x 失败**：
- 原因 A → 诊断命令
- 原因 B → 排查步骤
- 都不行 → 贴 {具体日志路径 / 浏览器 Network 截图}

**验收点 1**：{一句话说清楚跑这组 checkpoint 是在验什么关键事实}

---

## 🟢 Phase 2: 验证效果（{N} 分钟）

> 检查核心执行是否产生了期望副作用

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 2.1 | `sudo docker exec ticket-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" miduo -e "SELECT id,status,deleted FROM ticket ORDER BY id DESC LIMIT 1"` | 看到刚操作的记录，`deleted=0`、状态符合预期 | ☐ |
| 2.2 | 浏览器刷新列表页 | 看到新数据出现在第一行 | ☐ |
| 2.3 | F12 → Network | `GET /api/xxx/page` 返回 200，`code=200` | ☐ |

**如果 2.x 失败**：
- DB 没有记录但接口 200 → 检查事务是否回滚（grep `Rollback` 后端日志）
- DB 有记录但 UI 没刷新 → 前端是否调用了 `loadList()` 或 store 是否未触发响应式
- 仍然不行 → 贴 {具体日志 / DB 截图}

**验收点 2**：核心动作落到了正确的地方（DB / 缓存 / UI 三者一致）。

---

## 🟢 Phase 3: 回归检查（{N} 分钟）

> 确认老功能没被新改动搞坏

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 3.1 | 用低权限角色（如普通员工）登录前端 | 不能看到本次新增的"管理员菜单" | ☐ |
| 3.2 | 调用历史接口（任选一个老接口）| 仍然 200，响应结构与改动前一致 | ☐ |
| 3.3 | 跑一遍既有的关键流程（如登录→工单列表→详情）| 全程无报错、无 404 | ☐ |

**如果 3.x 失败**：
- 角色权限放错 → 检查 `@PreAuthorize` 或 `authzMenuMapping` 是否新增了越权条目
- 老接口结构变了 → 检查 DTO 字段是否被改名/删除
- 都不行 → 贴老接口的 request/response 对比

**验收点 3**：新功能不破坏老用户的体验。

---

## 🟢 Phase 4: 回滚演练（{N} 分钟，可选）

> 撤销操作也要能工作

| # | 操作 | 预期结果 | 状态 |
|---|---|---|---|
| 4.1 | 撤销 / 删除 / 取消刚才的操作 | 接口 200，DB 中 `deleted=1` 或状态回退 | ☐ |
| 4.2 | 列表页确认数据消失 / 状态回滚 | UI 与 DB 一致 | ☐ |

**如果 4.x 失败**：
- 逻辑删除没生效 → 检查实体是否继承 `BaseEntity` 且字段含 `@TableLogic`
- 状态机回滚被禁止 → 这是预期行为还是 bug？查 `miduo-md/business/` 对应需求文档

**验收点 4**：用户做错操作时能撤回。

---

## 🧾 整体验收汇总

| 验收点 | Phase | 你的结论（✅ / ❌ / ⏭）|
|---|---|---|
| 环境冷启动干净 | 0 | |
| 核心动作可执行 | 1 | |
| 副作用落到正确位置 | 2 | |
| 老功能未受影响 | 3 | |
| 撤销路径可走通 | 4 | |

**任一 ❌**：按对应 Phase 的"失败处置"排查；还不行就贴日志。
**全部 ✅**：功能正式验收通过 → 建议下一步跑 `/handoff` 生成交接文档。
