ALTER TABLE `ticket_template`
    ADD COLUMN `template_code` varchar(100) DEFAULT NULL COMMENT '稳定模板编码' AFTER `id`,
    ADD COLUMN `is_system` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否系统模板（0:否 1:是）' AFTER `description`,
    ADD COLUMN `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序号' AFTER `is_system`;

UPDATE `ticket_template`
SET `template_code` = CONCAT('legacy_', `id`)
WHERE `template_code` IS NULL;

ALTER TABLE `ticket_template`
    MODIFY COLUMN `template_code` varchar(100) NOT NULL COMMENT '稳定模板编码',
    ADD UNIQUE KEY `uk_ticket_template_code` (`template_code`),
    ADD KEY `idx_ticket_template_system` (`is_system`),
    ADD KEY `idx_ticket_template_active_sort` (`is_active`, `sort_order`);

INSERT INTO `ticket_template`
(`template_code`, `name`, `category_id`, `fields_config`, `description`, `is_system`, `sort_order`, `is_active`, `create_by`, `update_by`)
SELECT 'system_default', '默认模板', NULL, JSON_ARRAY(), '系统默认工单模板，不可删除或停用', 1, -1000, 1, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM `ticket_template` WHERE `template_code` = 'system_default' AND `deleted` = 0
);

CREATE TABLE IF NOT EXISTS `ticket_template_custom_field`
(
    `id`                       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id`              bigint(20)   NOT NULL COMMENT '模板ID',
    `custom_field_id`          bigint(20)   NOT NULL COMMENT '自定义字段定义ID',
    `sort_order`               int(11)      NOT NULL DEFAULT 0 COMMENT '模板内排序',
    `is_enabled`               tinyint(4)   NOT NULL DEFAULT 1 COMMENT '模板内启用状态（0:关闭 1:开启）',
    `is_required`              tinyint(4)   NOT NULL DEFAULT 0 COMMENT '模板内是否必填（0:否 1:是）',
    `placeholder_override`     varchar(200) DEFAULT NULL COMMENT '模板内占位提示',
    `default_value_override`   text         DEFAULT NULL COMMENT '模板内默认值',
    `create_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`                varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`                varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`                  tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_custom_field` (`template_id`, `custom_field_id`),
    KEY `idx_template_field_sort` (`template_id`, `sort_order`),
    KEY `idx_custom_field_id` (`custom_field_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='模板自定义字段关联表';
