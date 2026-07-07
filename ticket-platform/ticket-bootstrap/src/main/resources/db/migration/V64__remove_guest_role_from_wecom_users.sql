-- ============================================================
-- V64__remove_guest_role_from_wecom_users.sql
-- 撤销误授予企微内部员工的 GUEST 角色
-- 背景：V63 插件账号按手机号归并时，会把插件侧的 GUEST 复制到企微主账号；
--       PluginUserMappingService 绑定已有账号时也存在同类问题。
--       GUEST 仅应保留在无企微绑定的外部/插件账号上。
-- ============================================================

UPDATE `sys_user_role` ur
INNER JOIN `sys_user` u ON u.`id` = ur.`user_id` AND u.`deleted` = 0
INNER JOIN `sys_role` r ON r.`id` = ur.`role_id` AND r.`deleted` = 0
SET ur.`deleted` = 1,
    ur.`update_by` = 'migration-v64'
WHERE ur.`deleted` = 0
  AND u.`wecom_userid` IS NOT NULL
  AND u.`wecom_userid` <> ''
  AND UPPER(r.`role_code`) = 'GUEST';
