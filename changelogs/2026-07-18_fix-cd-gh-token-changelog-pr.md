# CD changelog 归档 PR 认证修复

| 类型 | 模块 | 描述 |
|---|---|---|
| fix | CI/CD | 归档 changelog 步骤改用 github.token 作为 GH_TOKEN，修复 gh pr create 因未配置 GIT_TOKEN secret 而失败 |
