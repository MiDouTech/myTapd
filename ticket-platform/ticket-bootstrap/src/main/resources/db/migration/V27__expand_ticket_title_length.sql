-- 扩展工单标题字段长度：varchar(200) → varchar(300)
ALTER TABLE `ticket` MODIFY COLUMN `title` varchar(300) NOT NULL COMMENT '工单标题';
