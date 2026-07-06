-- ============================================================
-- V63__plugin_external_user_mapping.sql
-- 插件外部用户映射表 + 清理与企微员工重复的手机号账号
-- ============================================================

CREATE TABLE IF NOT EXISTS `plugin_external_user` (
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `system_code`      varchar(50)  NOT NULL COMMENT '接入系统标识，如 khjy_home',
    `external_user_id` varchar(100) NOT NULL COMMENT '外部系统用户ID',
    `user_id`          bigint       NOT NULL COMMENT '本地 sys_user.id',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`        varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`          tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_external_user` (`system_code`, `external_user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件外部用户与本地账号映射';

-- 1) 将插件创建的工单归并到同手机号的企微主账号
UPDATE `ticket` t
INNER JOIN `sys_user` plugin_u ON t.`creator_id` = plugin_u.`id` AND plugin_u.`deleted` = 0
INNER JOIN `sys_user` wecom_u ON wecom_u.`phone` = plugin_u.`phone`
    AND wecom_u.`deleted` = 0
    AND wecom_u.`id` <> plugin_u.`id`
    AND wecom_u.`wecom_userid` IS NOT NULL
    AND wecom_u.`wecom_userid` <> ''
SET t.`creator_id` = wecom_u.`id`,
    t.`update_by` = 'migration-v63'
WHERE plugin_u.`employee_no` LIKE 'plugin:%'
  AND plugin_u.`phone` IS NOT NULL
  AND plugin_u.`phone` <> '';

-- 2) 回填映射：同手机号优先指向企微主账号
INSERT INTO `plugin_external_user` (`system_code`, `external_user_id`, `user_id`, `create_by`, `update_by`, `deleted`)
SELECT
    SUBSTRING_INDEX(SUBSTRING(plugin_u.`employee_no`, 8), ':', 1) AS `system_code`,
    SUBSTRING_INDEX(plugin_u.`employee_no`, ':', -1) AS `external_user_id`,
    wecom_u.`id` AS `user_id`,
    'migration-v63',
    'migration-v63',
    0
FROM `sys_user` plugin_u
INNER JOIN `sys_user` wecom_u ON wecom_u.`phone` = plugin_u.`phone`
    AND wecom_u.`deleted` = 0
    AND wecom_u.`id` <> plugin_u.`id`
    AND wecom_u.`wecom_userid` IS NOT NULL
    AND wecom_u.`wecom_userid` <> ''
WHERE plugin_u.`deleted` = 0
  AND plugin_u.`employee_no` LIKE 'plugin:%'
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `update_by` = 'migration-v63',
    `deleted` = 0;

-- 3) 无企微冲突的插件账号：映射到自身
INSERT INTO `plugin_external_user` (`system_code`, `external_user_id`, `user_id`, `create_by`, `update_by`, `deleted`)
SELECT
    SUBSTRING_INDEX(SUBSTRING(u.`employee_no`, 8), ':', 1) AS `system_code`,
    SUBSTRING_INDEX(u.`employee_no`, ':', -1) AS `external_user_id`,
    u.`id` AS `user_id`,
    'migration-v63',
    'migration-v63',
    0
FROM `sys_user` u
WHERE u.`deleted` = 0
  AND u.`employee_no` LIKE 'plugin:%'
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_user` w
      WHERE w.`deleted` = 0
        AND w.`phone` = u.`phone`
        AND w.`id` <> u.`id`
        AND w.`wecom_userid` IS NOT NULL
        AND w.`wecom_userid` <> ''
        AND u.`phone` IS NOT NULL
        AND u.`phone` <> ''
  )
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `update_by` = 'migration-v63',
    `deleted` = 0;

-- 4) 为归并目标账号补充 GUEST 角色（若插件账号有而主账号没有）
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`, `update_by`, `deleted`)
SELECT DISTINCT
    wecom_u.`id`,
    r.`id`,
    'migration-v63',
    'migration-v63',
    0
FROM `sys_user` plugin_u
INNER JOIN `sys_user` wecom_u ON wecom_u.`phone` = plugin_u.`phone`
    AND wecom_u.`deleted` = 0
    AND wecom_u.`id` <> plugin_u.`id`
    AND wecom_u.`wecom_userid` IS NOT NULL
    AND wecom_u.`wecom_userid` <> ''
INNER JOIN `sys_user_role` ur ON ur.`user_id` = plugin_u.`id` AND ur.`deleted` = 0
INNER JOIN `sys_role` r ON r.`id` = ur.`role_id` AND r.`role_code` = 'guest' AND r.`deleted` = 0
WHERE plugin_u.`deleted` = 0
  AND plugin_u.`employee_no` LIKE 'plugin:%'
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_user_role` exist_ur
      WHERE exist_ur.`user_id` = wecom_u.`id`
        AND exist_ur.`role_id` = r.`id`
        AND exist_ur.`deleted` = 0
  );

-- 5) 逻辑删除已归并的插件重复账号，并清空冲突手机号
UPDATE `sys_user` plugin_u
INNER JOIN `sys_user` wecom_u ON wecom_u.`phone` = plugin_u.`phone`
    AND wecom_u.`deleted` = 0
    AND wecom_u.`id` <> plugin_u.`id`
    AND wecom_u.`wecom_userid` IS NOT NULL
    AND wecom_u.`wecom_userid` <> ''
SET plugin_u.`deleted` = 1,
    plugin_u.`phone` = NULL,
    plugin_u.`update_by` = 'migration-v63'
WHERE plugin_u.`deleted` = 0
  AND plugin_u.`employee_no` LIKE 'plugin:%'
  AND plugin_u.`phone` IS NOT NULL
  AND plugin_u.`phone` <> '';

-- 6) 其余插件账号若仍有手机号冲突，清空手机号避免再次触发 SSO 异常
UPDATE `sys_user` plugin_u
INNER JOIN `sys_user` other_u ON other_u.`phone` = plugin_u.`phone`
    AND other_u.`deleted` = 0
    AND other_u.`id` <> plugin_u.`id`
SET plugin_u.`phone` = NULL,
    plugin_u.`update_by` = 'migration-v63'
WHERE plugin_u.`deleted` = 0
  AND plugin_u.`employee_no` LIKE 'plugin:%'
  AND plugin_u.`phone` IS NOT NULL
  AND plugin_u.`phone` <> '';
