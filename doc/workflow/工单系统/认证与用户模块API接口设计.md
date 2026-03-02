# 认证与用户模块 API 接口设计

> **对应 Task**：Task003 - 企业微信认证与用户体系  
> **编写日期**：2026-03-02  
> **对应产品文档**：4.6.1 企微扫码登录、4.6.2 通讯录同步、4.10 系统管理

---

## 一、认证接口

### API000400 - 企微扫码登录

- **路径**：`POST /api/auth/wecom/login`
- **认证**：无需认证（白名单）
- **功能**：用企微授权code换取JWT Token

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 企微扫码后回调的授权码 |

**请求示例**：

```json
{
  "code": "wecom_auth_code_xxx"
}
```

**响应参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| accessToken | String | Access Token（有效期30分钟） |
| refreshToken | String | Refresh Token（有效期7天） |
| expiresIn | Long | Access Token有效时间（秒） |
| userInfo | Object | 用户基本信息 |
| userInfo.id | Long | 用户ID |
| userInfo.name | String | 用户姓名 |
| userInfo.avatar | String | 用户头像URL |
| userInfo.department | String | 所属部门名称 |
| userInfo.roles | List\<String\> | 角色编码列表 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 1800,
    "userInfo": {
      "id": 1,
      "name": "张三",
      "avatar": "https://xxx/avatar.jpg",
      "department": "技术研发部",
      "roles": ["ADMIN", "HANDLER"]
    }
  },
  "timestamp": 1709388000000
}
```

**业务逻辑**：
1. 用code调用企微API获取UserId
2. 通过wecom_userid匹配本地用户
3. 若用户不存在，调用通讯录API获取详情并自动创建账号
4. 检查账号状态（禁用状态不允许登录）
5. 签发AccessToken + RefreshToken并返回

---

### API000401 - 刷新Token

- **路径**：`POST /api/auth/refresh`
- **认证**：无需认证（白名单）
- **功能**：用RefreshToken换取新的AccessToken

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | String | 是 | 有效的Refresh Token |

**响应**：同登录接口响应格式

**错误码**：
- 401：RefreshToken无效或已过期
- 403：账号已被禁用

---

## 二、用户接口

### API000402 - 获取当前用户信息

- **路径**：`GET /api/user/current`
- **认证**：需要JWT认证
- **功能**：获取当前登录用户的详细信息

**响应参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| name | String | 姓名 |
| employeeNo | String | 工号 |
| departmentId | Long | 部门ID |
| departmentName | String | 部门名称 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| position | String | 职位 |
| avatarUrl | String | 头像URL |
| wecomUserid | String | 企微用户标识 |
| accountStatus | Integer | 账号状态（1:已激活 2:已禁用 4:未激活） |
| roleCodes | List\<String\> | 角色编码列表 |
| createTime | Date | 创建时间 |

---

### API000403 - 用户列表

- **路径**：`GET /api/user/list`
- **认证**：需要JWT认证
- **功能**：查询用户列表（支持按部门筛选）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| departmentId | Long | 否 | 部门ID筛选 |
| keyword | String | 否 | 姓名/工号关键字搜索 |
| accountStatus | Integer | 否 | 账号状态筛选 |

**响应**：UserListOutput列表

---

## 三、部门接口

### API000404 - 组织架构树

- **路径**：`GET /api/department/tree`
- **认证**：需要JWT认证
- **功能**：获取完整的部门树形结构

**响应参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 部门ID |
| name | String | 部门名称 |
| parentId | Long | 父部门ID |
| wecomDeptId | Long | 企微部门ID |
| sortOrder | Integer | 排序号 |
| children | List | 子部门列表 |

---

## 四、安全机制说明

### JWT 双Token认证

| Token | 有效期 | 用途 |
|-------|--------|------|
| Access Token | 30分钟 | 接口认证，放在Authorization请求头 |
| Refresh Token | 7天 | 刷新Access Token |

### Security白名单

| 路径 | 说明 |
|------|------|
| /api/auth/** | 认证相关接口 |
| /actuator/** | 监控端点 |
| /swagger-ui/** | API文档 |
| /v3/api-docs/** | API文档数据 |
| /wecom/callback/** | 企微回调接口 |

### RBAC角色

| 角色编码 | 名称 | 说明 |
|----------|------|------|
| ADMIN | 系统管理员 | 全部权限 |
| TICKET_ADMIN | 工单管理员 | 所有工单管理权限 |
| HANDLER | 处理人 | 处理分配给自己的工单 |
| SUBMITTER | 提交人 | 创建工单、查看自己的工单 |
| OBSERVER | 观察者 | 仅查看权限 |

---

## 五、定时任务

### 通讯录全量同步

- **类**：WecomSyncJob
- **频率**：每天凌晨1:00
- **功能**：从企业微信拉取全量部门和成员数据，同步到本地sys_user和department表
- **同步策略**：增量更新（已存在则更新字段变化，不存在则新增）
