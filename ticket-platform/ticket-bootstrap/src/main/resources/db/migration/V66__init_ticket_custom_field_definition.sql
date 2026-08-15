CREATE TABLE IF NOT EXISTS `ticket_custom_field_definition`
(
    `id`                     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `field_code`             varchar(100) NOT NULL COMMENT '稳定字段编码',
    `field_title`            varchar(100) NOT NULL COMMENT '字段标题',
    `field_type`             varchar(30)  NOT NULL COMMENT '字段类型',
    `description`            varchar(500) DEFAULT NULL COMMENT '字段说明',
    `placeholder`            varchar(200) DEFAULT NULL COMMENT '占位提示',
    `default_required`       tinyint(4)   NOT NULL DEFAULT 0 COMMENT '默认必填（0:否 1:是）',
    `default_value`          text         DEFAULT NULL COMMENT '默认值配置',
    `validation_config`      json         DEFAULT NULL COMMENT '校验规则配置',
    `option_config`          json         DEFAULT NULL COMMENT '选项配置',
    `is_active`              tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:停用 1:启用）',
    `create_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`              varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`              varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`                tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_custom_field_code` (`field_code`),
    KEY `idx_custom_field_type` (`field_type`),
    KEY `idx_custom_field_active` (`is_active`),
    KEY `idx_custom_field_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='自定义字段定义表';
