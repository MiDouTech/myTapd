-- 扩展工单标题字段长度：varchar(200) → varchar(300)
-- 自 V28 起独立版本号（原与 ticket_log 迁移同为 V27 会导致 Flyway 版本冲突）
ALTER TABLE `ticket` MODIFY COLUMN `title` varchar(300) NOT NULL COMMENT '工单标题';
