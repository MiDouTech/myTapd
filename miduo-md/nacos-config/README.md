# Nacos 配置中心迁移指南

## 概述

本项目已将所有敏感配置从代码仓库**完全移除**，全部由 Nacos 配置中心注入。
采用 **Spring Cloud Alibaba Nacos Config**，通过 `bootstrap.yml` 连接 Nacos，按 profile 自动加载对应 Data ID。

## 技术方案

| 项目 | 值 |
|------|-----|
| 依赖 | `spring-cloud-starter-alibaba-nacos-config` + `spring-cloud-starter-bootstrap` |
| 版本 | Spring Cloud Alibaba 2021.0.5.0 / Spring Cloud 2021.0.5 / Spring Boot 2.7.18 |
| Nacos 地址 | 由环境变量 `NACOS_SERVER_ADDR` 指定 |
| 命名空间 | 由环境变量 `NACOS_NAMESPACE` 指定 |
| 敏感配置 Data ID | `ticket-platform-secrets.yaml`（`bootstrap.yml` 中 `shared-configs` 固定拉取，与 profile 无关） |

## Data ID 命名规则

Spring Cloud Alibaba 自动按 `spring.application.name` + `spring.profiles.active` 拼接主配置 Data ID；**数据库密码、Redis、JWT、企微等敏感项**放在 **`ticket-platform-secrets.yaml`**（与本仓库 `miduo-md/nacos-config/ticket-platform-secrets-*.yml` 模板对应），由 `bootstrap.yml` 的 `shared-configs` 在启动前一并加载。

| Profile | 主配置 Data ID | 说明 |
|---------|---------|------|
| test | `ticket-platform-test.yaml` | 测试环境非敏感结构配置 |
| prod | `ticket-platform-prod.yaml` | 生产环境非敏感结构配置 |
| (无) | `ticket-platform.yaml` | 公共配置（可选） |
| 共用 | `ticket-platform-secrets.yaml` | 敏感配置（**必须**在 Nacos 中存在且含 `spring.datasource.url` 等） |

**YAML 注意**：同一文件中**禁止**出现两个顶级 `spring:` 键；后者会覆盖前者，导致数据源等配置丢失（表现为启动报错 `Failed to determine suitable jdbc url`）。邮件 `spring.mail` 应写在**同一个** `spring:` 节点下。

## Nacos 操作步骤

### 1. 在 Nacos 控制台创建配置

1. 打开 Nacos 控制台（地址由运维配置提供）
2. 切换到对应环境的命名空间
3. 新建或更新 **`ticket-platform-secrets.yaml`**（敏感项，**必选**）：
   - **Data ID**：`ticket-platform-secrets.yaml`
   - **Group**：`DEFAULT_GROUP`
   - **配置格式**：`YAML`
   - **配置内容**：从本仓库 `miduo-md/nacos-config/ticket-platform-secrets-test.yml`（或 dev/prod 模板）复制并替换占位符
4. 新建主配置（可选，可与本地 `application-*` 互补）：
   - **Data ID**：`ticket-platform-test.yaml`（或 `ticket-platform-prod.yaml`）
   - **Group**：`DEFAULT_GROUP`
   - **配置格式**：`YAML`
   - **配置内容**：非敏感结构参数（可为空，但 secrets 中必须有 JDBC URL）

### 2. 启动应用

```bash
# test 环境 → 自动加载 ticket-platform-test.yaml
cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=test \
  -Djdk.tls.client.protocols=TLSv1.2

# prod 环境 → 自动加载 ticket-platform-prod.yaml
cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=prod \
  -Djdk.tls.client.protocols=TLSv1.2
```

## 切换 Nacos 地址

通过环境变量覆盖 `bootstrap.yml` 中的默认值：

```bash
export NACOS_SERVER_ADDR=http://other-nacos:8848
export NACOS_NAMESPACE=other-namespace-id
```
