# Cloud Agent 环境启动脚本说明

本目录下的 `cloud-agent-startup.sh` 用于 Cloud Agent 启动阶段的一次性环境准备，目标是让后续会话可直接执行后端与前端构建命令。

## 脚本能力

脚本会按顺序执行以下动作（均为幂等）：

1. 安装并校验 **Maven 3.8+**（默认安装 3.9.11）。
2. 安装并校验 **JDK8**（默认安装 Temurin 8u442）。
3. 固化环境变量：
   - `JAVA_HOME=/opt/jdk8`
   - `MAVEN_HOME=/opt/apache-maven-<version>`
4. 在 `/workspace/miduo-frontend` 安装前端依赖（仅在 lock 变化时重新安装）。
5. 输出 Java / Maven / Node / npm 版本摘要。

> 说明：脚本会在 `node_modules/.cloud-agent-lock.sha256` 写入 lock 校验值。  
> 如果 `package-lock.json` 未变化，后续会话会跳过 `npm install`，可直接执行 `npm run build`。

## 多仓库（prd_agent）自动克隆

Dashboard 的 **Select multiple** 无法同时勾选 `MiDouTech/myTapd` 与 `inernoro/prd_agent` 时，启动脚本会自动克隆参考仓库：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `PRD_AGENT_DIR` | `/opt/prd_agent` | prd_agent 克隆目标路径 |
| `PRD_AGENT_REPO_URL` | `https://github.com/inernoro/prd_agent.git` | 仓库地址 |
| `PRD_AGENT_BRANCH` | `main` | 分支 |

验证：

```bash
ls /opt/prd_agent
```

## 文件路径

```bash
/workspace/scripts/cloud-agent-startup.sh
```

## 建议作为 Cloud Agent Startup Script 配置

```bash
bash /workspace/scripts/cloud-agent-startup.sh
```

## 仓库内默认会话钩子（已接入）

仓库中的 `.claude/hooks/session-start.sh` 在远程会话启动时会自动执行：

```bash
bash /workspace/scripts/cloud-agent-startup.sh
```

因此在 Claude Code Remote / Cloud Agent 场景下，默认会做一次幂等环境补全。

## 可选参数

```bash
# 指定 Maven 版本
MAVEN_VERSION=3.9.11 bash /workspace/scripts/cloud-agent-startup.sh

# 指定 npm install 参数（默认：--no-audit --no-fund）
NPM_INSTALL_ARGS="--no-audit --no-fund" bash /workspace/scripts/cloud-agent-startup.sh
```

## 完成后可直接执行的验证命令

```bash
# 后端
cd /workspace/ticket-platform && mvn -DskipTests compile

# 前端
cd /workspace/miduo-frontend && npm run build && npm run lint
```
