# 工单核心管理模块 API 接口设计

> 版本：v1.0
> 日期：2026-03-02
> 对应 Task：Task004
> 对应产品文档：4.2 工单管理、4.3 分类与模板管理

---

## 一、接口总览

| 接口编号 | 接口名称 | HTTP方法 | 接口路径 | 说明 |
|---|---|---|---|---|
| API000001 | 获取三级分类树 | GET | /api/category/tree | 返回完整三级分类树结构 |
| API000002 | 获取分类详情 | GET | /api/category/detail/{id} | 返回分类详情含关联信息 |
| API000003 | 新增分类 | POST | /api/category/create | 创建新分类节点 |
| API000004 | 修改分类 | PUT | /api/category/update/{id} | 更新分类信息 |
| API000422 | 删除分类 | DELETE | /api/category/delete/{id} | 删除叶子分类节点 |
| API000005 | 模板列表 | GET | /api/template/list | 按分类查询工单模板 |
| API000006 | 创建工单 | POST | /api/ticket/create | 创建新工单并触发工作流 |
| API000007 | 工单分页 | GET | /api/ticket/page | 多视图分页查询工单 |
| API000008 | 工单详情 | GET | /api/ticket/detail/{id} | 完整工单详情 |
| API000009 | 工单分派 | PUT | /api/ticket/assign/{id} | 手动分派处理人 |
| API000010 | 工单处理 | PUT | /api/ticket/process/{id} | 处理并流转状态 |
| API000011 | 工单关闭 | PUT | /api/ticket/close/{id} | 关闭工单 |
| API000012 | 关注工单 | POST | /api/ticket/follow/{id} | 关注工单 |
| API000013 | 取消关注 | DELETE | /api/ticket/follow/{id} | 取消关注 |
| API000029 | 催办工单 | POST | /api/ticket/urge/{id} | 中间态催办；默认通知关联处理人，请求体可选 extraNotifyUserIds |

---

## 二、统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1709366400000
}
```

---

## 三、接口详细设计

### API000001 获取三级分类树

- **路径**：`GET /api/category/tree`
- **响应**：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "IT 支持",
      "parentId": null,
      "level": 1,
      "children": [
        {
          "id": 2,
          "name": "网络问题",
          "parentId": 1,
          "level": 2,
          "children": [
            {
              "id": 5,
              "name": "VPN 连接异常",
              "parentId": 2,
              "level": 3,
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

### API000002 获取分类详情

- **路径**：`GET /api/category/detail/{id}`
- **响应字段**：id, name, parentId, parentName, level, path, fullPathName, templateId, templateName, workflowId, workflowName, slaPolicyId, slaPolicyName, defaultGroupId, defaultGroupName, sortOrder, isActive, createTime, updateTime

### API000003 新增分类

- **路径**：`POST /api/category/create`
- **请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 分类名称，最长100字符 |
| parentId | Long | 否 | 父分类ID（一级分类为空） |
| level | Integer | 是 | 层级（1/2/3） |
| templateId | Long | 否 | 关联模板ID |
| workflowId | Long | 否 | 关联工作流ID |
| slaPolicyId | Long | 否 | 关联SLA策略ID |
| defaultGroupId | Long | 否 | 默认处理组ID |
| sortOrder | Integer | 否 | 排序号 |

### API000004 修改分类

- **路径**：`PUT /api/category/update/{id}`
- **请求体**：name, templateId, workflowId, slaPolicyId, defaultGroupId, sortOrder, isActive（均为可选）

### API000422 删除分类

- **路径**：`DELETE /api/category/delete/{id}`
- **业务规则**：
  - 分类存在子分类时不可删除
  - 分类已关联模板时不可删除
  - 分类已关联工单时不可删除

### API000005 模板列表

- **路径**：`GET /api/template/list?categoryId={categoryId}`
- **参数**：categoryId（可选，不传返回全部启用模板）

### API000006 创建工单

- **路径**：`POST /api/ticket/create`
- **请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 工单标题 |
| description | String | 否 | 描述（富文本） |
| categoryId | Long | 是 | 分类ID |
| priority | String | 是 | 优先级（urgent/high/medium/low） |
| expectedTime | Date | 否 | 期望完成时间 |
| assigneeId | Long | 否 | 指定处理人 |
| source | String | 否 | 来源（默认web） |
| customFields | Map | 否 | 自定义字段键值对 |

- **业务规则**：
  - 创建后根据分类关联的工作流自动设置初始状态
  - 自动记录操作日志

### API000007 工单分页

- **路径**：`GET /api/ticket/page`
- **参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| view | String | 否 | 视图：my_created/my_todo/my_participated/my_followed/all |
| ticketNo | String | 否 | 工单编号（前缀匹配） |
| title | String | 否 | 标题关键词 |
| categoryId | Long | 否 | 分类ID |
| status | String | 否 | 状态 |
| priority | String | 否 | 优先级 |
| creatorId | Long | 否 | 创建人ID |
| assigneeId | Long | 否 | 处理人ID |
| createTimeStart | String | 否 | 创建时间起 |
| createTimeEnd | String | 否 | 创建时间止 |
| orderBy | String | 否 | 排序字段(create_time/update_time/priority) |
| asc | boolean | 否 | 是否升序 |

### API000008 工单详情

- **路径**：`GET /api/ticket/detail/{id}`
- **响应**：包含工单基本信息、分类信息、处理人信息、自定义字段、附件列表、评论列表、操作日志、是否已关注

### API000009 手动分派

- **路径**：`PUT /api/ticket/assign/{id}`
- **请求体**：assigneeId（必填）、remark（选填）

### API000010 处理工单并流转

- **路径**：`PUT /api/ticket/process/{id}`
- **请求体**：targetStatus（必填）、targetUserId（转派时填）、remark（选填）
- **业务规则**：根据工作流定义校验状态流转合法性

### API000011 关闭工单

- **路径**：`PUT /api/ticket/close/{id}`
- **请求体**：remark（选填）

### API000012 关注工单

- **路径**：`POST /api/ticket/follow/{id}`
- **幂等**：重复关注不报错

### API000013 取消关注

- **路径**：`DELETE /api/ticket/follow/{id}`

---

## 四、错误码

| 错误码 | 说明 |
|---|---|
| 2001 | 工单不存在 |
| 2002 | 工单状态不允许此操作 |
| 2003 | 工单分派失败 |
| 3001 | 工作流流转不合法 |
| 3002 | 工作流定义不存在 |
| 1001 | 参数校验失败 |
| 1002 | 数据不存在 |
