# LOCAL_SSO_9186_DEMO 接入交付信息

## 基础信息

- 快捷入口 ID：`77`
- 应用编码：`LOCAL_SSO_9186_DEMO`
- 应用名称：`LOCAL_SSO_9186_DEMO`
- 应用密钥：`LOCAL_SSO_9186_SECRET_20260320`
- 服务地址：`http://localhost:9186`
- 所属分类：-
- 快捷入口落地页：`http://localhost:9186/sso/test/landing`
- 类型：链接
- 图标：FontAwesome: `fas fa-key`（背景色：#409EFF）
- 角色可见范围：全员可见
- 排序：99
- 状态：启用
- 是否启用 SSO：`是`
- token 传递方式：`QUERY`
- 交换 token TTL：`600` 秒
- sessionToken TTL：`28800` 秒
- session 最大时长：`604800` 秒
- 允许返回字段：`userId`、`employeeNo`、`userName`、`mobile`、`email`
- 登录桥白名单：`https://localhost:9443/sso/callback`
- 备注：Local 9186 SSO integration test data
- 创建人：cursor
- 创建时间：2026-03-20 09:56:54
- 导出时间：2026-03-20 10:13:11

## 接入方必需信息

- `baseUrl`: `http://localhost:9186`
- `appCode`: `LOCAL_SSO_9186_DEMO`
- `appSecret`: `LOCAL_SSO_9186_SECRET_20260320`
- `shortcutId`: `77`
- `callbackUrl`: `http://localhost:9186/sso/test/landing`
- `redirectUri allowlist`: `https://localhost:9443/sso/callback`

说明：

- `appSecret` 只能保存在第三方后端，不能下发前端。
- 浏览器只负责把 `token` 带到第三方落地页。
- 第三方后端必须调用开放接口校验 `token`，不能直接信任 URL 参数。

## 接口清单

### 米多登录态接口

- `POST /api/auth/sms/login`
- `POST /api/sso/redirect-url?appCode=LOCAL_SSO_9186_DEMO`
- `GET /api/gzt/workbench/shortcut/jump/77`
- `GET /api/sso/bridge/redirect-url?appCode=LOCAL_SSO_9186_DEMO&redirectUri=https://localhost:9443/sso/callback&state=debug-state-001`

### 第三方开放接口

- `POST /api/open/sso/validate-login-token`
- `POST /api/open/sso/refresh-session-token`
- `POST /api/open/sso/revoke-session-token`

## 请求头要求

### 登录态接口

```text
Authorization: Bearer <accessToken>
```

### 开放接口

```text
Content-Type: application/json
X-App-Code: LOCAL_SSO_9186_DEMO
X-App-Timestamp: <unix秒级时间戳>
X-App-Nonce: <随机字符串>
X-App-Signature: <HMAC-SHA256签名，Base64或hex lower>
```

## 签名规则

```text
appCode + "\n" + timestamp + "\n" + nonce + "\n" + signedValue
```

其中：

- 校验交换 token 时，`signedValue = token`
- 续期 sessionToken 时，`signedValue = sessionToken`
- 吊销 sessionToken 时，`signedValue = sessionToken`

签名算法：

- 算法：`HmacSHA256`
- 密钥：`appSecret`
- 输出：推荐 `Base64`

## 接入顺序

### 场景一：首次从米多进入第三方

1. 米多前端调用 `POST /api/sso/redirect-url?appCode=LOCAL_SSO_9186_DEMO`
2. 或工作台直接调用 `GET /api/gzt/workbench/shortcut/jump/77`
3. 获取跳转 URL，例如：

```text
http://localhost:9186/sso/test/landing?token=exch_xxx&app_code=LOCAL_SSO_9186_DEMO&source=miduo
```

4. 第三方从 URL 中提取 `token`
5. 第三方后端调用 `POST /api/open/sso/validate-login-token`
6. 校验成功后获得用户信息和 `sessionToken`
7. 第三方保存 `sessionToken`
8. 后续定时或按需调用 `POST /api/open/sso/refresh-session-token`

### 场景二：第三方退出登录

1. 第三方后端调用 `POST /api/open/sso/revoke-session-token`
2. 吊销后，原 `sessionToken` 不应再可续期

### 场景三：第三方会话失效，走登录桥

1. 当 `refresh-session-token` 返回 `success=false`
2. 浏览器跳转到 `GET /api/sso/bridge/redirect-url`
3. 必须使用白名单中的 `redirectUri`
4. 米多生成新 `exchange token` 后回跳第三方
5. 第三方再次调用 `validate-login-token`

## 请求示例

### 1. 发放跳转 URL

```bash
curl --location --request POST 'http://localhost:9186/api/sso/redirect-url?appCode=LOCAL_SSO_9186_DEMO' \
--header 'Authorization: Bearer {{accessToken}}'
```

### 2. 快捷入口统一跳转

```bash
curl --location 'http://localhost:9186/api/gzt/workbench/shortcut/jump/77' \
--header 'Authorization: Bearer {{accessToken}}'
```

### 3. 校验交换 token

```bash
curl --location --request POST 'http://localhost:9186/api/open/sso/validate-login-token' \
--header 'Content-Type: application/json' \
--header 'X-App-Code: LOCAL_SSO_9186_DEMO' \
--header 'X-App-Timestamp: {{xAppTimestamp}}' \
--header 'X-App-Nonce: {{xAppNonce}}' \
--header 'X-App-Signature: {{xAppSignature}}' \
--data '{
    "token": "{{exchangeToken}}"
}'
```

### 4. 续期 sessionToken

```bash
curl --location --request POST 'http://localhost:9186/api/open/sso/refresh-session-token' \
--header 'Content-Type: application/json' \
--header 'X-App-Code: LOCAL_SSO_9186_DEMO' \
--header 'X-App-Timestamp: {{xAppTimestamp}}' \
--header 'X-App-Nonce: {{xAppNonce}}' \
--header 'X-App-Signature: {{xAppSignature}}' \
--data '{
    "sessionToken": "{{sessionToken}}"
}'
```

### 5. 吊销 sessionToken

```bash
curl --location --request POST 'http://localhost:9186/api/open/sso/revoke-session-token' \
--header 'Content-Type: application/json' \
--header 'X-App-Code: LOCAL_SSO_9186_DEMO' \
--header 'X-App-Timestamp: {{xAppTimestamp}}' \
--header 'X-App-Nonce: {{xAppNonce}}' \
--header 'X-App-Signature: {{xAppSignature}}' \
--data '{
    "sessionToken": "{{sessionToken}}"
}'
```

### 6. 登录桥 URL

```bash
curl --location 'http://localhost:9186/api/sso/bridge/redirect-url?appCode=LOCAL_SSO_9186_DEMO&redirectUri=https%3A%2F%2Flocalhost%3A9443%2Fsso%2Fcallback&state=debug-state-001' \
--header 'Authorization: Bearer {{accessToken}}'
```

## 注意事项

- `appSecret` 仅允许第三方服务端保存和使用。
- `exchange token` 是一次性的，成功校验后立即失效。
- `sessionToken` 用于第三方服务端维持会话，不应暴露给前端。
- 登录桥 `redirectUri` 当前只接受 `https`。
- 如果第三方只实现最小接入，至少要完成：发牌跳转、校验 token、保存 sessionToken、续期 sessionToken、失效后走登录桥。
