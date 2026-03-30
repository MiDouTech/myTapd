# Nacos 配置中心迁移指南

## 概述

本项目已将所有敏感配置从代码仓库**完全移除**，全部由 Nacos 配置中心注入。
采用 **Spring Cloud Alibaba Nacos Config** 方式，通过 `bootstrap.yml` + `shared-configs` 加载，与米多其他微服务项目完全对齐。

## 技术方案

- **依赖**：`spring-cloud-starter-alibaba-nacos-config` + `spring-cloud-starter-bootstrap`
- **版本**：Spring Cloud Alibaba 2021.0.5.0 / Spring Cloud 2021.0.5 / Spring Boot 2.7.18
- **加载方式**：`bootstrap.yml` → `spring.cloud.nacos.config.shared-configs` → Data ID: `ticket-platform-secrets.yaml`
- **命名空间**：与其他项目共用同一 Nacos 命名空间

## bootstrap.yml 配置

```yaml
spring:
  application:
    name: ticket-platform
  cloud:
    nacos:
      config:
        enabled: true
        server-addr: ${NACOS_SERVER_ADDR:http://10.0.4.4:8848}
        file-extension: yaml
        namespace: ${NACOS_NAMESPACE:e4479836-f77e-4b46-9e96-56179bdd6875}
        shared-configs:
          - data-id: ticket-platform-secrets.yaml
            group: DEFAULT_GROUP
            refresh: true
```

## Nacos 操作步骤

### 1. 在 Nacos 控制台创建配置

1. 打开 Nacos 控制台 `http://10.0.4.4:8848/nacos`
2. 切换到对应的命名空间（namespace: `e4479836-f77e-4b46-9e96-56179bdd6875`）
3. 新建配置：
   - **Data ID**：`ticket-platform-secrets.yaml`
   - **Group**：`DEFAULT_GROUP`
   - **配置格式**：`YAML`
   - **配置内容**：粘贴对应环境的配置内容

### 2. 启动应用

```bash
cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=dev \
  -Djdk.tls.client.protocols=TLSv1.2
```

### 3. 切换 Nacos 环境

通过环境变量覆盖 `bootstrap.yml` 中的默认值：

```bash
# 切换到其他 Nacos 实例或命名空间
export NACOS_SERVER_ADDR=http://other-nacos:8848
export NACOS_NAMESPACE=other-namespace-id
```

## Nacos 中的配置

| Data ID | Group | 说明 |
|---------|-------|------|
| `ticket-platform-secrets.yaml` | DEFAULT_GROUP | 工单系统全部敏感配置 |

## 本地配置 vs Nacos 配置

| 配置项 | 位置 | 说明 |
|--------|------|------|
| 连接池参数、日志级别、Flyway 开关 | `application-{profile}.yml` | 非敏感 |
| 数据库/Redis/JWT/七牛/企微/SSO | **Nacos** | 敏感 |
