-- ============================================================
-- V51__add_external_user_login.sql
-- 外部用户（上下游）手机号+密码登录支持
-- 功能：为 sys_user 增加 password_hash 字段，并插入4个外部账号
-- ============================================================

-- 1. sys_user 增加密码哈希字段（可为 NULL，仅外部账号使用）
ALTER TABLE `sys_user`
    ADD COLUMN `password_hash` varchar(100) DEFAULT NULL COMMENT '密码哈希（仅外部账号使用，BCrypt加密）' AFTER `wecom_userid`;

-- 2. 外部用户账号（上下游人员，无企微账号，通过手机号+密码登录）
-- 角色分配为 OBSERVER（观察者，仅查看权限），对应 sys_role.id = 5
INSERT INTO `sys_user` (`id`, `name`, `phone`, `wecom_userid`, `password_hash`, `account_status`, `create_by`, `update_by`, `deleted`)
VALUES
    (NULL, '李平锋', '13924065449', NULL, '$2a$12$M9CRwgfzHapy9FsaB6GgN.94mT6lqgXXcMoBBn7TvHeAInM3wwF6K', 1, 'system', 'system', 0),
    (NULL, '万雷芸', '18922024486', NULL, '$2a$12$qOPvRvd.9MjGKG5yb3Lsxeam5.6Axk.qTtdmRl2OpnGY13yn.mNUq', 1, 'system', 'system', 0),
    (NULL, '刘俊业', '18942400059', NULL, '$2a$12$QvBd0FZSlYNZ/w4Qi4cxNO1HLUmA9Ft9SXBotQrdVQO1pIGGwxy6K', 1, 'system', 'system', 0),
    (NULL, '朱雨梦', '18942444139', NULL, '$2a$12$Z2T6bIU2o7fZ39VxPSonVOJkeO9zGXA53Dpc0ENNWV3jYo2y7Sgwq', 1, 'system', 'system', 0);

-- 3. 为外部用户分配 OBSERVER 角色（id=5）
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`, `update_by`, `deleted`)
SELECT u.id, 5, 'system', 'system', 0
FROM `sys_user` u
WHERE u.phone IN ('13924065449', '18922024486', '18942400059', '18942444139')
  AND u.deleted = 0;
