# 内部工单系统 (Ticket Platform)

米多内部工单系统，基于 DDD 六边形架构的模块化单体应用。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端语言 | Java | 8+ |
| Web 框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 文件存储 | MinIO | 8.5.7 |
| 认证 | Spring Security + JWT | - |
| API 文档 | SpringDoc OpenAPI | 1.8.0 |

## 项目结构

```
ticket-platform/
├── ticket-bootstrap/          # 启动模块（主程序入口）
├── ticket-controller/         # 接口层（REST Controller）
├── ticket-application/        # 应用服务层（业务编排）
├── ticket-domain/             # 领域层（核心业务逻辑）
├── ticket-entity/             # DTO 模块（请求/响应对象）
├── ticket-infrastructure/     # 基础设施层（数据访问/外部服务）
├── ticket-common/             # 公共模块（枚举/工具/常量）
├── ticket-job/                # 定时任务模块
├── deployment/                # 部署配置（Docker）
├── scripts/                   # 启动/停止脚本
└── build/                     # 构建与 CI 配置
```

## 模块依赖关系

```
bootstrap → controller → application → domain ← infrastructure
                ↓              ↓           ↑
              entity        entity      common
                ↓              ↓
              common        common
```

- `ticket-domain` 只依赖 `ticket-common`，禁止依赖 infrastructure 和 DB 框架
- `ticket-controller` 禁止直接依赖 domain/infrastructure

## 快速开始

### 1. 环境准备

- JDK 8+
- Maven 3.8+
- Docker & Docker Compose

### 2. 启动基础设施

```bash
cd deployment/docker
docker compose up -d
```

这会启动 MySQL (3306)、Redis (6379)、MinIO (9000/9001)。

### 3. 编译项目

```bash
mvn clean install -DskipTests
```

### 4. 启动应用

```bash
cd ticket-bootstrap
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或使用一键脚本：

```bash
./scripts/start-local.sh
```

### 5. 访问

- 应用: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health
- MinIO Console: http://localhost:9001

## 企业微信配置填写位置（重要）

企微参数加载优先级：**数据库配置 > application配置（环境变量）**。

### 方式一（推荐）：管理端页面填写

登录后进入：**系统设置 → 集成设置 → 企微连接配置**，填写并保存以下字段：

- CorpID（企业ID，通常以 `ww` 开头）
- AgentID（自建应用ID）
- 应用Secret（corpSecret）
- API基础地址、超时、同步参数等

保存后可直接点击“连接测试”，后台会调用企微接口验证可用性。

### 方式二：启动参数 / 环境变量填写

`ticket-bootstrap/src/main/resources/application-dev.yml` 已支持以下环境变量：

- `WECOM_CORP_ID`
- `WECOM_AGENT_ID`
- `WECOM_SECRET`
- `WECOM_CONTACT_SECRET`
- `WECOM_CALLBACK_TOKEN`
- `WECOM_CALLBACK_AES_KEY`
- `WECOM_TRUSTED_DOMAIN`

如果你使用 `scripts/release-test-backend.sh`，可在 `scripts/.env.test.local` 中填写对应 `WECOM_*` 参数。
