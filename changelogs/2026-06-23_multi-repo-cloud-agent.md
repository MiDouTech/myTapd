# 多仓库 Cloud Agent 环境配置

| 类型 | 模块 | 描述 |
|---|---|---|
| chore | Cloud Agent | 新增 `.cursor/environment.json` 声明 prd_agent 依赖；启动脚本自动克隆 `/opt/prd_agent`，绕过 Dashboard 多仓库无法同时勾选的问题 |
