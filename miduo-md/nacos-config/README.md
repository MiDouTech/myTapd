# Nacos 配置中心迁移指南

## 概述

本项目已将所有敏感配置（数据库密码、Redis 密码、JWT 密钥、七牛云密钥、企微凭据、SSO 密钥等）从代码仓库迁移至 Nacos 配置中心。代码仓库中只保留非敏感的通用配置和环境变量占位符。

## 前置条件

- 已部署 Nacos Server（2.x 版本推荐）
- 确保应用服务器可访问 Nacos 服务地址

---

## 快速开始（3 步完成迁移）

### 第 1 步：填写配置文件

复制对应环境的模板文件，将 `<YOUR_xxx>` 占位符替换为真实值：

```bash
cd miduo-md/nacos-config/

# 以 dev 环境为例
cp ticket-platform-secrets-dev.yml ticket-platform-secrets-dev.yml.local
# 编辑文件，填入真实的敏感配置值
vi ticket-platform-secrets-dev.yml.local
```

**需要填写的配置项：**

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `<YOUR_DB_PASSWORD>` | 数据库密码 | `MyDbPass123` |
| `<YOUR_REDIS_PASSWORD>` | Redis 密码（无密码留空） | `MyRedisPass` |
| `<YOUR_JWT_SECRET>` | JWT 签名密钥（至少64位随机字符串） | `aB3d...长随机串` |
| `<YOUR_QINIU_ACCESS_KEY>` | 七牛云 AccessKey | `GZuoSAt...` |
| `<YOUR_QINIU_SECRET_KEY>` | 七牛云 SecretKey | `phrzhj...` |
| `<YOUR_QINIU_BUCKET>` | 七牛云存储空间名 | `my-bucket` |
| `<YOUR_QINIU_DOMAIN>` | 七牛云访问域名 | `https://cdn.example.com/` |
| `<YOUR_WECOM_CORP_ID>` | 企微企业ID | `ww5ef1...` |
| `<YOUR_WECOM_AGENT_ID>` | 企微应用AgentID | `1000028` |
| `<YOUR_WECOM_SECRET>` | 企微应用Secret | `pVAW2a...` |
| `<YOUR_WECOM_CONTACT_SECRET>` | 企微通讯录Secret（可留空） | - |
| `<YOUR_WECOM_CALLBACK_TOKEN>` | 企微回调Token | `nBd196...` |
| `<YOUR_WECOM_CALLBACK_AES_KEY>` | 企微回调AESKey | `pTKsrX...` |
| `<YOUR_WECOM_TRUSTED_DOMAIN>` | 企微可信域名 | `https://ticket.company.com` |
| `<YOUR_MIDUO_SSO_BASE_URL>` | SSO 服务地址 | `https://admin.ebcone.cn` |
| `<YOUR_MIDUO_SSO_APP_CODE>` | SSO 应用编码 | `APP00012` |
| `<YOUR_MIDUO_SSO_APP_SECRET>` | SSO 应用密钥 | `03164523...` |
| `<YOUR_MIDUO_SSO_SHORTCUT_ID>` | SSO 快捷入口ID | `59` |
| `<YOUR_MIDUO_SSO_REDIRECT_URI>` | SSO 回跳地址 | `http://ticket.ebcone.cn` |
| `<YOUR_TICKET_DETAIL_URL>` | 工单详情页URL | `http://ticket.ebcone.cn/open/ticket` |

### 第 2 步：导入 Nacos

**方式 A：使用脚本自动导入**

```bash
# 导入 dev 环境配置
bash nacos-import.sh dev http://127.0.0.1:8848 nacos nacos

# 导入 test 环境配置
bash nacos-import.sh test http://nacos.company.com:8848 admin YourPassword

# 导入 prod 环境配置
bash nacos-import.sh prod http://nacos.company.com:8848 admin YourPassword
```

**方式 B：手动通过 Nacos 控制台导入**

1. 打开 Nacos 控制台：`http://your-nacos-server:8848/nacos`
2. 创建命名空间：「命名空间」→「新建命名空间」
   - 命名空间ID：`dev`（或 `test` / `prod`）
   - 命名空间名：`dev`
3. 切换到对应命名空间，创建配置：
   - Data ID：`ticket-platform-secrets.yml`
   - Group：`DEFAULT_GROUP`
   - 配置格式：`YAML`
   - 配置内容：粘贴对应环境的 yml 文件内容（已替换真实值）

### 第 3 步：启动应用

```bash
# 方式 A：通过环境变量
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_NAMESPACE=dev
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos

cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=dev \
  -Djdk.tls.client.protocols=TLSv1.2

# 方式 B：通过 JVM 参数
cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=dev \
  -Djdk.tls.client.protocols=TLSv1.2 \
  -Dspring-boot.run.jvmArguments="\
    -Dnacos.config.server-addr=127.0.0.1:8848 \
    -Dnacos.config.namespace=dev \
    -Dnacos.config.username=nacos \
    -Dnacos.config.password=nacos"

# 方式 C：JAR 包部署
java -jar ticket-bootstrap.jar \
  --spring.profiles.active=prod \
  --nacos.config.server-addr=nacos.company.com:8848 \
  --nacos.config.namespace=prod \
  --nacos.config.username=admin \
  --nacos.config.password=YourPassword \
  -Djdk.tls.client.protocols=TLSv1.2
```

---

## Docker 部署

更新 `docker-compose.prod.yml` 中的后端服务环境变量：

```yaml
services:
  backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NACOS_SERVER_ADDR: nacos.company.com:8848
      NACOS_NAMESPACE: prod
      NACOS_USERNAME: admin
      NACOS_PASSWORD: ${NACOS_PASSWORD}
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC -Djdk.tls.client.protocols=TLSv1.2"
```

---

## 配置结构说明

```
miduo-md/nacos-config/
├── README.md                              # 本文档
├── nacos-import.sh                        # 一键导入脚本
├── ticket-platform-secrets-dev.yml        # DEV 环境配置模板
├── ticket-platform-secrets-test.yml       # TEST 环境配置模板
└── ticket-platform-secrets-prod.yml       # PROD 环境配置模板
```

### Nacos 中的配置组织

| 命名空间 | Data ID | Group | 说明 |
|---------|---------|-------|------|
| dev | ticket-platform-secrets.yml | DEFAULT_GROUP | 开发环境敏感配置 |
| test | ticket-platform-secrets.yml | DEFAULT_GROUP | 测试环境敏感配置 |
| prod | ticket-platform-secrets.yml | DEFAULT_GROUP | 生产环境敏感配置 |

---

## 配置优先级

配置加载优先级（从高到低）：

1. **JVM 参数** (`-D`)
2. **环境变量** (`NACOS_SERVER_ADDR` 等)
3. **Nacos 远程配置** (`ticket-platform-secrets.yml`)
4. **本地 application-{profile}.yml**
5. **本地 application.yml**

Nacos 中的配置会覆盖本地 `application-*.yml` 中的同名 key。

---

## 不连接 Nacos 也能运行（本地开发模式）

如果本地开发不想连接 Nacos，可以通过环境变量直接传入敏感配置：

```bash
export DATASOURCE_PASSWORD=localpass
export REDIS_PASSWORD=
export JWT_SECRET=local-dev-jwt-secret-key-must-be-long-enough-64-chars-xxxxx
export QINIU_ACCESS_KEY=your-ak
export QINIU_SECRET_KEY=your-sk
export QINIU_BUCKET=your-bucket
export QINIU_DOMAIN=https://your-cdn.com/

# 禁用 Nacos（启动时会跳过 Nacos 连接）
export NACOS_SERVER_ADDR=

cd ticket-platform
JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH \
  mvn spring-boot:run -pl ticket-bootstrap \
  -Dspring-boot.run.profiles=dev \
  -Djdk.tls.client.protocols=TLSv1.2
```

---

## 常见问题

### Q: Nacos 连不上怎么办？
A: 检查 `NACOS_SERVER_ADDR` 是否正确，网络是否可达。应用启动时如果 Nacos 不可达，会使用本地 `application-*.yml` 中的占位符默认值。

### Q: 如何验证配置是否从 Nacos 加载成功？
A: 查看启动日志中的 Nacos 相关日志，搜索 `nacos` 关键字。成功时会显示 `data-id` 和加载的配置内容。

### Q: 配置修改后需要重启吗？
A: `nacos-config-spring-boot-starter` 支持 `auto-refresh`，已启用。对于 `@ConfigurationProperties` 绑定的属性会自动刷新；但如数据库连接池等需要重启才生效。

### Q: 旧的 .env.prod.example 还需要吗？
A: 该文件仍保留作为参考，但实际的敏感值都应通过 Nacos 或环境变量注入，不再写入代码仓库。
