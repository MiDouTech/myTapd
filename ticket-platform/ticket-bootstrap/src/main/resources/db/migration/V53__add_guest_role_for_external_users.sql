-- ============================================================
-- V53__add_guest_role_for_external_users.sql
-- 新增外部游客（GUEST）角色，并将外部账号从 OBSERVER 迁移到 GUEST
-- 背景：外部游客需要与内部观察者区分，登录后隐藏「管理」菜单且
--       新建工单时不允许选择处理人
-- ============================================================

-- 1. 插入 GUEST 角色（幂等，若已存在则跳过）
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`, `create_by`, `update_by`)
SELECT 'GUEST', '外部游客', '外部上下游人员，仅可查看和提交工单，不可指定处理人，隐藏管理菜单', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `role_code` = 'GUEST' AND `deleted` = 0
);

-- 2. 将外部账号（通过手机号登录，无 wecom_userid）的角色从 OBSERVER 改为 GUEST
--    先撤销 OBSERVER 角色
UPDATE `sys_user_role` ur
JOIN `sys_user` u ON u.id = ur.user_id
JOIN `sys_role` r ON r.id = ur.role_id
SET ur.deleted = 1, ur.update_by = 'system'
WHERE u.wecom_userid IS NULL
  AND u.password_hash IS NOT NULL
  AND r.role_code = 'OBSERVER'
  AND ur.deleted = 0
  AND u.deleted = 0;

-- 3. 为外部账号分配 GUEST 角色（幂等）
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`, `update_by`, `deleted`)
SELECT u.id,
       (SELECT id FROM `sys_role` WHERE `role_code` = 'GUEST' AND `deleted` = 0 LIMIT 1),
       'system',
       'system',
       0
FROM `sys_user` u
WHERE u.wecom_userid IS NULL
  AND u.password_hash IS NOT NULL
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_user_role` ur2
      JOIN `sys_role` r2 ON r2.id = ur2.role_id
      WHERE ur2.user_id = u.id
        AND r2.role_code = 'GUEST'
        AND ur2.deleted = 0
  );
