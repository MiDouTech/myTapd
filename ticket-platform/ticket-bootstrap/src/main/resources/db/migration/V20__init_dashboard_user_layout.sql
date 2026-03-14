-- V20: 仪表盘个人布局配置表
-- 存储每位用户的个人仪表盘行组排序配置

CREATE TABLE `dashboard_user_layout` (
    `id`            bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       bigint(20)      NOT NULL COMMENT '用户ID，关联 sys_user.id',
    `row_group_key` varchar(64)     NOT NULL COMMENT '行组Key（overview/trend_category/efficiency_workload）',
    `sort_order`    int(11)         NOT NULL DEFAULT 0 COMMENT '排列序号，越小越靠前',
    `is_fixed`      tinyint(1)      NOT NULL DEFAULT 0 COMMENT '是否固定不可拖拽（1固定/0可拖）',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     varchar(50)     NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     varchar(50)     NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`       tinyint(4)      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_row_group` (`user_id`, `row_group_key`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='仪表盘个人布局配置表';
