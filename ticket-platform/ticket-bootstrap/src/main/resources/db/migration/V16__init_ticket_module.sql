-- 创建工单模块表（供测试信息-所属模块下拉选择使用）
CREATE TABLE `ticket_module`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(100) NOT NULL COMMENT '模块名称',
    `sort`        int(11)      NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_sort` (`sort`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='工单模块表';

-- 将复现步骤和实际结果列类型改为 mediumtext，以支持富文本HTML内容存储
ALTER TABLE `ticket_bug_test_info`
    MODIFY COLUMN `reproduce_steps` mediumtext DEFAULT NULL COMMENT '复现步骤（富文本HTML）',
    MODIFY COLUMN `actual_result`   mediumtext DEFAULT NULL COMMENT '实际结果（富文本HTML）';
