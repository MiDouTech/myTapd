# Task001：项目工程搭建与基础架构

> **业务模块**：工单系统  
> **依赖**：无  
> **预估工时**：2天  
> **对应产品文档**：第七章 实施路线图 第一期 T1.1

---

## 一、任务目标

搭建工单系统项目工程骨架，建立 DDD 六边形架构的模块化单体结构，配置开发环境与基础依赖。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 父 POM | `ticket-platform/pom.xml` |
| 2 | 各模块 POM | ticket-bootstrap、ticket-controller、ticket-application、ticket-domain、ticket-entity、ticket-infrastructure、ticket-common、ticket-job |
| 3 | 启动类 | `ticket-bootstrap/.../TicketApplication.java` |
| 4 | 全局配置 | WebMvcConfig、GlobalExceptionHandler、SecurityConfig（基础骨架） |
| 5 |  profiles 配置 | application.yml、application-dev.yml |
| 6 | Docker Compose | MySQL + Redis 本地开发环境 |
| 7 | 构建与 CI 配置 | Checkstyle、SonarQube 占位 |
| 8 | README | 项目说明与本地启动指南 |

## 三、技术规范

- **Java**：17 LTS  
- **Spring Boot**：3.2.x  
- **Maven**：多模块聚合  
- **DDD 分层**：遵循技术分析文档 3.4 工程模块结构  
- **模块依赖方向**：bootstrap → controller/application/domain/infrastructure/entity/common/job

## 四、验收标准

- [ ] `mvn clean install` 编译通过  
- [ ] 主程序可启动（可连 Mock 数据源或空实现）  
- [ ] Docker Compose 可一键启动 MySQL + Redis  
- [ ] 各模块依赖关系符合 DDD 分层原则  

## 五、产出说明

本 task 完成后，形成可运行的工程骨架，为 Task002 数据库设计与 Task003 企微认证提供基础设施。
