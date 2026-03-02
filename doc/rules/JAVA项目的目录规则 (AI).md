# 项目目录
```bash
miduo-basebackend/                               # Miduo 后端项目主目录（父工程）
├── pom.xml                                     # 父级 Pom 文件，统一依赖管理与模块聚合
├── README.md                                   # 项目说明文档
├── .gitignore                                  # Git 忽略文件配置
├── .editorconfig                               # 代码风格统一配置（缩进、换行、编码）
├── .gitattributes                              # Git 属性配置（行尾转换、合并策略）
├── LICENSE                                     # 许可证文件
│
├── build/                                      # 构建与 CI 配置文件目录 
│   ├── maven-settings.xml                      # Maven 自定义配置文件
│   ├── ci-checkstyle.xml                       # 代码规范检查规则文件
│   ├── jacoco.exec                             # 代码覆盖率报告文件（Jacoco）
│   └── sonar-project.properties                 # SonarQube 项目扫描配置
│
├── docs/                                       # 项目文档（设计、接口、数据库等）ai的doc
│   ├── api/                                    # 接口文档（API 定义、Swagger 导出等）
│   ├── arch/                                   # 架构设计文档（架构图、设计原则）
│   └── db/                                     # 数据库结构文档（ER 图、DDL 说明）
│
├── scripts/                                    # 启动、部署与数据迁移脚本
│   ├── start-local.sh                          # 本地启动脚本
│   ├── stop.sh                                 # 服务停止脚本
│   ├── export.sh                               # 数据导出脚本
│   └── migrate.sh                              # 数据库迁移脚本（Flyway/Liquibase）
│
├── deployment/                                 # 部署相关配置文件夹
│   ├── docker/                                 # Docker 相关文件夹
│   │   ├── Dockerfile                          # Docker 构建文件
│   │   └── docker-compose.yml                  # Docker Compose 启动配置
│   └── helm/                                   # Kubernetes Helm Chart 配置
│       └── miduo/
│           ├── Chart.yaml                      # Helm Chart 元信息
│           ├── values.yaml                     # Helm 配置参数
│           └── templates/                      # Helm 模板文件
│               ├── deployment.yaml             # Kubernetes 部署模板
│               ├── service.yaml                # Kubernetes Service 模板
│               └── configmap.yaml              # 配置映射模板
│
├── .github/                                   # GitHub Actions CI/CD 配置目录 / cnb.yml 可以效仿
│   └── workflows/
│       ├── build.yml                           # 构建流水线
│       ├── test.yml                            # 测试流水线
│       └── release.yml                         # 发布流水线
│
├── miduo-bootstrap/                            # 项目启动模块（主程序入口）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/bootstrap/
│       │   ├── MiduoApplication.java            # 启动类，SpringBootApplication 入口
│       │   └── config/                          # 全局配置类
│       │       ├── WebMvcConfig.java            # Web MVC 配置（跨域、拦截器）
│       │       ├── GlobalExceptionHandler.java  # 全局异常处理器
│       │       └── SecurityConfig.java          # 安全与鉴权配置
│       └── main/resources/
│           ├── application.yml                  # 默认配置文件
│           ├── application-dev.yml              # 开发环境配置
│           ├── application-test.yml             # 测试环境配置
│           ├── application-prod.yml             # 生产环境配置
│           ├── logback-spring.xml               # 日志配置文件
│           ├── banner.txt                       # 启动Banner
│           └── messages.properties              # 国际化配置
│
├── miduo-controller/                            # 接口层模块（Controller 层）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/controller/
│       │   ├── auth/                            # 认证相关接口
│       │   │   └── AuthController.java          # 登录/注册/Token 接口
│       │   ├── workbench/                       # 工作台接口
│       │   │   └── WorkbenchController.java     # 快捷入口、统计面板等接口
│       │   ├── customer/                        # 客户中心接口
│       │   │   └── CustomerController.java      # 客户资料、分组接口
│       │   ├── advice/                          # 全局切面（AOP/异常/验证）
│       │   │   └── ValidationAdvice.java        # 参数验证拦截与格式化
│       │   └── config/                          # Swagger、OpenAPI配置
│       │       └── OpenApiConfig.java
│       └── main/resources/
│           └── application.yml
│
├── miduo-application/                          # 应用层模块（业务编排、事务控制）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/application/
│       │   ├── auth/                            # 认证用例服务
│       │   │   ├── AuthApplicationService.java  # 登录、注册逻辑封装
│       │   │   └── mapper/                     # DTO ↔ Domain 映射层
│       │   │       └── AuthDtoMapper.java
│       │   ├── workbench/                       # 工作台用例服务
│       │   │   ├── WorkbenchApplicationService.java
│       │   │   └── mapper/
│       │   │       └── WorkbenchDtoMapper.java
│       │   ├── customer/                        # 客户业务服务
│       │   │   └── CustomerApplicationService.java
│       │   └── common/
│       │       └── BaseApplicationService.java  # 公共父类
│       └── main/resources/
│           └── application.yml
│
├── miduo-domain/                               # 领域层模块（核心业务逻辑）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/domain/
│       │   ├── auth/                            # 认证领域模型
│       │   │   ├── model/                       # 实体和值对象
│       │   │   │   ├── User.java
│       │   │   │   └── Token.java
│       │   │   ├── service/                     # 领域服务
│       │   │   │   └── AuthDomainService.java
│       │   │   └── repository/                  # 仓储接口定义
│       │   │       └── UserRepository.java
│       │   ├── workbench/                       # 工作台领域模型
│       │   │   ├── model/
│       │   │   ├── service/
│       │   │   └── repository/
│       │   └── common/                          # 通用领域（事件、通用逻辑）
│       │       └── event/
│       │           ├── DomainEvent.java         # 领域事件基类
│       │           └── UserLoggedInEvent.java   # 登录事件示例
│       └── main/resources/
│           └── application.yml
│
├── miduo-infrastructure/                       # 基础设施层（数据访问/外部服务）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/infrastructure/
│       │   ├── persistence/                     # 数据持久化
│       │   │   ├── mybatis/
│       │   │   │   ├── auth/                    # 认证相关数据库访问
│       │   │   │   │   ├── mapper/              # Mapper接口
│       │   │   │   │   │   ├── DepartmentMapper.java
│       │   │   │   │   │   └── WeworkQrcodeMapper.java
│       │   │   │   │   └── po/                  # 数据库PO对象
│       │   │   │   │       ├── DepartmentPO.java
│       │   │   │   │       └── WeworkQrcodePO.java
│       │   │   │   ├── workbench/               # 工作台数据访问
│       │   │   │   │   ├── mapper/
│       │   │   │   │   │   ├── SysShortcutCategoryMapper.java
│       │   │   │   │   │   └── SysSsoAppConfigExtMapper.java
│       │   │   │   │   └── po/
│       │   │   │   │       ├── SysShortcutCategoryPO.java
│       │   │   │   │       └── SysSsoAppConfigExtPO.java
│       │   │   ├── repositoryimpl/              # 仓储实现类
│       │   │   │   ├── AuthRepositoryImpl.java
│       │   │   │   └── WorkbenchRepositoryImpl.java
│       │   ├── external/                        # 外部系统适配层
│       │   │   └── wework/
│       │   │       ├── WeworkClient.java        # 企业微信客户端封装
│       │   │       └── WeworkConfig.java        # 企业微信配置
│       │   ├── mq/                              # 消息队列模块
│       │   │   ├── publisher/                   # 消息发布者
│       │   │   └── subscriber/                  # 消息消费者
│       │   ├── cache/                           # 缓存封装模块
│       │   │   └── RedisCacheTemplate.java
│       │   └── config/                          # 数据源与MyBatis配置
│       │       ├── DataSourceConfig.java
│       │       └── MybatisPlusConfig.java
│       └── main/resources/
│           ├── mapper/                           # MyBatis XML文件
│           │   ├── auth/
│           │   │   └── WeworkQrcodeMapper.xml
│           │   └── workbench/
│           │       └── SysSsoAppConfigExtMapper.xml
│           ├── db/migration/                     # 数据库迁移脚本
│           │   ├── V1__init.sql
│           │   └── V2__workbench_shortcut.sql
│           └── application.yml
│
├── miduo-common/                               # 公共模块（通用DTO、常量、工具类）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/common/
│       │   ├── dto/common/                     # 通用DTO定义
│       │   │   ├── ApiResult.java
│       │   │   ├── ErrorOutput.java
│       │   │   ├── PageInput.java
│       │   │   ├── PageOutput.java
│       │   │   ├── SortInput.java
│       │   │   ├── IdInput.java
│       │   │   └── IdsInput.java
│       │   ├── enums/                          # 枚举定义
│       │   │   └── ErrorCodes.java
│       │   ├── util/                           # 工具类（封装常用函数）
│       │   │   ├── SnowflakeId.java
│       │   │   └── JacksonUtils.java
│       │   └── constants/                      # 常量类
│       │       └── AppConstants.java
│       └── main/resources/
│           └── application.yml
│
├── miduo-entity/                               # DTO 模块（业务用例划分 + Input/Output）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/entity/dto/
│       │   ├── auth/                            # 认证DTO（输入输出成对）
│       │   │   ├── LoginInput.java              # 登录请求体
│       │   │   ├── LoginOutput.java             # 登录返回体
│       │   │   ├── RefreshTokenInput.java       # 刷新Token输入
│       │   │   └── RefreshTokenOutput.java      # 刷新Token输出
│       │   ├── workbench/                       # 工作台DTO
│       │   │   ├── ListShortcutsInput.java
│       │   │   ├── ListShortcutsOutput.java
│       │   │   ├── CreateShortcutInput.java
│       │   │   └── CreateShortcutOutput.java
│       │   ├── customer/                        # 客户DTO
│       │   │   ├── CreateCustomerInput.java
│       │   │   ├── CreateCustomerOutput.java
│       │   │   ├── GetCustomerInput.java
│       │   │   └── GetCustomerOutput.java
│       │   └── v2/                              # 版本化DTO目录
│       │       └── workbench/...
│       └── main/resources/
│           └── application.yml
│
├── miduo-mbg/                                  # MyBatis Generator 自动生成模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/mbg/
│       │   ├── config/GeneratorConfig.java       # 生成器配置
│       │   ├── templates/                        # 自定义模板
│       │   └── runner/GeneratorRunner.java       # 启动生成任务
│       └── main/resources/
│           └── generator.yml                     # 生成器YAML配置
│
├── miduo-integration/                          # 外部系统/内部服务集成模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/integration/
│       │   ├── feign/                           # Feign客户端接口
│       │   ├── dto/                             # 外部系统DTO
│       │   └── config/                          # 集成配置
│       └── main/resources/
│           └── application.yml
│
├── miduo-job/                                  # 定时任务与批处理模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/job/
│       │   ├── handler/                         # 任务执行器
│       │   │   └── WorkbenchSyncJob.java        # 示例任务
│       │   └── config/                          # 任务配置
│       └── main/resources/
│           ├── application.yml
│           └── xxl-job-admin.properties          # XXL-Job 配置
│
├── miduo-mini/                                 # 小程序/轻应用模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/miduo/cloud/mini/
│       │   ├── controller/                      # 小程序接口层
│       │   ├── service/                         # 小程序服务层
│       │   └── dto/                             # 小程序DTO（同Input/Output规范）
│       └── main/resources/
│           └── application.yml
│
└── miduo-test/                                 # 集成与端到端测试模块
    ├── pom.xml
    └── src/
        ├── test/java/com/miduo/cloud/e2e/       # E2E/契约测试
        │   ├── AuthE2ETest.java
        │   └── WorkbenchE2ETest.java
        └── test/resources/
            ├── application-test.yml             # 测试配置
            └── data/                            # 测试数据
```

# 关键说明
1. **DTO 目录**：在 `miduo-entity/src/main/java/com/miduo/cloud/entity/dto/` 下，**按业务（或 Controller）分目录**，**每个用例两个文件**：`XxxInput` 与 `XxxOutput`。示例已完整列出 `auth/ workbench/ customer/`。
2. **分层闭环**：`controller → application → domain → infrastructure` 四层全量存在；`domain` 只暴露仓储接口，具体实现放 `infrastructure/repositoryimpl`。
3. **数据访问**：MyBatis-Plus 的 `mapper/java` 与 `mapper.xml` 均在 `miduo-infrastructure` 下，严格和 DTO/Domain 解耦；数据库迁移脚本在 `db/migration`。
4. **公共能力**：`miduo-common` 里集中收纳 `ApiResult/分页/错误码/工具/常量`，供各层复用。
5. **启动与装配**：`miduo-bootstrap` 作为唯一入口（可拆 profile），统一异常、日志、跨模块配置。
6. **CI/CD 与部署**：顶层提供 `build/`、`.github/workflows/`、`deployment/docker` 与 `helm` 样板，确保“结构完整到上线维度”。
7. **测试隔离**：公共 E2E/契约测试收拢在 `miduo-test`，避免干扰业务模块依赖；各模块内部亦可自带 `unit test`。
8. **版本化策略**：在 `miduo-entity/dto` 下可创建 `v2/` 子包平滑演进；网关或 `OpenApiConfig` 中可切换路由版本。
9. **生成器隔离**：`miduo-mbg` 专管代码生成；生成产物映射到 `infrastructure/persistence/mybatis` 的 `po/mapper/xml`，不污染 DTO 与领域模型。
10. **小程序侧**：`miduo-mini` 保持相同 DTO 约定与分层结构，避免“另起一套”。
11. **奥卡姆剃刀 **: 用不到的项目包, 或可合并的项目, 按照自己项目的方式进行合并, 如domain/application, 可合并项目文件夹不变。

---

# 典型调用链（类名与位置一一可定位）
+ `AuthController (miduo-controller)`  
→ `AuthApplicationService (miduo-application)`  
→ `AuthDomainService (miduo-domain)` + `UserRepository (miduo-domain)`  
→ `AuthRepositoryImpl/Mapper (miduo-infrastructure)`  
→ DB/Cache/MQ
+ 入参：`LoginInput (miduo-entity/dto/auth)`
+ 出参：`LoginOutput (miduo-entity/dto/auth)`
+ 包装：`ApiResult<LoginOutput> (miduo-common/dto/common)`

---

# 父 Pom（建议的聚合顺序与依赖方向）
+ `miduo-bootstrap` 仅依赖：`controller + application + domain + infrastructure + entity + common`
+ `miduo-controller` 依赖：`application + entity + common`
+ `miduo-application` 依赖：`domain + infrastructure + entity + common`
+ `miduo-domain` 依赖：`common`（**禁止依赖** `infrastructure/mbg` 与 DB 框架）
+ `miduo-infrastructure` 依赖：`domain + common`（实现仓储接口/外部适配）
+ `miduo-entity` 依赖：`common`（严禁依赖 `infrastructure`）
+ `miduo-mbg` 独立运行，不被业务模块依赖（或仅在开发期 scope 设置 `provided`/`test`）  
  
  


# 项目依赖及组件管理
## 提前阅读：依赖中的版本为总后台使用版本，仅作为参考
## 依赖分级管理原则
### 分级定义
+ **必须级依赖**：核心框架和基础设施，项目启动和运行必需
+ **建议级依赖**：增强功能和性能优化，强烈建议引入
+ **推荐级依赖**：开发辅助和特定场景使用，按需引入

---

## 一级：必须级依赖（MUST）
> 这些依赖是项目核心基础，必须引入，不可缺少
>

### 1. Spring Boot 核心框架
```xml
<!-- Spring Boot 父依赖 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.2.5.RELEASE</version>
</parent>
<!-- Web 启动器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- AOP 支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<!-- 数据校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

```

**理由**: Spring Boot 是整个项目的基础框架，提供依赖注入、Web服务、AOP切面等核心功能

---

### 2. MyBatis-Plus 持久层框架
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>

```

**理由**: 项目采用MyBatis-Plus作为ORM框架，提供强大的CRUD功能和代码生成能力

---

### 3. MySQL 数据库驱动
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.20</version>
</dependency>

```

**理由**: 项目使用MySQL作为主数据库，驱动程序必需

---

### 4. Druid 数据库连接池
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.6</version>
</dependency>

```

**理由**: 提供高性能数据库连接池，包含监控和统计功能

---

### 5. Redis 缓存支持
```xml
<!-- Spring Boot Redis 启动器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Jedis 客户端 -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.1.5</version>
</dependency>
<!-- 连接池支持 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.6.2</version>
</dependency>

```

**理由**: Redis用于缓存、会话管理、分布式锁等核心功能

---

### 6. Lombok 代码简化工具
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

```

**理由**: 简化Java Bean编写，减少样板代码，提高开发效率

---

### 7. Hutool 工具类库
```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.0</version>
</dependency>

```

**理由**: 提供丰富的Java工具类，是项目中大量使用的工具库

---

### 8. JWT 认证支持
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

```

**理由**: 用于实现基于JWT的用户认证和授权机制

---

### 9. FastJSON 序列化工具
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>

```

**理由**: 高性能JSON序列化/反序列化工具，项目中广泛使用

---

## 二级：建议级依赖（SHOULD）
> 这些依赖提供重要的增强功能，强烈建议引入以提升系统质量
>

### 1. Spring Cloud Alibaba（微服务基础）
```xml
<!-- Spring Cloud 依赖管理 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>Hoxton.SR12</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- Spring Cloud Alibaba 依赖管理 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>2.2.5.RELEASE</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- Nacos 服务注册与发现 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!-- Nacos 配置中心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- 配置刷新支持 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-context</artifactId>
</dependency>

```

**理由**: 

+ 支持微服务架构
+ 提供动态配置管理
+ 实现服务注册与发现
+ 为系统扩展预留空间

---

### 2. Swagger/Knife4j API文档
```xml
<!-- Swagger 核心依赖 -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.9.2</version>
</dependency>
<!-- 解决 Swagger 版本问题 -->
<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-models</artifactId>
    <version>1.6.0</version>
</dependency>
<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>1.6.0</version>
</dependency>
<!-- Knife4j 文档增强 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>2.0.4</version>
</dependency>

```

**理由**: 

+ 自动生成API文档
+ 提供在线调试功能
+ 提升前后端协作效率
+ 减少文档维护成本

---

### 3. Actuator 监控与健康检查
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

```

**理由**: 

+ 提供应用健康检查
+ 监控系统运行状态
+ 支持生产环境运维

---

### 4. Logstash 日志收集
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>5.3</version>
</dependency>

```

**理由**: 

+ 结构化日志输出
+ 支持ELK日志分析
+ 便于问题排查和分析

---

### 5. MyBatis-Plus 代码生成器
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.3.1</version>
</dependency>
<!-- 代码生成器模板引擎 -->
<dependency>
    <groupId>org.apache.velocity</groupId>
    <artifactId>velocity-engine-core</artifactId>
    <version>2.3</version>
</dependency>

```

**理由**: 

+ 快速生成基础CRUD代码
+ 提高开发效率
+ 保证代码规范一致性

---

### 6. 文件处理工具
```xml
<!-- 文件上传处理 -->
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.5</version>
</dependency>
<!-- ZIP文件处理 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-compress</artifactId>
    <version>1.21</version>
</dependency>

```

**理由**: 

+ 支持文件上传下载
+ 处理压缩文件
+ 是原型发布等功能的基础

---

## 三级：推荐级依赖（MAY）
> 这些依赖用于特定场景或功能增强，按实际需求选择性引入
>

### 1. 七牛云存储 SDK
```xml
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-java-sdk</artifactId>
    <version>7.13.0</version>
</dependency>

```

**使用场景**: 

+ HTML转URL功能
+ 原型文件存储
+ 静态资源托管

**引入建议**: 如果使用七牛云作为对象存储，则必须引入

---

### 2. Spring Boot 测试支持
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

```

**使用场景**: 

+ 单元测试
+ 集成测试
+ 测试驱动开发

**引入建议**: 强烈建议在开发环境引入，提升代码质量

---

### 3. Spring Boot 配置处理器
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

```

**使用场景**: 

+ 自定义配置类智能提示
+ 提升开发体验

**引入建议**: 开发环境建议引入，生产环境可选

---

## 依赖冲突解决
### 常见冲突处理
```xml
<!-- 排除冲突的传递依赖 -->
<dependency>
    <groupId>some.group</groupId>
    <artifactId>some-artifact</artifactId>
    <exclusions>
        <exclusion>
            <groupId>conflict.group</groupId>
            <artifactId>conflict-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>

```

### 版本统一管理
使用`<dependencyManagement>`统一管理版本，避免子模块版本不一致

---

## 依赖引入流程
### 标准流程
1. **需求评估**: 评估是否必须引入新依赖
2. **方案调研**: 对比多个同类库的优劣
3. **版本选择**: 选择稳定版本，避免使用快照版本
4. **技术评审**: 团队评审通过后引入
5. **文档更新**: 更新依赖文档和版本说明
6. **测试验证**: 完整测试确保无问题



## 参考资源
### 官方文档
+ [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
+ [MyBatis-Plus 官方文档](https://baomidou.com/)
+ [Spring Cloud Alibaba 官方文档](https://spring-cloud-alibaba-group.github.io/)

### 内部规范
+ [https://miduo1031.yuque.com/xbe40z/vut4hx/wktrgrqua4eihfb3](https://miduo1031.yuque.com/xbe40z/vut4hx/wktrgrqua4eihfb3)

