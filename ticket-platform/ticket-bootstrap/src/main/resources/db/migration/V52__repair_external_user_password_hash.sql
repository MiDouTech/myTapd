-- ============================================================
-- V52__repair_external_user_password_hash.sql
-- 修复：确保 sys_user 表的 password_hash 列存在
-- 背景：V51 在部分环境未成功执行，导致 password_hash 列缺失，
--       外部用户手机号+密码登录（/api/auth/local/login）报
--       Unknown column 'password_hash' in 'field list'
-- ============================================================

-- 1. 幂等添加 password_hash 列（若已存在则跳过）
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password_hash'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD COLUMN `password_hash` varchar(100) DEFAULT NULL COMMENT ''密码哈希（仅外部账号使用，BCrypt加密）'' AFTER `wecom_userid`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 幂等插入外部用户账号（按手机号去重）
INSERT INTO `sys_user` (`name`, `phone`, `wecom_userid`, `password_hash`, `account_status`, `create_by`, `update_by`, `deleted`)
SELECT '李平锋', '13924065449', NULL, '$2a$12$M9CRwgfzHapy9FsaB6GgN.94mT6lqgXXcMoBBn7TvHeAInM3wwF6K', 1, 'system', 'system', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `phone` = '13924065449' AND `deleted` = 0);

INSERT INTO `sys_user` (`name`, `phone`, `wecom_userid`, `password_hash`, `account_status`, `create_by`, `update_by`, `deleted`)
SELECT '万雷芸', '18922024486', NULL, '$2a$12$qOPvRvd.9MjGKG5yb3Lsxeam5.6Axk.qTtdmRl2OpnGY13yn.mNUq', 1, 'system', 'system', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `phone` = '18922024486' AND `deleted` = 0);

INSERT INTO `sys_user` (`name`, `phone`, `wecom_userid`, `password_hash`, `account_status`, `create_by`, `update_by`, `deleted`)
SELECT '刘俊业', '18942400059', NULL, '$2a$12$QvBd0FZSlYNZ/w4Qi4cxNO1HLUmA9Ft9SXBotQrdVQO1pIGGwxy6K', 1, 'system', 'system', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `phone` = '18942400059' AND `deleted` = 0);

INSERT INTO `sys_user` (`name`, `phone`, `wecom_userid`, `password_hash`, `account_status`, `create_by`, `update_by`, `deleted`)
SELECT '朱雨梦', '18942444139', NULL, '$2a$12$Z2T6bIU2o7fZ39VxPSonVOJkeO9zGXA53Dpc0ENNWV3jYo2y7Sgwq', 1, 'system', 'system', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `phone` = '18942444139' AND `deleted` = 0);

-- 3. 为已存在但缺少密码的外部用户补充密码哈希
UPDATE `sys_user` SET `password_hash` = '$2a$12$M9CRwgfzHapy9FsaB6GgN.94mT6lqgXXcMoBBn7TvHeAInM3wwF6K'
WHERE `phone` = '13924065449' AND `deleted` = 0 AND (`password_hash` IS NULL OR `password_hash` = '');

UPDATE `sys_user` SET `password_hash` = '$2a$12$qOPvRvd.9MjGKG5yb3Lsxeam5.6Axk.qTtdmRl2OpnGY13yn.mNUq'
WHERE `phone` = '18922024486' AND `deleted` = 0 AND (`password_hash` IS NULL OR `password_hash` = '');

UPDATE `sys_user` SET `password_hash` = '$2a$12$QvBd0FZSlYNZ/w4Qi4cxNO1HLUmA9Ft9SXBotQrdVQO1pIGGwxy6K'
WHERE `phone` = '18942400059' AND `deleted` = 0 AND (`password_hash` IS NULL OR `password_hash` = '');

UPDATE `sys_user` SET `password_hash` = '$2a$12$Z2T6bIU2o7fZ39VxPSonVOJkeO9zGXA53Dpc0ENNWV3jYo2y7Sgwq'
WHERE `phone` = '18942444139' AND `deleted` = 0 AND (`password_hash` IS NULL OR `password_hash` = '');

-- 4. 幂等分配 OBSERVER 角色（role_id=5）
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`, `update_by`, `deleted`)
SELECT u.id, 5, 'system', 'system', 0
FROM `sys_user` u
WHERE u.phone IN ('13924065449', '18922024486', '18942400059', '18942444139')
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` ur
      WHERE ur.user_id = u.id AND ur.role_id = 5 AND ur.deleted = 0
  );
