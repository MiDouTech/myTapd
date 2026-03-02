# 内部工单系统 (Ticket Platform)

米多内部工单系统，基于 DDD 六边形架构的模块化单体应用。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端语言 | Java | 17 LTS |
| Web 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 文件存储 | MinIO | 8.5.7 |
| 认证 | Spring Security + JWT | - |
| API 文档 | SpringDoc OpenAPI | 2.3.0 |

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

- JDK 17+
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
