# Nacos 配置中心迁移 - 设计方案

## 方案概要

采用 `nacos-config-spring-boot-starter` 集成 Nacos 配置中心，通过 `bootstrap.yml` 在 Spring Boot 启动前加载 Nacos 中的敏感配置，替代原本写死在 `application-*.yml` 中的凭据。

## 技术选型

- **集成方式**：`nacos-config-spring-boot-starter 0.2.12`（兼容 Spring Boot 2.7.18）
- **配置格式**：YAML
- **命名空间**：按环境隔离（dev / test / prod）
- **Data ID**：`ticket-platform-secrets.yml`（各环境敏感配置统一命名）
- **Group**：`DEFAULT_GROUP`

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|--------|------|
| `ticket-platform/pom.xml` | 新增依赖 | 添加 nacos-config-spring-boot-starter |
| `ticket-bootstrap/pom.xml` | 新增依赖 | 添加 nacos-config-spring-boot-starter |
| `application-dev.yml` | 修改 | 移除敏感信息，改为占位符 |
| `application-test.yml` | 修改 | 移除敏感信息，改为占位符 |
| `application-prod.yml` | 修改 | 移除敏感信息，改为占位符 |
| `bootstrap.yml` | 新增 | Nacos 连接配置 |
| `bootstrap-dev.yml` | 新增 | dev 环境 Nacos 地址 |
| `bootstrap-test.yml` | 新增 | test 环境 Nacos 地址 |
| `bootstrap-prod.yml` | 新增 | prod 环境 Nacos 地址 |

## 关键决策

1. **使用 bootstrap.yml 而非 application.yml 配置 Nacos**：确保在 Spring Context 加载前完成配置拉取
2. **敏感配置全部迁移到 Nacos**：数据库、Redis、JWT、七牛云、企微、SSO 凭据
3. **Nacos 连接信息支持环境变量覆盖**：便于容器化部署
4. **保留 application-*.yml 中的非敏感配置**：如连接池参数、日志级别等

## 迁移到 Nacos 的配置清单

| 配置项 | 所属模块 | 敏感级别 |
|--------|---------|---------|
| spring.datasource.url | 数据库 | 中 |
| spring.datasource.username | 数据库 | 中 |
| spring.datasource.password | 数据库 | 高 |
| spring.redis.host | Redis | 中 |
| spring.redis.port | Redis | 低 |
| spring.redis.password | Redis | 高 |
| jwt.secret | JWT | 高 |
| qiniu.access-key | 七牛云 | 高 |
| qiniu.secret-key | 七牛云 | 高 |
| qiniu.bucket | 七牛云 | 中 |
| qiniu.domain | 七牛云 | 低 |
| wecom.corp-id | 企微 | 中 |
| wecom.agent-id | 企微 | 中 |
| wecom.secret | 企微 | 高 |
| wecom.contact-secret | 企微 | 高 |
| wecom.callback-token | 企微 | 高 |
| wecom.callback-aes-key | 企微 | 高 |
| miduo.sso.app-code | SSO | 中 |
| miduo.sso.app-secret | SSO | 高 |
| miduo.sso.base-url | SSO | 低 |
| miduo.sso.shortcut-id | SSO | 低 |
| miduo.sso.redirect-uri | SSO | 低 |

## 回滚策略

- 移除 `bootstrap*.yml`，在 `application-*.yml` 中还原敏感配置即可回滚
- 移除 pom.xml 中的 nacos 依赖
