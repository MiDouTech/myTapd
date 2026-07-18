-- Webhook 配置增加分类范围：空 category_ids 表示全部分类（兼容历史数据）

ALTER TABLE `webhook_config`
    ADD COLUMN `category_ids` varchar(500) DEFAULT NULL COMMENT '适用分类ID（逗号分隔，空=全部分类）' AFTER `event_types`,
    ADD COLUMN `include_descendants` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否包含子分类（0:否 1:是）' AFTER `category_ids`;
