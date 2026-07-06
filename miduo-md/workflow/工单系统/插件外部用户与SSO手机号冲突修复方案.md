# 插件外部用户与 SSO 手机号冲突修复方案

> **日期**：2026-07-06  
> **状态**：已实施  
> **关联**：V62 业务原生工单插件、`/api/auth/sso/callback`

## 一、背景与问题

工单插件通过 `PluginUserMappingService` 为外部用户创建 `sys_user` 记录，工号格式为 `plugin:{systemCode}:{externalUserId}`，并写入手机号。

企微同步员工同样占用 `sys_user.phone`。当两者手机号相同时，SSO 登录按手机号 `selectOne()` 会命中 2 条记录，抛出 `TooManyResultsException`。

## 二、目标

1. SSO 登录不再因手机号重复而 500。
2. 插件外部用户与内部员工身份可正确归并（同手机号优先绑定企微账号）。
3. 插件身份与 `sys_user` 解耦，避免继续污染 `employee_no` 字段。

## 三、方案概要

| 层级 | 措施 |
|------|------|
| 数据 | 新建 `plugin_external_user` 映射表；迁移合并重复账号、归并工单创建人 |
| 插件 | 先查映射表；手机号冲突时绑定已有企微用户，不新建重复账号；新建插件账号不写冲突手机号 |
| SSO | `findByPhone` 等查询多条时优先返回有 `wecom_userid` 的账号并打 warn 日志 |

## 四、非目标

- 不为 `sys_user.phone` 增加全局唯一约束（外部账号、历史数据需兼容）。
- 不改动米多 SSO 回调协议。

## 五、验收标准

1. 存在「企微员工 + 插件用户」同手机号的场景，SSO 登录成功。
2. 插件 LaunchToken 仍可按 `externalUserId` 解析到正确 `userId`。
3. 已合并的插件重复账号逻辑删除，其创建的工单 `creator_id` 指向企微主账号。
