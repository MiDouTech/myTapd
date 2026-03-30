# Nacos 配置中心迁移 - 变更需求

## 背景

项目准备开源，当前 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中包含大量敏感信息（数据库密码、Redis 密码、JWT 密钥、七牛云密钥、企微密钥、SSO 密钥等），需要将这些敏感配置迁移到 Nacos 配置中心，避免在代码仓库中暴露。

## 现状问题

1. 数据库密码明文写在 yml 配置文件中
2. Redis 密码明文写在 yml 配置文件中
3. JWT 签名密钥硬编码在配置文件和 Java 类默认值中
4. 七牛云 AK/SK 以默认值形式写在配置文件中
5. 企业微信 corpId/secret/callback 凭据明文写在配置文件中
6. 米多 SSO appSecret 明文写在配置文件中

## 目标

- 将所有敏感配置迁移到 Nacos 配置中心
- 本地配置文件只保留非敏感的通用配置和占位符
- 代码通过 `bootstrap.yml` 连接 Nacos 拉取敏感配置
- 提供完整的迁移脚本和操作文档，支持快速迁移

## 非目标

- 不引入 Nacos 服务发现（项目为单体架构）
- 不变更现有业务逻辑和对外接口
- 不变更前端配置

## 验收标准

1. 本地配置文件中不再包含任何真实的敏感凭据
2. 项目通过 `bootstrap.yml` 连接 Nacos 获取敏感配置
3. 所有环境（dev/test/prod）均有对应的 Nacos 配置文件
4. 提供一键导入 Nacos 的脚本
5. 项目可以正常编译和启动
