ALTER TABLE `integration_app`
    ADD COLUMN `category_ids` json DEFAULT NULL COMMENT '插件可选工单分类ID列表' AFTER `default_category_id`;

UPDATE `integration_app`
SET `category_ids` = JSON_ARRAY(`default_category_id`)
WHERE `default_category_id` IS NOT NULL
  AND `category_ids` IS NULL;
