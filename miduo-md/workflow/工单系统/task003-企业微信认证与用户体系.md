# Task003：企业微信认证与用户体系

> **业务模块**：工单系统  
> **依赖**：Task001、Task002  
> **预估工时**：5天  
> **对应产品文档**：4.6.1 企微扫码登录、4.6.2 企微通讯录同步、4.10 系统管理

---

## 一、任务目标

实现企业微信 OAuth2.0 扫码登录、JWT 双 Token 认证、通讯录同步与 RBAC 权限框架，建立统一的用户身份体系。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | 企微 OAuth2.0 后端 | `/api/auth/wecom/login` 接收 code 换 Token |
| 2 | 企微 Token 管理 | WecomTokenManager（Redis 缓存 access_token） |
| 3 | 企微 API 客户端 | WecomClient：getToken、getUserInfo |
| 4 | JWT 签发与校验 | 双 Token（Access 30min + Refresh 7d） |
| 5 | Security 配置 | 白名单、JWT 过滤器、登录接口放行 |
| 6 | 通讯录全量同步 | WecomSyncJob 定时拉取部门与成员 |
| 7 | 通讯录增量回调 | 成员/部门变更事件（可选二期） |
| 8 | 用户/部门 CRUD | 用户表、部门表、wecom_userid 映射 |
| 9 | RBAC 权限 | 角色定义、权限注解、数据权限骨架 |
| 10 | 前端登录页 | 企微扫码登录组件、Token 存储与刷新 |

## 三、接口清单（需填接口编号）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 企微登录 | POST | /api/auth/wecom/login | 用 code 换 JWT |
| 刷新 Token | POST | /api/auth/refresh | 用 RefreshToken 换 AccessToken |
| 获取当前用户 | GET | /api/user/current | 当前登录用户信息 |
| 用户列表 | GET | /api/user/list | 用户列表（按部门筛选） |
| 部门树 | GET | /api/department/tree | 组织架构树 |

## 四、验收标准

- [x] 企微扫码后可完成登录并跳转系统  
- [x] JWT 校验正常，未登录接口返回 401  
- [x] 通讯录同步后用户表与企微数据一致  
- [x] 前端登录页符合产品 5.1 设计  

## 五、产出说明

用户身份与权限体系就绪后，工单创建、分派、通知等模块可依赖当前用户上下文。
