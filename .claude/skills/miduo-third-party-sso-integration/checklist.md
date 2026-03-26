# Miduo Third-Party SSO Checklist（通用验收清单）

This checklist is designed to be reused in **any new project** integrating Miduo Planet SSO.

Mark each item as `OK`, `Missing`, or `Needs Clarification`.

## 1) Export Markdown（交付信息）字段清单

### 1.1 Required identifiers

- `baseUrl`
- `appCode`
- `appSecret`（必须强调：仅第三方后端保存）
- `shortcutId`（如走工作台快捷入口）
- `callbackUrl`（第三方落地页/回调入口）
- `redirectUri allowlist`（登录桥回跳白名单，需 https 且严格匹配）

### 1.2 Required policy fields

- token delivery mode: `QUERY` or `FORM_POST`
- exchange token TTL (seconds)
- sessionToken TTL (seconds)
- session max age (seconds)
- allowed claims（允许返回字段列表）

### 1.3 Required APIs

- `POST /api/sso/redirect-url`
- `GET /api/gzt/workbench/shortcut/jump/{shortcutId}`
- `POST /api/open/sso/validate-login-token`
- `POST /api/open/sso/refresh-session-token`
- `POST /api/open/sso/revoke-session-token`
- `GET /api/sso/bridge/redirect-url`

### 1.4 Required request details（开放接口签名）

- Open API headers:
  - `X-App-Code`
  - `X-App-Timestamp`（unix seconds）
  - `X-App-Nonce`
  - `X-App-Signature`（HMAC-SHA256）
- Signature canonical string:
  - `appCode + "\n" + timestamp + "\n" + nonce + "\n" + signedValue`
- Signed value mapping:
  - validate: `token`
  - refresh: `sessionToken`
  - revoke: `sessionToken`
- Signature output encoding（至少明确一种：建议 Base64；如允许 hex 需明确大小写）
- Anti-replay expectations（timestamp 窗口 + nonce 去重窗口）

### 1.5 Required response handling rules（必须写清）

- Wrapper fields: `return_code`, `return_msg`, `return_data`
- Business success:
  - validate: `return_data.valid == true`
  - refresh/revoke: `return_data.success == true`

## 2) Third-Party Implementation Checklist（你方必须实现的模块）

### 2.1 Browser layer

- Landing/callback handler supports:
  - `QUERY` mode: reads `token` from query
  - `FORM_POST` mode: reads `token` from form body
- Immediately posts `{ token, state }` to backend over HTTPS
- Does not persist `token`/`sessionToken` in localStorage/sessionStorage

### 2.2 Backend layer

- Endpoint to consume browser token (e.g. `/auth/miduo/callback`)
- `state` generation + storage + verification strategy（bind to login attempt/session）
- `validate-login-token` call with signed headers
- Local session creation only when `valid=true`
- Server-side storage for `sessionToken` (DB/Redis), never exposed to browser
- Refresh strategy (refresh-on-activity or scheduler)
- Logout strategy that calls `revoke-session-token`
- Bridge strategy: when refresh fails (`success=false`), redirect browser to bridge URL

### 2.3 Operational & security requirements

- Secrets stored via env/vault; rotation plan exists
- Nonce cache implemented (anti-replay) with reasonable TTL
- Clock skew handling (accept small skew; fail clearly when beyond window)
- Logging policy masks `appSecret`, `token`, `sessionToken`
- Rate limiting/backoff for signature failures and repeated refresh failures

## 3) Common Failure Patterns（问题目录）

### P0（必修复）

- `appSecret` exposed to frontend
- Treating URL PII (`mobile`, `name`, etc.) as trusted identity
- No backend validate step
- Ignoring `return_data.valid` / `return_data.success`
- No login bridge fallback after refresh failure
- `redirectUri` not in allowlist
- `redirectUri` is HTTP while bridge requires HTTPS
- No `state` verification (CSRF/session fixation risk)

### P1（强烈建议）

- No nonce uniqueness handling / no anti-replay window
- No timestamp skew handling
- No logout revoke call
- No second-validate failure handling (one-time token reused)
- Logging full secrets/tokens

### P2（质量/运维提升）

- No runbook for signature failures (how to debug canonical string)
- No explicit refresh cadence recommendation
- No distinction between "internal handoff" vs "external handoff" secrecy级别

## 4) Recommended Acceptance Tests（可复用验收用例）

At minimum, verify:

1. `redirect-url` returns a redirect URL containing an exchange token (QUERY or FORM_POST instruction)
2. `shortcut/jump/{shortcutId}` returns a redirect URL containing an exchange token
3. First `validate-login-token` succeeds with `valid=true` and returns `sessionToken`
4. Second validate of the **same** exchange token returns `valid=false`
5. `refresh-session-token` succeeds (`success=true`) before revoke
6. `revoke-session-token` succeeds (`success=true`)
7. `refresh-session-token` fails (`success=false`) after revoke
8. Bridge URL generation succeeds only for **HTTPS + allowlisted** `redirectUri`
9. Signature failure returns a predictable error and does not leak details
10. Expired/invalid token returns a predictable response and does not create a local session

## 5) Suggested Review Output Template

```markdown
## Integration Verdict
Ready | Ready With Conditions | Not Ready

## Blocking Gaps
- P0: ...

## Required Third-Party Changes
- ...

## Security And Operational Risks
- ...

## Acceptance Checklist
- [ ] ...
```
