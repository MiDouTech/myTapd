# Nacos 配置中心迁移指南

## 概述

本项目已将所有敏感配置从代码仓库**完全移除**，全部由 Nacos 配置中心注入。
采用 **Spring Cloud Alibaba Nacos Config**，通过 `bootstrap.yml` 连接 Nacos，按 profile 自动加载对应 Data ID。

## 技术方案

| 项目 | 值 |
|------|-----|
| 依赖 | `spring-cloud-starter-alibaba-nacos-config` + `spring-cloud-starter-bootstrap` |
| 版本 | Spring Cloud Alibaba 2021.0.5.0 / Spring Cloud 2021.0.5 / Spring Boot 2.7.18 |
| Nacos 地址 | `http://10.0.4.4:8848` |
| 命名空间 | `e4479836-f77e-4b46-9e96-56179bdd6875` |

## Data ID 命名规则

Spring Cloud Alibaba 自动按 `spring.application.name` + `spring.profiles.active` 拼接 Data ID：

| Profile | Data ID | 说明 |
|---------|---------|------|
| test | `ticket-platform-test.yaml` | 测试环境 |
| prod | `ticket-platform-prod.yaml` | 生产环境 |
| (无) | `ticket-platform.yaml` | 公共配置（可选） |

## Nacos 操作步骤

### 1. 在 Nacos 控制台创建配置

1. 打开 `http://10.0.4.4:8848/nacos`
2. 切换到命名空间 `e4479836-f77e-4b46-9e96-56179bdd6875`
3. 新建配置：
   - **Data ID**：`ticket-platform-test.yaml`（或 `ticket-platform-prod.yaml`）
   - **Group**：`DEFAULT_GROUP`
   - **配置格式**：`YAML`
   - **配置内容**：粘贴对应环境的配置

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
