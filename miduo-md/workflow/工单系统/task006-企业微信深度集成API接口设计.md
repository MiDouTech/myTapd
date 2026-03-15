# Task006 - 企业微信深度集成 API 接口设计

> **版本**：v1.0  
> **日期**：2026-03-02  
> **对应产品文档**：4.6.3 企微应用消息推送、4.6.4 企微群机器人工单

---

## 一、接口总览

| 接口编号 | 接口名称 | HTTP方法 | 接口路径 | 说明 |
|---|---|---|---|---|
| API000020 | 企微回调（URL验证/消息接收） | GET / POST | /api/wecom/callback | URL验证、消息接收、异步处理 |
| API000021 | 企微群绑定列表 | GET | /api/wecom/group-binding/list | 查询群与分类绑定配置 |
| API000022 | 企微群绑定创建 | POST | /api/wecom/group-binding/create | 新增群绑定配置 |
| API000023 | 企微群绑定更新 | PUT | /api/wecom/group-binding/update/{id} | 修改群绑定配置 |

---

## 二、API000020 企微回调

### 1）URL验证（GET）

- **路径**：`GET /api/wecom/callback`
- **请求参数**：
  - `msg_signature`：签名
  - `timestamp`：时间戳
  - `nonce`：随机串
  - `echostr`：加密随机串
- **处理逻辑**：
  1. 按企微规则校验签名；
  2. AES解密 `echostr`；
  3. 返回解密后的明文字符串。

### 2）消息接收（POST）

- **路径**：`POST /api/wecom/callback`
- **请求参数**：
  - `msg_signature`、`timestamp`、`nonce`
- **请求体**：企微加密 XML 消息体（包含 `Encrypt` 字段）
- **处理逻辑**：
  1. 校验签名 + AES 解密；
  2. 解析消息（`MsgId`、`FromUserName`、`ChatId`、`Content`）；
  3. 基于 `MsgId` 去重；
  4. 5秒内返回 `success`；
  5. 消息异步进入消费链路（MQ 开启时走 RabbitMQ，未开启时走异步降级执行）。

---

## 三、API000021 企微群绑定列表

- **路径**：`GET /api/wecom/group-binding/list`
- **说明**：查询企微群绑定配置，返回默认分类名称、Webhook地址、启用状态等。
- **响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "chatId": "wrfxxxxxxxx",
      "chatName": "客服群",
      "defaultCategoryId": 12,
      "defaultCategoryName": "IT支持/网络问题",
      "webhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx",
      "isActive": 1,
      "createTime": "2026-03-02 12:00:00",
      "updateTime": "2026-03-02 12:00:00"
    }
  ],
  "timestamp": 1709380000000
}
```

---

## 四、API000022 企微群绑定创建

- **路径**：`POST /api/wecom/group-binding/create`
- **请求体**：

```json
{
  "chatId": "wrfxxxxxxxx",
  "chatName": "客服群",
  "defaultCategoryId": 12,
  "webhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx",
  "isActive": 1
}
```

- **字段说明**：
  - `chatId`：群唯一标识（必填）
  - `chatName`：群名称（选填）
  - `defaultCategoryId`：默认分类ID（选填）
  - `webhookUrl`：群Webhook地址（选填）
  - `isActive`：启用状态（选填，默认1）

---

## 五、API000023 企微群绑定更新

- **路径**：`PUT /api/wecom/group-binding/update/{id}`
- **请求体**（可选字段更新）：

```json
{
  "chatName": "客服群（新版）",
  "defaultCategoryId": 15,
  "webhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=yyy",
  "isActive": 1
}
```

---

## 六、机器人指令（回调消息异步处理）

以下指令在 `API000020` 的消息接收链路内处理：

| 指令 | 说明 | 示例 |
|---|---|---|
| `@工单助手 帮助` | 返回使用说明 | `@工单助手 帮助` |
| `@工单助手 分类` | 返回可用分类列表 | `@工单助手 分类` |
| `@工单助手 查询 WO-xxx` | 查询工单状态 | `@工单助手 查询 WO-20260228-003` |
| `@工单助手 我的工单` | 查询待处理工单 | `@工单助手 我的工单` |
| `@工单助手 催办 WO-xxx` | 催办指定工单 | `@工单助手 催办 WO-20260228-003` |
| `@工单助手 #分类 标题` | 创建工单（支持优先级/描述扩展） | `@工单助手 #IT支持/网络问题 VPN无法连接` |

---

## 七、错误处理约定

- 解析失败：返回格式提示；
- 分类不存在：返回“分类不存在”并提示查看分类；
- 用户未绑定：返回“请先完成企微授权登录”；
- 重复消息：按 `MsgId` 幂等去重并记录日志；
- 非文本消息：记录为忽略，不进入指令处理。
