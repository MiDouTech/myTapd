# Task003-企微连接配置与API客户端交付说明

## 1. 已交付代码

1. 企微配置持久化
   - `SysWeworkConfigPO`
   - `SysWeworkConfigMapper`
2. 密钥密文托管
   - `WeworkSecretCodec`（AES加密存储，查询脱敏）
3. 运行时配置提供
   - `WeworkRuntimeConfigProvider`（优先DB，回退application配置）
4. API客户端配置接入
   - `WecomTokenManager` 改造为动态读取运行时配置
   - `WecomClient` 改造为动态API基址 + 动态AgentId
5. 配置管理接口
   - `GET /api/wecom/config/detail`
   - `POST /api/wecom/config/save`
   - `POST /api/wecom/config/test-connect`

## 2. 接口编号

1. `API000422`：查询企业微信配置详情
2. `API000423`：保存企业微信配置
3. `API000424`：企业微信连接测试

## 3. 关键行为

1. 配置保存时，`corpSecret` 以密文形式写入 `sys_wework_config.corp_secret`。
2. 配置详情返回时，`corpSecret` 仅脱敏展示（前4后4）；回调 `Token` 仅通过 `callbackTokenMasked` 脱敏返回，不返回明文。
3. **更新保存（已有记录）**：`corpSecret`、`callbackAesKey`、`callbackToken` 若请求中为空或仅空白，表示保留库内原值，不要求每次完整重填。
4. **无变更跳过写库**：解析后的有效配置与当前库记录一致时，不执行 UPDATE，且不刷新 access_token 缓存。
5. **首次保存**：仍须提供非空的 `corpSecret`（新建记录）。
6. 连接测试通过拉取企微部门列表验证可用性，并返回统计结果。
7. 配置发生实际变更并写库后自动刷新 token 缓存，确保新配置立即生效。

## 4. 配置项说明

建议在环境中配置：

- `wecom.config-secret-key`：用于加解密`corpSecret`（未配置则回退使用`jwt.secret`）
- `WEWORK_CORP_ID`、`WEWORK_AGENT_ID`、`WEWORK_CORP_SECRET`：可作为初始配置来源

## 5. 编译验证

- 执行：`mvn -pl ticket-controller -am -DskipTests compile`
- 结果：`BUILD SUCCESS`
