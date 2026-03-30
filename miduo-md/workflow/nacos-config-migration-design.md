# Nacos 配置中心迁移 - 设计方案

## 方案概要

采用 **Spring Cloud Alibaba Nacos Config** 集成 Nacos 配置中心，通过 `bootstrap.yml` + `shared-configs` 在 Spring Context 加载前拉取 Nacos 中的敏感配置。与米多其他微服务项目使用完全相同的 Nacos 实例和命名空间。

## 技术选型

- **集成方式**：`spring-cloud-starter-alibaba-nacos-config` + `spring-cloud-starter-bootstrap`
- **版本**：Spring Cloud Alibaba 2021.0.5.0 / Spring Cloud 2021.0.5
- **配置格式**：YAML
- **命名空间**：与其他项目共用（namespace: `e4479836-f77e-4b46-9e96-56179bdd6875`）
- **Data ID**：`ticket-platform-test.yaml`（test）/ `ticket-platform-prod.yaml`（prod）
- **Group**：`DEFAULT_GROUP`

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|--------|------|
| `ticket-platform/pom.xml` | 修改 | 添加 Spring Cloud + Spring Cloud Alibaba BOM |
| `ticket-bootstrap/pom.xml` | 修改 | 添加 nacos-config + bootstrap starter |
| `bootstrap.yml` | 新增 | Nacos 连接和 shared-configs 配置 |
| `application.yml` | 修改 | 移除旧 nacos 配置段 |
| `application-dev.yml` | 修改 | 移除所有敏感配置，只留结构性参数 |
| `application-test.yml` | 修改 | 移除所有敏感配置，只留结构性参数 |
| `application-prod.yml` | 修改 | 移除所有敏感配置，只留结构性参数 |

## 关键决策

1. **使用 Spring Cloud Alibaba 而非 nacos-config-spring-boot-starter**：与其他微服务项目技术栈统一
2. **共用命名空间**：多个项目在同一 Nacos 实例同一命名空间下，通过不同 data-id 区分
3. **敏感配置完全从 yml 中删除**：不保留占位符，代码零泄露
4. **bootstrap.yml 加载**：确保数据库等配置在 Spring Context 初始化前可用

## 回滚策略

- 删除 `bootstrap.yml`
- 将 pom.xml 中 Spring Cloud Alibaba 依赖移除
- 在 `application-*.yml` 中还原敏感配置
