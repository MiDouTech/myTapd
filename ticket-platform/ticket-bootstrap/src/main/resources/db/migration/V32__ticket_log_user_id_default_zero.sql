-- 修复自动分派场景下 ticket_log.user_id 缺少默认值导致 INSERT 失败的问题
-- 自动分派/系统操作使用 user_id=0 表示系统自动执行

ALTER TABLE `ticket_log`
    MODIFY COLUMN `user_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '操作用户ID（0表示系统自动操作）';
